/*
 * Copyright (C) 2015 Simon Norberg
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
package net.simno.klingar.ui.fragment;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.RequestManager;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.Extra;
import net.simno.klingar.data.Type;
import net.simno.klingar.ui.ToolbarOwner;
import net.simno.klingar.ui.adapter.MediaAdapter;
import net.simno.klingar.ui.callback.DummyMediaItemListener;
import net.simno.klingar.ui.callback.MediaControllerListener;
import net.simno.klingar.ui.callback.MediaItemListener;
import net.simno.klingar.ui.widget.BackgroundLayout;
import net.simno.klingar.ui.widget.CircleImageViewTarget;
import net.simno.klingar.ui.widget.DistanceScrollListener;
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.ui.widget.EffectScrollListener;
import net.simno.klingar.ui.widget.SquareImageView;
import net.simno.klingar.util.Strings;
import net.simno.klingar.util.Urls;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindDrawable;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.simno.klingar.util.MediaItemHelper.getViewType;

public class DetailFragment extends BaseFragment implements MediaAdapter.OnItemClickListener,
    MediaControllerListener {

  private static final String ARG_MEDIA_ITEM = "arg_media_item";
  private static final String STATE_ITEMS = "state_items";
  private static final String STATE_SCROLLED_DISTANCE = "state_scrolled_distance";

  @Bind(R.id.recycler_view) RecyclerView recyclerView;
  @Bind(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @Bind(R.id.detail_container) RelativeLayout detailContainer;
  @BindColor(R.color.primary) int primaryColor;
  @BindDimen(R.dimen.background_height) int backgroundHeight;
  @BindDimen(R.dimen.background_image_width) int imageWidth;
  @BindDimen(R.dimen.background_image_height) int imageHeight;
  @BindDimen(R.dimen.toolbar_height) int toolbarHeight;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;

  @Inject ToolbarOwner toolbarOwner;
  @Inject RequestManager glide;

  private MediaItemListener mediaItemListener;
  private MediaItem mediaItem;
  private MediaAdapter adapter;
  private boolean shouldGetItems;
  /** Keep track of scrolled distance. Useful for config changes! */
  private DistanceScrollListener distanceListener;
  private int savedDistance;

  public static DetailFragment newInstance(@NonNull MediaItem mediaItem) {
    DetailFragment fragment = new DetailFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_MEDIA_ITEM, mediaItem);
    fragment.setArguments(args);
    return fragment;
  }

  public DetailFragment() {
  }

  @Override
  int getLayoutResource() {
    return R.layout.fragment_detail;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mediaItemListener = (MediaItemListener) activity;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KlingarApp.get(getActivity()).component().inject(this);

    if (getArguments() != null) {
      mediaItem = getArguments().getParcelable(ARG_MEDIA_ITEM);
    }

    adapter = new MediaAdapter(this, glide);
    if (savedInstanceState != null) {
      ArrayList<MediaItem> items = savedInstanceState.getParcelableArrayList(STATE_ITEMS);
      if (items != null) {
        adapter.set(items);
      }
    } else {
      shouldGetItems = true;
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    contentLoading.hide();

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setHasFixedSize(true);
    recyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));
    recyclerView.setAdapter(adapter);

    if (savedInstanceState != null) {
      savedDistance = savedInstanceState.getInt(STATE_SCROLLED_DISTANCE, 0);
    }

    if (distanceListener != null) {
      // Listener still exist if fragment is restored from backstack
      savedDistance = distanceListener.getDistance();
    }

    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      distanceListener = new DistanceScrollListener(savedDistance);
      recyclerView.addOnScrollListener(distanceListener);
      return view;
    }

    final int viewType = getViewType(mediaItem);
    String imageTranscodeUri = null;

    MediaDescriptionCompat description = mediaItem.getDescription();
    Bundle extras = description.getExtras();
    if (extras != null) {
      switch (viewType) {
        case Type.PLAYLIST:
          imageTranscodeUri = extras.getString(Extra.STRING_ART_URI);
          break;
        case Type.ARTIST:
          imageTranscodeUri = extras.getString(Extra.STRING_ART_URI);
          break;
        case Type.ALBUM:
          imageTranscodeUri = extras.getString(Extra.STRING_THUMB_URI);
          break;
      }
    }

    if (Strings.isBlank(imageTranscodeUri)) {
      // Skip background image but keep track of scroll distance anyway
      distanceListener = new DistanceScrollListener(savedDistance);
      recyclerView.addOnScrollListener(distanceListener);
      return view;
    }

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
      glide.load(Urls.addTranscodeParams(imageTranscodeUri, imageWidth, imageHeight))
          .asBitmap()
          .centerCrop()
          .into(new CircleImageViewTarget(background));
    } else {
      glide.load(Urls.addTranscodeParams(imageTranscodeUri, imageWidth, imageHeight))
          .centerCrop()
          .into(background);
    }

    // Set recyclerview top padding to background height
    recyclerView.setPadding(recyclerView.getPaddingStart(), backgroundHeight,
        recyclerView.getPaddingEnd(), recyclerView.getPaddingBottom());

    // Add scroll listener that handles toolbar fading and background parallax effects
    distanceListener = new EffectScrollListener(toolbarOwner, backgroundLayout, backgroundHeight,
        toolbarHeight, savedDistance);
    recyclerView.addOnScrollListener(distanceListener);

    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    MediaBrowserCompat mediaBrowser = mediaItemListener.getMediaBrowser();
    if (mediaBrowser != null && mediaBrowser.isConnected()) {
      onConnected();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    toolbarOwner.setTitle(mediaItem.getDescription().getTitle());
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      toolbarOwner.showTitleAndBackground();
    } else {
      toolbarOwner.hideTitleAndBackground();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    MediaBrowserCompat mediaBrowser = mediaItemListener.getMediaBrowser();
    if (mediaBrowser != null && mediaBrowser.isConnected() && mediaItem != null) {
      mediaBrowser.unsubscribe(mediaItem.getMediaId());
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (adapter != null) {
      outState.putParcelableArrayList(STATE_ITEMS, adapter.getItems());
    }
    if (distanceListener != null) {
      savedDistance = distanceListener.getDistance();
    }
    outState.putInt(STATE_SCROLLED_DISTANCE, savedDistance);
  }

  @Override
  public void onDestroyView() {
    recyclerView.clearOnScrollListeners();
    super.onDestroyView();
  }

  @Override
  public void onDetach() {
    mediaItemListener = new DummyMediaItemListener();
    super.onDetach();
  }

  @Override
  public void onItemClicked(MediaItem mediaItem) {
    mediaItemListener.onMediaItemSelected(mediaItem);
  }

  @Override
  public void onConnected() {
    // onConnected is called either in onStart or by Activity
    if (isDetached()) {
      return;
    }
    // Only get items when fragment is first created
    if (shouldGetItems()) {
      getItems();
    }
  }

  private boolean shouldGetItems() {
    if (shouldGetItems) {
      shouldGetItems = false;
      return true;
    }
    return false;
  }

  private void getItems() {
    MediaBrowserCompat mediaBrowser = mediaItemListener.getMediaBrowser();
    if (mediaBrowser == null) {
      return;
    }
    contentLoading.show();
    mediaBrowser.unsubscribe(mediaItem.getMediaId());
    mediaBrowser.subscribe(mediaItem.getMediaId(), new MediaBrowserCompat.SubscriptionCallback() {
      @Override
      public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
        contentLoading.hide();
        adapter.addAll(children);
      }
    });
  }
}
