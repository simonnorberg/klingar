/*
 * Copyright (C) 2016 Simon Norberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simno.klingar.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.google.android.gms.cast.framework.CastButtonFactory;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.ServerManager;
import net.simno.klingar.data.model.Album;
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.data.model.Library;
import net.simno.klingar.data.model.MediaType;
import net.simno.klingar.data.model.PlexItem;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.data.repository.MusicRepository;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.QueueManager;
import net.simno.klingar.ui.adapter.MusicAdapter;
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.ui.widget.EndScrollListener;
import net.simno.klingar.util.Rx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;
import static net.simno.klingar.data.Key.PLEX_ITEM;
import static net.simno.klingar.util.Views.visible;

public class BrowserController extends BaseController implements
    MusicAdapter.OnPlexItemClickListener, EndScrollListener.EndListener,
    AdapterView.OnItemSelectedListener {

  private static final int PAGE_SIZE = 50;
  private final MusicAdapter adapter;
  @BindView(R.id.toolbar_libs_spinner) Spinner toolbarSpinner;
  @BindView(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindView(R.id.recycler_view) RecyclerView recyclerView;
  @BindView(R.id.miniplayer_container) FrameLayout miniplayerContainer;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;
  @Inject ServerManager serverManager;
  @Inject MusicRepository musicRepository;
  @Inject QueueManager queueManager;
  @Inject MusicController musicController;
  @Inject Rx rx;
  private EndScrollListener endScrollListener;
  private List<Library> libs = Collections.emptyList();
  private Library currentLib;
  private MediaType mediaType;
  private int currentPage = -1;
  private boolean isLoading;
  private boolean serverRefreshed;

  public BrowserController(Bundle args) {
    super(args);
    adapter = new MusicAdapter(this);
  }

  @Override protected int getLayoutResource() {
    return R.layout.controller_browser;
  }

  @Override protected void injectDependencies() {
    if (getActivity() != null) {
      KlingarApp.get(getActivity()).component().inject(this);
    }
  }

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    View view = super.onCreateView(inflater, container);

    PlexItem plexItem = getArgs().getParcelable(PLEX_ITEM);
    if (plexItem instanceof MediaType) {
      mediaType = (MediaType) plexItem;
    }

    ActionBar actionBar = null;
    if (getActivity() != null) {
      actionBar = ((KlingarActivity) getActivity()).getSupportActionBar();
    }
    if (actionBar != null) {
      setHasOptionsMenu(true);
      if (mediaType == null) {
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
      } else {
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mediaType.title());
      }
    }

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setHasFixedSize(true);
    recyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));

    contentLoading.hide();

    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    recyclerView.setAdapter(adapter);
    if (mediaType == null) {
      if (!serverRefreshed) {
        serverRefreshed = true;
        serverManager.refresh();
      }
      observeLibs();
    } else {
      startEndlessScrolling();
      if (currentPage < 0) {
        currentPage = 0;
        browseMediaType();
      }
    }
    observePlayback();
  }

  @Override protected void onDetach(@NonNull View view) {
    super.onDetach(view);
    recyclerView.clearOnScrollListeners();
    recyclerView.setAdapter(null);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_main, menu);
    CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
        R.id.media_route_menu_item);
  }

  @Override public void endReached() {
    if (mediaType != null) {
      browseMediaType();
    }
  }

  @Override public void onPlexItemClicked(PlexItem plexItem) {
    if (plexItem instanceof MediaType) {
      goToMediaType((MediaType) plexItem);
    } else if (plexItem instanceof Artist) {
      goToDetails(plexItem);
    } else if (plexItem instanceof Album) {
      goToDetails(plexItem);
    } else if (plexItem instanceof Track) {
      playTrack((Track) plexItem);
    }
  }

  @OnClick(R.id.miniplayer_container) void onMiniplayerClicked() {
    getRouter().pushController(RouterTransaction.with(new PlayerController(null)));
  }

  private void observeLibs() {
    disposables.add(serverManager.libs()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(libs -> {
          BrowserController.this.libs = libs;

          ArrayList<String> libNames = new ArrayList<>();
          int currentPosition = 0;
          for (int i = 0; i < libs.size(); ++i) {
            Library lib = libs.get(i);
            libNames.add(lib.name());
            if (lib.equals(currentLib)) {
              currentPosition = i;
            }
          }

          if (getActivity() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.item_spinner,
                libNames);
            toolbarSpinner.setAdapter(adapter);
            toolbarSpinner.setSelection(currentPosition);
            toolbarSpinner.setOnItemSelectedListener(this);
            visible(toolbarSpinner);
          }
        }, Rx::onError));
  }

  private void browseLibrary(Library lib) {
    if (lib.equals(currentLib)) {
      return;
    }
    currentLib = lib;
    disposables.add(musicRepository.browseLibrary(lib)
        .compose(bindUntilEvent(DETACH))
        .compose(rx.singleSchedulers())
        .subscribe(adapter::set, Rx::onError));
  }

  private void browseMediaType() {
    if (isLoading) {
      return;
    }
    isLoading = true;
    disposables.add(musicRepository.browseMediaType(mediaType, PAGE_SIZE * currentPage)
        .compose(bindUntilEvent(DETACH))
        .compose(rx.singleSchedulers())
        .subscribe(items -> {
          if (items.isEmpty()) {
            stopEndlessScrolling();
          } else {
            adapter.addAll(items);
          }
          currentPage++; // Only increment page if current page was loaded successfully
          isLoading = false;
        }, Rx::onError));
  }

  private void startEndlessScrolling() {
    endScrollListener = new EndScrollListener((LinearLayoutManager)
        recyclerView.getLayoutManager(), this);
    recyclerView.addOnScrollListener(endScrollListener);
  }

  private void stopEndlessScrolling() {
    recyclerView.removeOnScrollListener(endScrollListener);
  }

  private void observePlayback() {
    disposables.add(musicController.state()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(state -> {
          switch (state) {
            case PlaybackStateCompat.STATE_ERROR:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
              for (Router router : getChildRouters()) {
                removeChildRouter(router);
              }
              break;
            default:
              Router miniplayerRouter = getChildRouter(miniplayerContainer);
              if (!miniplayerRouter.hasRootController()) {
                miniplayerRouter.setRoot(RouterTransaction.with(new MiniPlayerController(null)));
              }
          }
        }, Rx::onError));
  }

  private void goToMediaType(MediaType mediaType) {
    Bundle args = new Bundle();
    args.putParcelable(PLEX_ITEM, mediaType);
    getRouter().pushController(RouterTransaction.with(new BrowserController(args)));
  }

  private void goToDetails(PlexItem plexItem) {
    Bundle args = new Bundle();
    args.putParcelable(PLEX_ITEM, plexItem);
    getRouter().pushController(RouterTransaction.with(new DetailController(args)));
  }

  private void playTrack(Track track) {
    Timber.d("playTrack %s", track);
    disposables.add(musicRepository.createPlayQueue(track)
        .compose(bindUntilEvent(DETACH))
        .compose(rx.singleSchedulers())
        .subscribe(pair -> {
          queueManager.setQueue(pair.first, pair.second);
          musicController.play();
        }, Rx::onError));
  }

  @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    if (position < libs.size()) {
      browseLibrary(libs.get(position));
    }
  }

  @Override public void onNothingSelected(AdapterView<?> parent) {
  }
}
