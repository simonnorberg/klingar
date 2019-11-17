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

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import androidx.annotation.NonNull;

import net.simno.klingar.AndroidClock;
import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.QueueManager.RepeatMode;
import net.simno.klingar.playback.QueueManager.ShuffleMode;

import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_FAST_FORWARDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_REWINDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

class PlaybackManager implements Playback.Callback {

  static final String CUSTOM_ACTION_REPEAT = "net.simno.klingar.REPEAT";
  static final String CUSTOM_ACTION_SHUFFLE = "net.simno.klingar.SHUFFLE";

  private final QueueManager queueManager;
  private final MediaSessionCallback sessionCallback;
  private final PlaybackServiceCallback serviceCallback;
  private final AndroidClock androidClock;
  private Playback playback;

  PlaybackManager(QueueManager queueManager, PlaybackServiceCallback serviceCallback,
                  AndroidClock androidClock, Playback playback) {
    this.queueManager = queueManager;
    this.serviceCallback = serviceCallback;
    this.androidClock = androidClock;
    this.playback = playback;
    this.playback.setCallback(this);
    this.sessionCallback = new MediaSessionCallback();
  }

  public Playback getPlayback() {
    return playback;
  }

  MediaSessionCompat.Callback getMediaSessionCallback() {
    return sessionCallback;
  }

  private void handlePlayRequest() {
    Track currentQueueItem = queueManager.currentTrack();
    if (currentQueueItem != null) {
      playback.play(currentQueueItem);
      serviceCallback.onPlaybackStart();
    }
  }

  private void handlePauseRequest() {
    if (playback.isPlaying()) {
      playback.pause();
      serviceCallback.onPlaybackStop();
    }
  }

  void handleStopRequest() {
    playback.stop(true);
    serviceCallback.onPlaybackStop();
    updatePlaybackState();
  }

  void updatePlaybackState() {
    long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    if (playback != null && playback.isConnected()) {
      position = playback.getCurrentStreamPosition();
    }

    PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
        .setActions(getAvailableActions());

    addCustomActions(stateBuilder);

    @State int state = playback.getState();
    stateBuilder.setState(state, position, 1.0f, androidClock.elapsedRealTime());

    serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

    if (state == STATE_PLAYING || state == STATE_PAUSED) {
      serviceCallback.onNotificationRequired();
    }
  }

  private void addCustomActions(PlaybackStateCompat.Builder stateBuilder) {
    @ShuffleMode int shuffleMode = queueManager.getShuffleMode();
    @RepeatMode int repeatMode = queueManager.getRepeatMode();

    if (shuffleMode == QueueManager.SHUFFLE_OFF) {
      stateBuilder.addCustomAction(CUSTOM_ACTION_SHUFFLE, "Shuffle", R.drawable.ic_shuffle_all);
    } else {
      stateBuilder.addCustomAction(CUSTOM_ACTION_SHUFFLE, "Shuffle", R.drawable.ic_shuffle_off);
    }

    if (repeatMode == QueueManager.REPEAT_OFF) {
      stateBuilder.addCustomAction(CUSTOM_ACTION_REPEAT, "Repeat", R.drawable.ic_repeat_all);
    } else if (repeatMode == QueueManager.REPEAT_ALL) {
      stateBuilder.addCustomAction(CUSTOM_ACTION_REPEAT, "Repeat", R.drawable.ic_repeat_one);
    } else {
      stateBuilder.addCustomAction(CUSTOM_ACTION_REPEAT, "Repeat", R.drawable.ic_repeat_off);
    }
  }

  private long getAvailableActions() {
    long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE
        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
    if (playback.isPlaying()) {
      actions |= PlaybackStateCompat.ACTION_PAUSE;
    } else {
      actions |= PlaybackStateCompat.ACTION_PLAY;
    }
    return actions;
  }

  @Override public void onCompletion() {
    Timber.d("onCompletion");
    if (queueManager.hasNext()) {
      queueManager.next();
      handlePlayRequest();
    } else {
      handleStopRequest();
    }
  }

  @Override public void onPlaybackStatusChanged() {
    Timber.d("onPlaybackStatusChanged");
    updatePlaybackState();
  }

  @Override public void setCurrentTrack(Track track) {
    Timber.d("setCurrentTrack %s", track);
    queueManager.setCurrentTrack(track);
  }

  /**
   * Switch to a different Playback instance, maintaining all playback state, if possible.
   *
   * @param newPlayback switch to this playback
   */
  void switchToPlayback(@NonNull Playback newPlayback, boolean resumePlaying) {
    Timber.d("switchToPlayback %s resume %s", newPlayback.getClass().getSimpleName(),
        resumePlaying);
    // Suspend the current one
    @State int oldState = playback.getState();
    int position = playback.getCurrentStreamPosition();
    Track currentMediaId = playback.getCurrentTrack();
    playback.stop(false);
    newPlayback.setCallback(this);
    newPlayback.setCurrentTrack(currentMediaId);
    newPlayback.seekTo(Math.max(position, 0));
    newPlayback.start();
    // Finally swap the instance
    playback = newPlayback;
    switch (oldState) {
      case STATE_BUFFERING:
      case STATE_CONNECTING:
      case STATE_PAUSED:
        playback.pause();
        break;
      case STATE_PLAYING:
        Track currentQueueItem = queueManager.currentTrack();
        if (resumePlaying && currentQueueItem != null) {
          playback.play(currentQueueItem);
        } else if (!resumePlaying) {
          playback.pause();
        } else {
          playback.stop(true);
        }
        break;

      case STATE_ERROR:
      case STATE_FAST_FORWARDING:
      case STATE_NONE:
      case STATE_REWINDING:
      case STATE_SKIPPING_TO_NEXT:
      case STATE_SKIPPING_TO_PREVIOUS:
      case STATE_SKIPPING_TO_QUEUE_ITEM:
      case STATE_STOPPED:
      default:
    }
  }

  interface PlaybackServiceCallback {
    void onPlaybackStart();
    void onPlaybackStop();
    void onNotificationRequired();
    void onPlaybackStateUpdated(PlaybackStateCompat newState);
  }

  private class MediaSessionCallback extends MediaSessionCompat.Callback {
    @Override public void onPlay() {
      Timber.d("onPlay");
      handlePlayRequest();
    }

    @Override public void onSkipToQueueItem(long id) {
      Timber.d("onSkipToQueueItem %s", id);
      queueManager.setQueuePosition(id);
      handlePlayRequest();
    }

    @Override public void onPause() {
      Timber.d("onPause");
      handlePauseRequest();
    }

    @Override public void onSkipToNext() {
      Timber.d("onSkipToNext");
      queueManager.next();
      handlePlayRequest();
    }

    @Override public void onSkipToPrevious() {
      Timber.d("onSkipToPrevious");
      if (playback.getCurrentStreamPosition() > 1500) {
        playback.seekTo(0);
        return;
      }
      queueManager.previous();
      handlePlayRequest();
    }

    @Override public void onStop() {
      Timber.d("onStop");
      handleStopRequest();
    }

    @Override public void onSeekTo(long position) {
      Timber.d("onSeekTo %s", position);
      playback.seekTo((int) position);
    }

    @Override public void onCustomAction(String action, Bundle extras) {
      Timber.d("onCustomAction %s", action);
      switch (action) {
        case CUSTOM_ACTION_REPEAT:
          queueManager.repeat();
          break;
        case CUSTOM_ACTION_SHUFFLE:
          queueManager.shuffle();
          break;
        default:
      }
    }
  }
}
