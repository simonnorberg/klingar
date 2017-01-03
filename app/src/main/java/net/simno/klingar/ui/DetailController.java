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
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bumptech.glide.Glide;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.Type;
import net.simno.klingar.data.model.Album;
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.data.model.PlexItem;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.data.repository.MusicRepository;
import net.simno.klingar.ui.adapter.MusicAdapter;
import net.simno.klingar.ui.widget.BackgroundLayout;
import net.simno.klingar.ui.widget.BackgroundScrollListener;
import net.simno.klingar.ui.widget.CircleImageViewTarget;
import net.simno.klingar.ui.widget.DistanceScrollListener;
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.ui.widget.SquareImageView;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;
import net.simno.klingar.util.Urls;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.simno.klingar.data.Key.PLEX_ITEM;
import static net.simno.klingar.ui.ToolbarOwner.TITLE_GONE;
import static net.simno.klingar.ui.ToolbarOwner.TITLE_VISIBLE;

public class DetailController extends BaseController implements
    MusicAdapter.OnPlexItemClickListener {

  private final MusicAdapter adapter;
  @BindView(R.id.recycler_view) RecyclerView recyclerView;
  @BindView(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindView(R.id.detail_container) RelativeLayout detailContainer;
  @BindColor(R.color.primary) int primaryColor;
  @BindDimen(R.dimen.background_height) int backgroundHeight;
  @BindDimen(R.dimen.background_image_width) int imageWidth;
  @BindDimen(R.dimen.background_image_height) int imageHeight;
  @BindDimen(R.dimen.toolbar_height) int toolbarHeight;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;
  @Inject ToolbarOwner toolbarOwner;
  @Inject MusicRepository musicRepository;
  private DistanceScrollListener scrollListener;
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

    if (getResources() != null) {
      if (getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
        createPortraitView();
      } else {
        createLandscapeView();
      }
    }

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setHasFixedSize(true);
    recyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));
    recyclerView.setAdapter(adapter);

    contentLoading.hide();

    return view;
  }

  private void createPortraitView() {
    ToolbarOwner.Config.Builder builder = ToolbarOwner.Config.builder()
        .background(false)
        .backNavigation(true)
        .titleAlpha(TITLE_GONE);

    if (plexItem instanceof Artist) {
      Artist artist = (Artist) plexItem;
      toolbarOwner.setConfig(builder
          .title(artist.title())
          .build());
      showBackgroundImage(artist.art(), Type.ARTIST);
    } else if (plexItem instanceof Album) {
      Album album = (Album) plexItem;
      toolbarOwner.setConfig(builder
          .title(album.title())
          .build());
      showBackgroundImage(album.thumb(), Type.ALBUM);
    }
  }

  private void createLandscapeView() {
    ToolbarOwner.Config.Builder builder = ToolbarOwner.Config.builder()
        .background(true)
        .backNavigation(true)
        .titleAlpha(TITLE_VISIBLE);

    if (plexItem instanceof Artist) {
      Artist artist = (Artist) plexItem;
      toolbarOwner.setConfig(builder
          .title(artist.title())
          .build());
    } else if (plexItem instanceof Album) {
      Album album = (Album) plexItem;
      toolbarOwner.setConfig(builder
          .title(album.title())
          .build());
    }

    scrollListener = new DistanceScrollListener(ORIENTATION_LANDSCAPE);
    recyclerView.addOnScrollListener(scrollListener);
  }

  @Override protected void onRestoreViewState(@NonNull View view, @NonNull Bundle savedViewState) {
    super.onRestoreViewState(view, savedViewState);
    if (scrollListener != null) {
      scrollListener.onRestoreViewState(savedViewState);
    }
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    if (!itemsLoaded) {
      if (plexItem instanceof Artist) {
        getArtistItems((Artist) plexItem);
      } else if (plexItem instanceof Album) {
        getAlbumItems((Album) plexItem);
      }
    }
  }

  @Override protected void onSaveViewState(@NonNull View view, @NonNull Bundle outState) {
    super.onSaveViewState(view, outState);
    if (scrollListener != null) {
      scrollListener.onSaveViewState(outState);
    }
  }

  @Override protected void onDetach(@NonNull View view) {
    super.onDetach(view);
    recyclerView.clearOnScrollListeners();
  }

  private void showBackgroundImage(String imageTranscodeUri, int viewType) {
    // Add background image and layout programmatically
    ImageView background = new SquareImageView(getActivity());
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(imageWidth, imageHeight);
    params.addRule(RelativeLayout.CENTER_IN_PARENT);
    background.setLayoutParams(params);

    BackgroundLayout backgroundLayout = new BackgroundLayout(getActivity(), background,
        backgroundHeight - toolbarHeight);
    backgroundLayout.setBackgroundColor(primaryColor);

    detailContainer.addView(backgroundLayout, 0,
        new RelativeLayout.LayoutParams(MATCH_PARENT, backgroundHeight));

    // Load background image into image view
    if (viewType == Type.ARTIST) {
      Glide.with(getActivity())
          .load(Urls.addTranscodeParams(imageTranscodeUri, imageWidth, imageHeight))
          .asBitmap()
          .centerCrop()
          .into(new CircleImageViewTarget(background));
    } else {
      Glide.with(getActivity())
          .load(Urls.addTranscodeParams(imageTranscodeUri, imageWidth, imageHeight))
          .centerCrop()
          .into(background);
    }

    // Set recyclerview top padding to background height
    recyclerView.setPadding(recyclerView.getPaddingStart(), backgroundHeight,
        recyclerView.getPaddingEnd(), recyclerView.getPaddingBottom());

    // Add scroll listener that handles toolbar fading and background parallax effects
    scrollListener = new BackgroundScrollListener(ORIENTATION_PORTRAIT, backgroundLayout,
        toolbarOwner, backgroundHeight, toolbarHeight);
    recyclerView.addOnScrollListener(scrollListener);
  }

  private void getArtistItems(Artist artist) {
    subscriptions.add(musicRepository.artistItems(artist)
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<List<PlexItem>>() {
          @Override public void onNext(List<PlexItem> items) {
            adapter.addAll(items);
            itemsLoaded = true;
          }
        })
    );
  }

  private void getAlbumItems(Album album) {
    subscriptions.add(musicRepository.albumItems(album)
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<List<PlexItem>>() {
          @Override public void onNext(List<PlexItem> items) {
            adapter.addAll(items);
            itemsLoaded = true;
          }
        })
    );
  }

  @Override public void onPlexItemClicked(PlexItem plexItem) {
    if (plexItem instanceof Album) {
      goToDetails(plexItem);
    } else if (plexItem instanceof Track) {
      playTrack((Track) plexItem);
    }
  }

  private void goToDetails(PlexItem plexItem) {
    Bundle args = new Bundle();
    args.putParcelable(PLEX_ITEM, plexItem);
    getRouter().pushController(RouterTransaction.with(new DetailController(args)));
  }

  private void playTrack(Track track) {
  }
}
