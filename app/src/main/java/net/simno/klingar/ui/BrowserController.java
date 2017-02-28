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
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

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
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSingleObserver;
import net.simno.klingar.util.SimpleSubscriber;

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
import static net.simno.klingar.ui.ToolbarOwner.TITLE_VISIBLE;

public class BrowserController extends BaseController implements
    MusicAdapter.OnPlexItemClickListener, EndScrollListener.EndListener {

  private static final int PAGE_SIZE = 50;
  private final MusicAdapter adapter;
  @BindView(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindView(R.id.recycler_view) RecyclerView recyclerView;
  @BindView(R.id.miniplayer_container) FrameLayout miniplayerContainer;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;
  @Inject ToolbarOwner toolbarOwner;
  @Inject ServerManager serverManager;
  @Inject MusicRepository musicRepository;
  @Inject QueueManager queueManager;
  @Inject MusicController musicController;
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
    if (plexItem != null && plexItem instanceof MediaType) {
      mediaType = (MediaType) plexItem;
    }

    ToolbarOwner.Config.Builder builder = ToolbarOwner.Config.builder()
        .background(true)
        .titleAlpha(TITLE_VISIBLE);

    if (mediaType == null) {
      toolbarOwner.setConfig(builder
          .backNavigation(false)
          .build());
    } else {
      toolbarOwner.setConfig(builder
          .backNavigation(true)
          .title(mediaType.title())
          .build());
    }

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setHasFixedSize(true);
    recyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));
    recyclerView.setAdapter(adapter);

    contentLoading.hide();

    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    if (mediaType == null) {
      if (!serverRefreshed) {
        serverRefreshed = true;
        serverManager.refresh();
      }
      observeSpinner();
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
        .compose(RxHelper.flowableSchedulers())
        .subscribeWith(new SimpleSubscriber<List<Library>>() {
          @Override public void onNext(List<Library> libs) {
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
            toolbarOwner.setConfig(toolbarOwner.getConfig().toBuilder()
                .title(null)
                .options(libNames)
                .selection(currentPosition)
                .build());
          }
        }));
  }

  private void observeSpinner() {
    disposables.add(toolbarOwner.spinnerSelection()
        .compose(bindUntilEvent(DETACH))
        .compose(RxHelper.flowableSchedulers())
        .subscribeWith(new SimpleSubscriber<Integer>() {
          @Override public void onNext(Integer position) {
            if (position < libs.size()) {
              browseLibrary(libs.get(position));
            }
          }
        }));
  }

  private void browseLibrary(Library lib) {
    if (lib.equals(currentLib)) {
      return;
    }
    currentLib = lib;
    disposables.add(musicRepository.browseLibrary(lib)
        .compose(bindUntilEvent(DETACH))
        .compose(RxHelper.singleSchedulers())
        .subscribeWith(new SimpleSingleObserver<List<PlexItem>>() {
          @Override public void onSuccess(List<PlexItem> items) {
            adapter.set(items);
          }
        }));
  }

  private void browseMediaType() {
    if (isLoading) {
      return;
    }
    isLoading = true;
    disposables.add(musicRepository.browseMediaType(mediaType, PAGE_SIZE * currentPage)
        .compose(bindUntilEvent(DETACH))
        .compose(RxHelper.singleSchedulers())
        .subscribeWith(new SimpleSingleObserver<List<PlexItem>>() {
          @Override public void onSuccess(List<PlexItem> items) {
            if (items.isEmpty()) {
              stopEndlessScrolling();
            } else {
              adapter.addAll(items);
            }
            currentPage++; // Only increment page if current page was loaded successfully
            isLoading = false;
          }
        }));
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
        .compose(RxHelper.flowableSchedulers())
        .subscribeWith(new SimpleSubscriber<Integer>() {
          @Override public void onNext(Integer state) {
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
          }
        }));
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
        .compose(RxHelper.singleSchedulers())
        .subscribeWith(new SimpleSingleObserver<Pair<List<Track>, Long>>() {
          @Override public void onSuccess(Pair<List<Track>, Long> pair) {
            queueManager.setQueue(pair.first, pair.second);
            musicController.play();
          }
        }));
  }
}
