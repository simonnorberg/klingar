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
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.ui.widget.EndlessScrollListener;
import net.simno.klingar.util.MediaIdHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindDrawable;

import static net.simno.klingar.util.MediaIdHelper.createBrowseMediaId;
import static net.simno.klingar.util.MediaItemHelper.createMediaItem;
import static net.simno.klingar.util.MediaItemHelper.getViewType;

/**
 * BrowserFragment shows items from a library or a specific browse type (with endless scroll)
 */
public class BrowserFragment extends BaseFragment implements EndlessScrollListener.EndListener,
    MediaAdapter.OnItemClickListener, MediaControllerListener {

  private static final String ARG_MEDIA_ITEM = "arg_media_item";
  private static final String STATE_ITEMS = "state_items";
  private static final String STATE_OFFSET_MEDIA_ID = "state_offset_media_id";
  private static final int PAGE_LIMIT = 50;

  @Bind(R.id.recycler_view) RecyclerView recyclerView;
  @Bind(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;

  @Inject ToolbarOwner toolbarOwner;
  @Inject RequestManager glide;

  private MediaItemListener mediaItemListener;
  private MediaItem mediaItem;
  private MediaAdapter adapter;
  private boolean shouldGetItems;
  /** Media Id used for loading pages with 50 items */
  private String offsetMediaId;
  private EndlessScrollListener endlessScrollListener;
  private boolean isLoading;

  public static BrowserFragment newInstance(@NonNull MediaItem mediaItem) {
    BrowserFragment fragment = new BrowserFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_MEDIA_ITEM, mediaItem);
    fragment.setArguments(args);
    return fragment;
  }

  public BrowserFragment() {
  }

  @Override
  int getLayoutResource() {
    return R.layout.fragment_browser;
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
      offsetMediaId = savedInstanceState.getString(STATE_OFFSET_MEDIA_ID);
    }

    if (offsetMediaId != null) {
      // If offsetMediaId exist we had an endless scroll listener before. Let's recreate it!
      setupEndlessScrolling();
    }

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
    toolbarOwner.showTitleAndBackground();
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
    outState.putString(STATE_OFFSET_MEDIA_ID, offsetMediaId);
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
    int viewType = getViewType(mediaItem);

    if (viewType == Type.LIBRARY) {
      MediaDescriptionCompat description = mediaItem.getDescription();
      Bundle extras = description.getExtras();

      if (extras != null) {
        // Create media items for all browse types
        String libKey = extras.getString(Extra.STRING_KEY);
        adapter.add(getBrowseMediaItem(libKey, mediaItem.getMediaId(), "Artists",
            String.valueOf(Type.ARTIST), "8"));
        adapter.add(getBrowseMediaItem(libKey, mediaItem.getMediaId(), "Albums",
            String.valueOf(Type.ALBUM), "9"));
        adapter.add(getBrowseMediaItem(libKey, mediaItem.getMediaId(), "Tracks",
            String.valueOf(Type.TRACK), "10"));
      }
    }

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
        if (viewType == Type.MEDIA_TYPE) {
          // if we are browsing a specific type.
          // this is the first time we create the endless scroll listener.
          offsetMediaId = MediaIdHelper.addOffsetToBrowseMediaId(mediaItem.getMediaId(), PAGE_LIMIT);
          setupEndlessScrolling();
        }
      }
    });
  }

  private static MediaItem getBrowseMediaItem(String libKey, String parentId, String title,
                                              String mediaType, String mediaKey) {
    Bundle extrasArtists = new Bundle();
    extrasArtists.putInt(Extra.INT_TYPE, Type.MEDIA_TYPE);
    extrasArtists.putString(Extra.STRING_KEY, mediaKey);

    String artistsMediaId = createBrowseMediaId(parentId, libKey, mediaType, mediaKey, 0);
    return createMediaItem(artistsMediaId, title, extrasArtists, MediaItem.FLAG_BROWSABLE);
  }

  @Override
  public void endReached() {
    MediaBrowserCompat mediaBrowser = mediaItemListener.getMediaBrowser();
    if (mediaBrowser == null) {
      return;
    }
    if (!isLoading) {
      isLoading = true;
      mediaBrowser.unsubscribe(offsetMediaId);
      mediaBrowser.subscribe(offsetMediaId, subscriptionCallback);
    }
  }

  private void setupEndlessScrolling() {
    isLoading = false;
    endlessScrollListener = new EndlessScrollListener(
        (LinearLayoutManager) recyclerView.getLayoutManager(), BrowserFragment.this);
    recyclerView.addOnScrollListener(endlessScrollListener);
  }

  private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback =
      new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
          if (children.isEmpty() || offsetMediaId == null) {
            // Remove endless scroll listener if we have received all items or if offsetMediaId
            // doesn't exist
            recyclerView.removeOnScrollListener(endlessScrollListener);
            offsetMediaId = null; // and don't recreate the scroll listener
          } else {
            adapter.addAll(children);
            isLoading = false;
            offsetMediaId = MediaIdHelper.addOffsetToBrowseMediaId(offsetMediaId, PAGE_LIMIT);
          }
        }
      };
}
