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
import android.widget.FrameLayout;

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
import net.simno.klingar.data.model.Album;
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.data.model.PlexItem;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.data.repository.MusicRepository;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.QueueManager;
import net.simno.klingar.ui.adapter.MusicAdapter;
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.util.Rx;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;
import static net.simno.klingar.data.Key.PLEX_ITEM;

public class DetailController extends BaseController implements
    MusicAdapter.OnPlexItemClickListener {

  private final MusicAdapter adapter;
  @BindView(R.id.recycler_view) RecyclerView recyclerView;
  @BindView(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindView(R.id.miniplayer_container) FrameLayout miniplayerContainer;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;
  @Inject MusicRepository musicRepository;
  @Inject QueueManager queueManager;
  @Inject MusicController musicController;
  @Inject Rx rx;
  private PlexItem plexItem;
  private boolean itemsLoaded;

  public DetailController(Bundle args) {
    super(args);
    adapter = new MusicAdapter(this);
  }

  @Override protected int getLayoutResource() {
    return R.layout.controller_detail;
  }

  @Override protected void injectDependencies() {
    if (getActivity() != null) {
      KlingarApp.get(getActivity()).component().inject(this);
    }
  }

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    View view = super.onCreateView(inflater, container);

    plexItem = getArgs().getParcelable(PLEX_ITEM);

    ActionBar actionBar = null;
    if (getActivity() != null) {
      actionBar = ((KlingarActivity) getActivity()).getSupportActionBar();
    }
    if (actionBar != null) {
      setHasOptionsMenu(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setDisplayShowTitleEnabled(true);
      if (plexItem instanceof Artist) {
        actionBar.setTitle(((Artist) plexItem).title());
      } else if (plexItem instanceof Album) {
        actionBar.setTitle(((Album) plexItem).title());
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
    if (!itemsLoaded) {
      if (plexItem instanceof Artist) {
        getArtistItems((Artist) plexItem);
      } else if (plexItem instanceof Album) {
        getAlbumItems((Album) plexItem);
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

  @Override public void onPlexItemClicked(PlexItem plexItem) {
    if (plexItem instanceof Album) {
      goToDetails(plexItem);
    } else if (plexItem instanceof Track) {
      playTrack((Track) plexItem);
    }
  }

  @OnClick(R.id.miniplayer_container) void onMiniplayerClicked() {
    getRouter().pushController(RouterTransaction.with(new PlayerController(null)));
  }

  private void getArtistItems(Artist artist) {
    disposables.add(musicRepository.artistItems(artist)
        .compose(bindUntilEvent(DETACH))
        .compose(rx.singleSchedulers())
        .subscribe(items -> {
          adapter.addAll(items);
          itemsLoaded = true;
        }, Rx::onError));
  }

  private void getAlbumItems(Album album) {
    disposables.add(musicRepository.albumItems(album)
        .compose(bindUntilEvent(DETACH))
        .compose(rx.singleSchedulers())
        .subscribe(items -> {
          adapter.addAll(items);
          itemsLoaded = true;
        }, Rx::onError));
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
}
