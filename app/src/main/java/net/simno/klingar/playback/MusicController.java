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

import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import com.jakewharton.rxrelay.BehaviorRelay;

import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static net.simno.klingar.playback.PlaybackManager.CUSTOM_ACTION_REPEAT;
import static net.simno.klingar.playback.PlaybackManager.CUSTOM_ACTION_SHUFFLE;

@Singleton
public class MusicController {

  private final BehaviorRelay<Long> progressRelay = BehaviorRelay.create();
  private final BehaviorRelay<Integer> stateRelay = BehaviorRelay.create();
  private final Context context;
  private MediaControllerCompat mediaController;
  private Token token;
  private PlaybackStateCompat playbackState;
  private Subscription progressSubscription;
  private String castName;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
    @Override public void onPlaybackStateChanged(PlaybackStateCompat newState) {
      super.onPlaybackStateChanged(newState);
      Timber.d("onPlaybackStateChanged %s", getStateString(newState.getState()));
      @State int currentState = stateRelay.getValue() != null ? stateRelay.getValue()
          : PlaybackStateCompat.STATE_NONE;
      if (newState.getState() != currentState) {
        stateRelay.call(newState.getState());
        handleProgress(newState);
      }
      playbackState = newState;
    }
  };

  @Inject public MusicController(Context context) {
    this.context = context;
    progressRelay.call(0L);
  }

  private static String getStateString(@State int state) {
    switch (state) {
      case PlaybackStateCompat.STATE_STOPPED:
        return "STATE_STOPPED";
      case PlaybackStateCompat.STATE_PLAYING:
        return "STATE_PLAYING";
      case PlaybackStateCompat.STATE_PAUSED:
        return "STATE_PAUSED";
      case PlaybackStateCompat.STATE_FAST_FORWARDING:
        return "STATE_FAST_FORWARDING";
      case PlaybackStateCompat.STATE_REWINDING:
        return "STATE_REWINDING";
      case PlaybackStateCompat.STATE_BUFFERING:
        return "STATE_BUFFERING";
      case PlaybackStateCompat.STATE_ERROR:
        return "STATE_ERROR";
      case PlaybackStateCompat.STATE_CONNECTING:
        return "STATE_CONNECTING";
      case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
        return "STATE_SKIPPING_TO_PREVIOUS";
      case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
        return "STATE_SKIPPING_TO_NEXT";
      case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
        return "STATE_SKIPPING_TO_QUEUE_ITEM";
      default:
        return "STATE_NONE";
    }
  }

  public Token getSessionToken() {
    return token;
  }

  void setSessionToken(Token token) {
    this.token = token;
    if (mediaController != null) {
      mediaController.unregisterCallback(callback);
      mediaController = null;
    }
    try {
      mediaController = new MediaControllerCompat(context, token);
      mediaController.registerCallback(callback);
    } catch (RemoteException e) {
      Timber.e(e, "failed to create controller");
    }
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

  public Observable<Long> progress() {
    return progressRelay.onBackpressureLatest();
  }

  public Observable<Integer> state() {
    return stateRelay.onBackpressureLatest();
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

  public void playPause() {
    PlaybackStateCompat state = mediaController.getPlaybackState();
    if (state != null) {
      switch (state.getState()) {
        case PlaybackStateCompat.STATE_PLAYING:
        case PlaybackStateCompat.STATE_BUFFERING:
          pause();
          break;
        case PlaybackStateCompat.STATE_PAUSED:
        case PlaybackStateCompat.STATE_STOPPED:
          play();
          break;
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

  private void handleProgress(PlaybackStateCompat state) {
    final long startPosition = state.getPosition();
    if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
      stopProgress();
      startProgress(startPosition);
    } else {
      stopProgress();
    }
  }

  private void startProgress(final long startPosition) {
    progressRelay.call(startPosition);
    progressSubscription = Observable.interval(1, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.newThread())
        .subscribe(new SimpleSubscriber<Long>() {
          @Override public void onNext(Long count) {
            progressRelay.call(((count + 1) * 1000) + startPosition);
          }
        });
  }

  private void stopProgress() {
    RxHelper.unsubscribe(progressSubscription);
  }
}
