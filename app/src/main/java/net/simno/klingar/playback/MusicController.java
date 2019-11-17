/*
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

import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import androidx.annotation.Nullable;

import com.jakewharton.rxrelay2.BehaviorRelay;

import net.simno.klingar.util.Rx;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
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
import static net.simno.klingar.playback.PlaybackManager.CUSTOM_ACTION_REPEAT;
import static net.simno.klingar.playback.PlaybackManager.CUSTOM_ACTION_SHUFFLE;

public class MusicController {

  private final BehaviorRelay<Long> progressRelay = BehaviorRelay.createDefault(0L);
  private final BehaviorRelay<Integer> stateRelay = BehaviorRelay.createDefault(STATE_NONE);
  private final Flowable<Long> seconds;
  private final Rx rx;
  private MediaControllerCompat mediaController;
  private PlaybackStateCompat playbackState;
  private Disposable progressDisposable;
  private String castName;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
    @Override public void onPlaybackStateChanged(PlaybackStateCompat newPlaybackState) {
      super.onPlaybackStateChanged(newPlaybackState);

      @State int newState = newPlaybackState.getState();
      @State int currentState = stateRelay.getValue() != null ? stateRelay.getValue() : STATE_NONE;

      if (newState != currentState) {
        stateRelay.accept(newState);
        handleProgress(newState, newPlaybackState.getPosition());
      }
      playbackState = newPlaybackState;

      Timber.d("onPlaybackStateChanged %s", getStateString(newState));
    }
  };

  public MusicController(Flowable<Long> seconds, Rx rx) {
    this.seconds = seconds;
    this.rx = rx;
  }

  private static String getStateString(@State int state) {
    switch (state) {
      case STATE_NONE:
        return "STATE_NONE";
      case STATE_STOPPED:
        return "STATE_STOPPED";
      case STATE_PLAYING:
        return "STATE_PLAYING";
      case STATE_PAUSED:
        return "STATE_PAUSED";
      case STATE_FAST_FORWARDING:
        return "STATE_FAST_FORWARDING";
      case STATE_REWINDING:
        return "STATE_REWINDING";
      case STATE_BUFFERING:
        return "STATE_BUFFERING";
      case STATE_ERROR:
        return "STATE_ERROR";
      case STATE_CONNECTING:
        return "STATE_CONNECTING";
      case STATE_SKIPPING_TO_PREVIOUS:
        return "STATE_SKIPPING_TO_PREVIOUS";
      case STATE_SKIPPING_TO_NEXT:
        return "STATE_SKIPPING_TO_NEXT";
      case STATE_SKIPPING_TO_QUEUE_ITEM:
        return "STATE_SKIPPING_TO_QUEUE_ITEM";
      default:
        return "UNKNOWN";
    }
  }

  @Nullable public Token getSessionToken() {
    return mediaController != null ? mediaController.getSessionToken() : null;
  }

  /**
   * Set a new MediaController with the current session token
   */
  void setMediaController(MediaControllerCompat newMediaController) {
    if (mediaController != null) {
      mediaController.unregisterCallback(callback);
      mediaController = null;
    }
    mediaController = newMediaController;
    mediaController.registerCallback(callback);
  }

  public PlaybackStateCompat getPlaybackState() {
    return playbackState;
  }

  @Nullable public String getCastName() {
    return castName;
  }

  void setCastName(@Nullable String castName) {
    this.castName = castName;
  }

  public Flowable<Long> progress() {
    return progressRelay.toFlowable(BackpressureStrategy.LATEST);
  }

  public Flowable<Integer> state() {
    return stateRelay.toFlowable(BackpressureStrategy.LATEST);
  }

  public void play() {
    if (mediaController != null) {
      mediaController.getTransportControls().play();
    }
  }

  public void pause() {
    if (mediaController != null) {
      mediaController.getTransportControls().pause();
    }
  }

  public void stop() {
    if (mediaController != null) {
      mediaController.getTransportControls().stop();
    }
  }

  public void playPause() {
    PlaybackStateCompat state = mediaController.getPlaybackState();
    if (state != null) {
      switch (state.getState()) {
        case STATE_PLAYING:
        case STATE_BUFFERING:
          pause();
          break;
        case STATE_PAUSED:
        case STATE_STOPPED:
          play();
          break;
        case STATE_CONNECTING:
        case STATE_ERROR:
        case STATE_FAST_FORWARDING:
        case STATE_NONE:
        case STATE_REWINDING:
        case STATE_SKIPPING_TO_NEXT:
        case STATE_SKIPPING_TO_PREVIOUS:
        case STATE_SKIPPING_TO_QUEUE_ITEM:
        default:
      }
    }
  }

  public void playQueueItem(long id) {
    if (mediaController != null) {
      mediaController.getTransportControls().skipToQueueItem(id);
    }
  }

  public void seekTo(long milliseconds) {
    if (mediaController != null) {
      mediaController.getTransportControls().seekTo(milliseconds);
    }
  }

  public void next() {
    if (mediaController != null) {
      mediaController.getTransportControls().skipToNext();
    }
  }

  public void previous() {
    if (mediaController != null) {
      mediaController.getTransportControls().skipToPrevious();
    }
  }

  public void shuffle() {
    if (mediaController != null) {
      mediaController.getTransportControls().sendCustomAction(CUSTOM_ACTION_SHUFFLE, null);
    }
  }

  public void repeat() {
    if (mediaController != null) {
      mediaController.getTransportControls().sendCustomAction(CUSTOM_ACTION_REPEAT, null);
    }
  }

  private void handleProgress(@State final int state, final long startPosition) {
    if (state == STATE_PLAYING) {
      stopProgress();
      startProgress(startPosition);
    } else {
      stopProgress();
    }
  }

  private void startProgress(final long startPosition) {
    progressRelay.accept(startPosition);
    progressDisposable = seconds.map(count -> ((count + 1) * 1000) + startPosition)
        .subscribeOn(rx.newThread())
        .subscribe(progressRelay, Rx::onError);
  }

  private void stopProgress() {
    Rx.dispose(progressDisposable);
  }
}
