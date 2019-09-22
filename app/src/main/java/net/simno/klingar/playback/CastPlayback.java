/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2017 Simon Norberg
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

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import androidx.annotation.NonNull;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaSeekOptions;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.simno.klingar.data.model.Track;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import timber.log.Timber;

/**
 * An implementation of Playback that talks to Cast.
 */
class CastPlayback implements Playback {

  private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
  private static final String CUSTOM_DATA_TRACK = "custom_data_track";

  private final JsonAdapter<Track> jsonAdapter = Track.jsonAdapter(new Moshi.Builder().build());
  private final Context appContext;
  private final RemoteMediaClient remoteMediaClient;
  private final RemoteMediaClient.Callback remoteMediaClientCallback;
  private Callback callback;
  @State private int state;
  private volatile int playerState;
  private volatile int currentPosition;
  private volatile Track currentTrack;

  CastPlayback(Context context) {
    appContext = context.getApplicationContext();
    CastSession castSession = CastContext.getSharedInstance(appContext).getSessionManager()
        .getCurrentCastSession();
    remoteMediaClient = castSession.getRemoteMediaClient();
    remoteMediaClientCallback = new CastMediaClientCallback();
  }

  private static MediaInfo toCastMediaMetadata(Track track, JSONObject customData) {
    MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
    mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.title());
    mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, track.artistTitle());
    mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, track.artistTitle());
    mediaMetadata.putString(MediaMetadata.KEY_ALBUM_TITLE, track.albumTitle());
    WebImage image = new WebImage(new Uri.Builder().encodedPath(track.thumb()).build());
    // First image is used by the receiver for showing the audio album art.
    mediaMetadata.addImage(image);
    // Second image is used by Cast Library when the cast dialog is clicked.
    mediaMetadata.addImage(image);

    //noinspection ResourceType
    return new MediaInfo.Builder(track.source())
        .setContentType(MIME_TYPE_AUDIO_MPEG)
        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setMetadata(mediaMetadata)
        .setCustomData(customData)
        .build();
  }

  private static String getIdleReason(int idleReason) {
    switch (idleReason) {
      case MediaStatus.IDLE_REASON_NONE:
        return "IDLE_REASON_NONE";
      case MediaStatus.IDLE_REASON_FINISHED:
        return "IDLE_REASON_FINISHED";
      case MediaStatus.IDLE_REASON_CANCELED:
        return "IDLE_REASON_CANCELED";
      case MediaStatus.IDLE_REASON_INTERRUPTED:
        return "IDLE_REASON_INTERRUPTED";
      case MediaStatus.IDLE_REASON_ERROR:
        return "IDLE_REASON_ERROR";
      default:
        return "UNKNOWN";
    }
  }

  private static String getPlayerState(int state) {
    switch (state) {
      case MediaStatus.PLAYER_STATE_UNKNOWN:
        return "PLAYER_STATE_UNKNOWN";
      case MediaStatus.PLAYER_STATE_IDLE:
        return "PLAYER_STATE_IDLE";
      case MediaStatus.PLAYER_STATE_BUFFERING:
        return "PLAYER_STATE_BUFFERING";
      case MediaStatus.PLAYER_STATE_PAUSED:
        return "PLAYER_STATE_PAUSED";
      case MediaStatus.PLAYER_STATE_PLAYING:
        return "PLAYER_STATE_PLAYING";
      default:
        return "UNKNOWN";
    }
  }

  @Override public void start() {
    remoteMediaClient.registerCallback(remoteMediaClientCallback);
  }

  @Override public void stop(boolean notifyListeners) {
    remoteMediaClient.unregisterCallback(remoteMediaClientCallback);
    state = PlaybackStateCompat.STATE_STOPPED;
    if (notifyListeners && callback != null) {
      callback.onPlaybackStatusChanged();
    }
  }

  @Override public int getCurrentStreamPosition() {
    if (!isConnected()) {
      return currentPosition;
    }
    return (int) remoteMediaClient.getApproximateStreamPosition();
  }

  @Override public void updateLastKnownStreamPosition() {
    currentPosition = getCurrentStreamPosition();
  }

  @Override public void play(Track track) {
    try {
      loadMedia(track, true);
      state = PlaybackStateCompat.STATE_BUFFERING;
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }
    } catch (JSONException e) {
      Timber.e(e, "Exception loading media");
      state = PlaybackStateCompat.STATE_ERROR;
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }
    }
  }

  @Override public void pause() {
    try {
      if (remoteMediaClient.hasMediaSession()) {
        remoteMediaClient.pause();
        currentPosition = (int) remoteMediaClient.getApproximateStreamPosition();
      } else {
        loadMedia(currentTrack, false);
      }
    } catch (JSONException e) {
      Timber.e(e, "Exception pausing cast playback");
      state = PlaybackStateCompat.STATE_ERROR;
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }
    }
  }

  @Override public void seekTo(int position) {
    if (currentTrack == null) {
      state = PlaybackStateCompat.STATE_ERROR;
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }
      return;
    }
    try {
      if (remoteMediaClient.hasMediaSession()) {
        remoteMediaClient.seek(new MediaSeekOptions.Builder().setPosition(position).build());
        currentPosition = position;
      } else {
        currentPosition = position;
        loadMedia(currentTrack, false);
      }
    } catch (JSONException e) {
      Timber.e(e, "Exception pausing cast playback");
      state = PlaybackStateCompat.STATE_ERROR;
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }
    }
  }

  @Override public Track getCurrentTrack() {
    return currentTrack;
  }

  @Override public void setCurrentTrack(Track track) {
    this.currentTrack = track;
  }

  @Override public void setCallback(Callback callback) {
    this.callback = callback;
  }

  @Override public boolean isConnected() {
    CastSession castSession = CastContext.getSharedInstance(appContext).getSessionManager()
        .getCurrentCastSession();
    return castSession != null && castSession.isConnected();
  }

  @Override public boolean isPlaying() {
    return isConnected() && remoteMediaClient.isPlaying();
  }

  @Override @State public int getState() {
    return state;
  }

  private void loadMedia(@NonNull Track track, boolean autoPlay) throws JSONException {
    Timber.d("loadMedia %s %s", track, autoPlay);
    if (!track.equals(currentTrack)) {
      currentTrack = track;
      currentPosition = 0;
    }
    JSONObject customData = new JSONObject();
    customData.put(CUSTOM_DATA_TRACK, jsonAdapter.toJson(track));
    MediaInfo media = toCastMediaMetadata(track, customData);
    MediaLoadOptions loadOptions = new MediaLoadOptions.Builder()
        .setAutoplay(autoPlay)
        .setPlayPosition(currentPosition)
        .setCustomData(customData)
        .build();
    remoteMediaClient.load(media, loadOptions);
  }

  private void setMetadataFromRemote() {
    // Sync: We get the customData from the remote media information and update the local
    // metadata if it happens to be different from the one we are currently using.
    // This can happen when the app was either restarted/disconnected + connected, or if the
    // app joins an existing session while the Chromecast was playing a queue.
    try {
      MediaInfo mediaInfo = remoteMediaClient.getMediaInfo();
      if (mediaInfo == null) {
        return;
      }
      JSONObject customData = mediaInfo.getCustomData();
      if (customData != null && customData.has(CUSTOM_DATA_TRACK)) {
        Track remoteTrack = jsonAdapter.fromJson(customData.getString(CUSTOM_DATA_TRACK));
        Timber.d("setMetadataFromRemote %s", remoteTrack);
        if (!Objects.equals(remoteTrack, currentTrack)) {
          currentTrack = remoteTrack;
          if (callback != null) {
            callback.setCurrentTrack(remoteTrack);
          }
          updateLastKnownStreamPosition();
        }
      }
    } catch (JSONException | IOException e) {
      Timber.e(e, "Exception processing update metadata");
    }
  }

  private void updatePlaybackState() {
    int newPlayerState = remoteMediaClient.getPlayerState();
    int idleReason = remoteMediaClient.getIdleReason();

    Timber.d("updatePlaybackState %s %s", getPlayerState(playerState), getIdleReason(idleReason));

    if (newPlayerState == playerState) {
      return;
    }

    playerState = newPlayerState;
    switch (playerState) {
      case MediaStatus.PLAYER_STATE_IDLE:
        if (idleReason == MediaStatus.IDLE_REASON_FINISHED) {
          if (callback != null) {
            currentPosition = 0;
            callback.onCompletion();
          }
        }
        break;
      case MediaStatus.PLAYER_STATE_BUFFERING:
        state = PlaybackStateCompat.STATE_BUFFERING;
        if (callback != null) {
          callback.onPlaybackStatusChanged();
        }
        break;
      case MediaStatus.PLAYER_STATE_PLAYING:
        state = PlaybackStateCompat.STATE_PLAYING;
        setMetadataFromRemote();
        if (callback != null) {
          callback.onPlaybackStatusChanged();
        }
        break;
      case MediaStatus.PLAYER_STATE_PAUSED:
        state = PlaybackStateCompat.STATE_PAUSED;
        setMetadataFromRemote();
        if (callback != null) {
          callback.onPlaybackStatusChanged();
        }
        break;
      default:
    }
  }

  private class CastMediaClientCallback extends RemoteMediaClient.Callback {
    @Override public void onMetadataUpdated() {
      Timber.d("onMetadataUpdated");
      setMetadataFromRemote();
    }

    @Override public void onStatusUpdated() {
      Timber.d("onStatusUpdated");
      updatePlaybackState();
    }
  }
}
