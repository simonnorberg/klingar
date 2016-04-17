/*
 * Copyright (C) 2014 The Android Open Source Project
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
package net.simno.klingar.playback;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import net.simno.klingar.data.Extra;
import net.simno.klingar.data.MusicProvider;
import net.simno.klingar.util.Strings;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * An implementation of Playback that talks to Cast.
 */
public class CastPlayback implements Playback {

  private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
  private static final String ITEM_ID = "itemId";

  private final MusicProvider musicProvider;
  private final VideoCastConsumerImpl castConsumer = new VideoCastConsumerImpl() {

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
      setMetadataFromRemote();
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
      updatePlaybackState();
    }
  };

  private Callback callback;
  private int state;
  private volatile int currentPosition;
  private volatile String currentMediaId;
  private volatile String currentTrackUri;

  public CastPlayback(MusicProvider musicProvider) {
    this.musicProvider = musicProvider;
  }

  @Override
  public void start() {
    VideoCastManager.getInstance().addVideoCastConsumer(castConsumer);
  }

  @Override
  public void stop(boolean notifyListeners) {
    VideoCastManager.getInstance().removeVideoCastConsumer(castConsumer);
    state = PlaybackStateCompat.STATE_STOPPED;
    if (notifyListeners && callback != null) {
      callback.onPlaybackStatusChanged(state);
    }
  }

  @Override
  public void setState(int state) {
    this.state = state;
  }

  @Override
  public int getState() {
    return state;
  }

  @Override
  public boolean isConnected() {
    return VideoCastManager.getInstance().isConnected();
  }

  @Override
  public boolean isPlaying() {
    try {
      return VideoCastManager.getInstance().isConnected() &&
          VideoCastManager.getInstance().isRemoteMediaPlaying();
    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
      Timber.e(e, "Exception calling isRemoteMoviePlaying");
    }
    return false;
  }

  @Override
  public int getCurrentStreamPosition() {
    if (!VideoCastManager.getInstance().isConnected()) {
      return currentPosition;
    }
    try {
      return (int) VideoCastManager.getInstance().getCurrentMediaPosition();
    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
      Timber.e(e, "Exception getting media position");
    }
    return -1;
  }

  @Override
  public void setCurrentStreamPosition(int pos) {
    this.currentPosition = pos;
  }

  @Override
  public void updateLastKnownStreamPosition() {
    currentPosition = getCurrentStreamPosition();
  }

  @Override
  public void play(QueueItem item) {
    try {
      MediaDescriptionCompat description = item.getDescription();
      Bundle extras = description.getExtras();
      if (extras != null && extras.containsKey(Extra.STRING_URI)) {
        loadMedia(item.getDescription().getMediaId(), extras.getString(Extra.STRING_URI), true);
        state = PlaybackStateCompat.STATE_BUFFERING;
        if (callback != null) {
          callback.onPlaybackStatusChanged(state);
        }
      }
    } catch (JSONException | TransientNetworkDisconnectionException | NoConnectionException e) {
      Timber.e(e, "Exception loading media");
      if (callback != null) {
        callback.onError(e.getMessage());
      }
    }
  }

  @Override
  public void pause() {
    try {
      if (VideoCastManager.getInstance().isRemoteMediaLoaded()) {
        VideoCastManager.getInstance().pause();
        currentPosition = (int) VideoCastManager.getInstance().getCurrentMediaPosition();
      } else {
        loadMedia(currentMediaId, currentTrackUri, false);
      }
    } catch (JSONException | CastException | TransientNetworkDisconnectionException
        | NoConnectionException | IllegalArgumentException e) {
      Timber.e(e, "Exception pausing cast playback");
      if (callback != null) {
        callback.onError(e.getMessage());
      }
    }
  }

  @Override
  public void seekTo(int position) {
    if (currentMediaId == null) {
      if (callback != null) {
        callback.onError("seekTo cannot be calling in the absence of mediaId.");
      }
      return;
    }
    try {
      if (VideoCastManager.getInstance().isRemoteMediaLoaded()) {
        VideoCastManager.getInstance().seek(position);
        currentPosition = position;
      } else {
        currentPosition = position;
        loadMedia(currentMediaId, currentTrackUri, false);
      }
    } catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException |
        IllegalArgumentException e) {
      Timber.e(e, "Exception pausing cast playback");
      if (callback != null) {
        callback.onError(e.getMessage());
      }
    }
  }

  @Override
  public void setCurrentMediaId(String mediaId) {
    this.currentMediaId = mediaId;
  }

  @Override
  public String getCurrentMediaId() {
    return currentMediaId;
  }

  @Override
  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  private void updatePlaybackState() {
    int status = VideoCastManager.getInstance().getPlaybackStatus();
    int idleReason = VideoCastManager.getInstance().getIdleReason();

    // Convert the remote playback states to media playback states.
    switch (status) {
      case MediaStatus.PLAYER_STATE_IDLE:
        if (idleReason == MediaStatus.IDLE_REASON_FINISHED) {
          if (callback != null) {
            callback.onCompletion();
          }
        }
        break;
      case MediaStatus.PLAYER_STATE_BUFFERING:
        state = PlaybackStateCompat.STATE_BUFFERING;
        if (callback != null) {
          callback.onPlaybackStatusChanged(state);
        }
        break;
      case MediaStatus.PLAYER_STATE_PLAYING:
        state = PlaybackStateCompat.STATE_PLAYING;
        setMetadataFromRemote();
        if (callback != null) {
          callback.onPlaybackStatusChanged(state);
        }
        break;
      case MediaStatus.PLAYER_STATE_PAUSED:
        state = PlaybackStateCompat.STATE_PAUSED;
        setMetadataFromRemote();
        if (callback != null) {
          callback.onPlaybackStatusChanged(state);
        }
        break;
      default: // case unknown
        Timber.d("State default : %s", status);
        break;
    }
  }

  private void setMetadataFromRemote() {
    // Sync: We get the customData from the remote media information and update the local
    // metadata if it happens to be different from the one we are currently using.
    // This can happen when the app was either restarted/disconnected + connected, or if the
    // app joins an existing session while the Chromecast was playing a queue.
    try {
      MediaInfo mediaInfo = VideoCastManager.getInstance().getRemoteMediaInformation();
      if (mediaInfo == null) {
        return;
      }
      JSONObject customData = mediaInfo.getCustomData();

      if (customData != null && customData.has(ITEM_ID)) {
        String remoteMediaId = customData.getString(ITEM_ID);
        String remoteTrackUri = mediaInfo.getContentId();
        if (!Strings.equals(currentMediaId, remoteMediaId)) {
          currentMediaId = remoteMediaId;
          currentTrackUri = remoteTrackUri;
          if (callback != null) {
            callback.setCurrentMediaId(remoteMediaId);
          }
          updateLastKnownStreamPosition();
        }
      }
    } catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException e) {
      Timber.e(e, "Exception processing update metadata");
    }
  }

  private void loadMedia(String mediaId, String trackUri, boolean autoPlay) throws
      TransientNetworkDisconnectionException, NoConnectionException, JSONException {

    MediaMetadataCompat track = musicProvider.getMetadata(mediaId);
    if (track == null) {
      throw new IllegalArgumentException("Invalid mediaId " + mediaId);
    }
    if (!Strings.equals(mediaId, currentMediaId)) {
      currentMediaId = mediaId;
      currentTrackUri = trackUri;
      currentPosition = 0;
    }
    JSONObject customData = new JSONObject();
    customData.put(ITEM_ID, mediaId);
    MediaInfo media = toCastMediaMetadata(track, customData, trackUri);
    VideoCastManager.getInstance().loadMedia(media, autoPlay, currentPosition, customData);
  }

  /**
   * Helper method to convert a {@link android.media.MediaMetadata} to a
   * {@link com.google.android.gms.cast.MediaInfo} used for sending media to the receiver app.
   *
   * @param track {@link com.google.android.gms.cast.MediaMetadata}
   * @param customData custom data specifies the local mediaId used by the player.
   * @param trackUri track uri/contentId
   * @return mediaInfo {@link com.google.android.gms.cast.MediaInfo}
   */
  private static MediaInfo toCastMediaMetadata(MediaMetadataCompat track,
                                               JSONObject customData, String trackUri) {
    MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

    mediaMetadata.putString(MediaMetadata.KEY_TITLE,
        track.getDescription().getTitle() == null ? "" :
            track.getDescription().getTitle().toString());

    mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE,
        track.getDescription().getSubtitle() == null ? "" :
            track.getDescription().getSubtitle().toString());

    mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST,
        track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST));

    mediaMetadata.putString(MediaMetadata.KEY_ALBUM_TITLE,
        track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));

    WebImage image = new WebImage(
        new Uri.Builder().encodedPath(
            track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
            .build());
    // First image is used by the receiver for showing the audio album art.
    mediaMetadata.addImage(image);
    // Second image is used by Cast Companion Library on the full screen activity that is shown
    // when the cast dialog is clicked.
    mediaMetadata.addImage(image);

    return new MediaInfo.Builder(trackUri)
        .setContentType(MIME_TYPE_AUDIO_MPEG)
        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setMetadata(mediaMetadata)
        .setCustomData(customData)
        .build();
  }
}
