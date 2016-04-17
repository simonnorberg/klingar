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
package net.simno.klingar.playback;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import net.simno.klingar.data.MusicProvider;
import net.simno.klingar.util.Strings;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QueueManager {

  @Inject RequestManager glide;
  @Inject MusicProvider musicProvider;

  private final int iconWidth;
  private final int iconHeight;
  private List<MediaItem> mediaItems;
  private List<QueueItem> queue;
  private int currentIndexOnQueue;
  private MetadataUpdateListener listener;

  @Inject
  public QueueManager(Resources resources) {
    this.iconWidth = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
    this.iconHeight = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
  }

  public void setMetadataUpdateListener(MetadataUpdateListener listener) {
    this.listener = listener;
  }

  public void setMediaItems(List<MediaItem> mediaItems) {
    this.mediaItems = mediaItems;
  }

  public void setQueue(String mediaId) {
    reset();
    queue = buildQueue();
    listener.onQueueUpdated(getQueueTitle(mediaId), queue);
  }

  private List<QueueItem> buildQueue() {
    if (mediaItems == null || mediaItems.isEmpty()) {
      return null;
    }
    List<QueueItem> queue = new ArrayList<>();
    for (int i = 0; i < mediaItems.size(); ++i) {
      queue.add(new QueueItem(mediaItems.get(i).getDescription(), i));
    }
    return queue;
  }

  public CharSequence getQueueTitle(String mediaId) {
    int index = getTrackIndexOnQueue(mediaId);
    if (isIndexPlayable(index)) {
      return queue.get(index).getDescription().getSubtitle();
    }
    return null;
  }

  public @Nullable
  QueueItem getCurrentItem() {
    return isIndexPlayable(currentIndexOnQueue) ? queue.get(currentIndexOnQueue) : null;
  }

  public boolean setCurrentItem(String mediaId) {
    currentIndexOnQueue = getTrackIndexOnQueue(mediaId);
    if (isIndexPlayable(currentIndexOnQueue)) {
      updateMetadata();
      listener.onCurrentQueueIndexUpdated(currentIndexOnQueue);
      return true;
    }
    return false;
  }

  public boolean setCurrentItem(long queueId) {
    currentIndexOnQueue = getTrackIndexOnQueue(queueId);
    if (isIndexPlayable(currentIndexOnQueue)) {
      updateMetadata();
      listener.onCurrentQueueIndexUpdated(currentIndexOnQueue);
      return true;
    }
    return false;
  }

  public boolean skipToNext() {
    if (canSkipToNext()) {
      ++currentIndexOnQueue;
    } else {
      currentIndexOnQueue = 0;
    }
    if (isIndexPlayable(currentIndexOnQueue)) {
      updateMetadata();
      return true;
    }
    return false;
  }

  public boolean skipToPrevious() {
    if (canSkipToPrevious()) {
      --currentIndexOnQueue;
    } else {
      currentIndexOnQueue = 0;
    }
    if (isIndexPlayable(currentIndexOnQueue)) {
      updateMetadata();
      return true;
    }
    return false;
  }

  public long getAvailableActions(long actions) {
    if (canSkipToPrevious()) {
      actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
    }
    if (canSkipToNext()) {
      actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
    }
    return actions;
  }

  public void reset() {
    queue = null;
    currentIndexOnQueue = -1;
  }

  private int getTrackIndexOnQueue(String mediaId) {
    if (isQueueEmpty()) {
      return -1;
    }
    int index = 0;
    for (QueueItem item : queue) {
      if (Strings.equals(mediaId, item.getDescription().getMediaId())) {
        return index;
      }
      ++index;
    }
    return -1;
  }

  private int getTrackIndexOnQueue(long queueId) {
    if (isQueueEmpty()) {
      return -1;
    }
    int index = 0;
    for (QueueItem item : queue) {
      if (queueId == item.getQueueId()) {
        return index;
      }
      index++;
    }
    return -1;
  }

  private int getQueueSize() {
    return isQueueEmpty() ? 0 : queue.size();
  }

  private boolean canSkipToPrevious() {
    return isIndexPlayable(currentIndexOnQueue - 1);
  }

  private boolean canSkipToNext() {
    return isIndexPlayable(currentIndexOnQueue + 1);
  }

  private boolean isIndexPlayable(int index) {
    return (index >= 0 && index < getQueueSize());
  }

  private boolean isQueueEmpty() {
    return queue == null || queue.isEmpty();
  }

  private void updateMetadata() {
    MediaSessionCompat.QueueItem queueItem = getCurrentItem();
    if (queueItem == null) {
      listener.onMetadataRetrieveError();
      return;
    }

    MediaMetadataCompat track = musicProvider.getMetadata(queueItem.getDescription());
    final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

    listener.onMetadataChanged(track);

    // Update metadata and session with album artwork icon
    if (track.getDescription().getIconUri() != null &&
        track.getDescription().getIconBitmap() == null) {

      String albumUri = track.getDescription().getIconUri().toString();

      glide.load(albumUri)
          .asBitmap()
          .diskCacheStrategy(DiskCacheStrategy.ALL) // Also cache the original size for other use
          .override(iconWidth, iconHeight)
          .into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
              MediaSessionCompat.QueueItem currentQueueItem = getCurrentItem();
              MediaMetadataCompat currentTrack = musicProvider.getMetadata(trackId);
              currentTrack = new MediaMetadataCompat.Builder(currentTrack)
                  .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, resource)
                  .build();

              musicProvider.updateMetadata(trackId, currentTrack);

              // If we are still playing the same track
              String currentTrackId = currentQueueItem == null ? null :
                  currentQueueItem.getDescription().getMediaId();
              if (Strings.equals(trackId, currentTrackId)) {
                listener.onMetadataChanged(currentTrack);
              }
            }
          });
    }
  }

  public interface MetadataUpdateListener {
    void onMetadataChanged(MediaMetadataCompat metadata);
    void onMetadataRetrieveError();
    void onCurrentQueueIndexUpdated(int queueIndex);
    void onQueueUpdated(CharSequence title, List<QueueItem> newQueue);
  }
}
