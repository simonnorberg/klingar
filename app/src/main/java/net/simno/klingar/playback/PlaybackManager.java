/*
* Copyright (C) 2014 The Android Open Source Project
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

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import timber.log.Timber;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

  private final QueueManager queueManager;
  private final PlaybackServiceCallback serviceCallback;
  private final MediaSessionCallback mediaSessionCallback;
  private Playback playback;

  public PlaybackManager(PlaybackServiceCallback serviceCallback, QueueManager queueManager,
                         Playback playback) {
    this.serviceCallback = serviceCallback;
    this.queueManager = queueManager;
    this.mediaSessionCallback = new MediaSessionCallback();
    this.playback = playback;
    this.playback.setCallback(this);
  }

  public Playback getPlayback() {
    return playback;
  }

  public MediaSessionCompat.Callback getMediaSessionCallback() {
    return mediaSessionCallback;
  }

  /**
   * Handle a request to play music
   */
  public void handlePlayRequest() {
    Timber.d("play %s", getPlaybackStateAsString(playback.getState()));
    MediaSessionCompat.QueueItem currentItem = queueManager.getCurrentItem();
    if (currentItem != null) {
      serviceCallback.onPlaybackStart();
      playback.play(currentItem);
    }
  }

  /**
   * Handle a request to pause music
   */
  public void handlePauseRequest() {
    Timber.d("pause %s", getPlaybackStateAsString(playback.getState()));
    if (playback.isPlaying()) {
      playback.pause();
      serviceCallback.onPlaybackStop();
    }
  }

  /**
   * Handle a request to stop music
   *
   * @param withError Error message in case the stop has an unexpected cause. The error
   *                  message will be set in the PlaybackState and will be visible to
   *                  MediaController clients.
   */
  public void handleStopRequest(String withError) {
    Timber.d("stop %s error %s", getPlaybackStateAsString(playback.getState()), withError);
    playback.stop(true);
    serviceCallback.onPlaybackStop();
    updatePlaybackState(withError);
  }

  /**
   * Update the current media player state, optionally showing an error message.
   *
   * @param error if not null, error message to present to the user.
   */
  public void updatePlaybackState(String error) {
    Timber.d("update %s", getPlaybackStateAsString(playback.getState()));
    long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
    if (playback != null && playback.isConnected()) {
      position = playback.getCurrentStreamPosition();
    }

    //noinspection ResourceType
    PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
        .setActions(getAvailableActions());

    int state = playback.getState();

    // If there is an error message, send it to the playback state:
    if (error != null) {
      // Error states are really only supposed to be used for errors that cause playback to
      // stop unexpectedly and persist until the user takes action to fix it.
      stateBuilder.setErrorMessage(error);
      state = PlaybackStateCompat.STATE_ERROR;
    }
    //noinspection ResourceType
    stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

    // Set the activeQueueItemId if the current index is valid.
    MediaSessionCompat.QueueItem currentItem = queueManager.getCurrentItem();
    if (currentItem != null) {
      stateBuilder.setActiveQueueItemId(currentItem.getQueueId());
    }

    serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

    if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
      serviceCallback.onNotificationRequired();
    }
  }

  private long getAvailableActions() {
    long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
    if (playback.isPlaying()) {
      actions |= PlaybackStateCompat.ACTION_PAUSE;
    }
    return queueManager.getAvailableActions(actions);
  }

  /**
   * Implementation of the Playback.Callback interface
   */
  @Override
  public void onCompletion() {
    if (queueManager.skipToNext()) {
      handlePlayRequest();
    } else {
      queueManager.reset();
      handleStopRequest(null);
    }
  }

  @Override
  public void onPlaybackStatusChanged(int state) {
    updatePlaybackState(null);
  }

  @Override
  public void onError(String error) {
    updatePlaybackState(error);
  }

  @Override
  public void setCurrentMediaId(String mediaId) {
    queueManager.setCurrentItem(mediaId);
  }

  /**
   * Switch to a different Playback instance, maintaining all playback state, if possible.
   *
   * @param playback switch to this playback
   */
  public void switchToPlayback(Playback playback, boolean resumePlaying) {
    if (playback == null) {
      throw new IllegalArgumentException("Playback cannot be null");
    }
    // suspend the current one.
    int oldState = this.playback.getState();
    int pos = this.playback.getCurrentStreamPosition();
    String currentMediaId = this.playback.getCurrentMediaId();
    this.playback.stop(false);
    playback.setCallback(this);
    playback.setCurrentStreamPosition(pos < 0 ? 0 : pos);
    playback.setCurrentMediaId(currentMediaId);
    playback.start();
    // finally swap the instance
    this.playback = playback;
    switch (oldState) {
      case PlaybackStateCompat.STATE_BUFFERING:
      case PlaybackStateCompat.STATE_CONNECTING:
      case PlaybackStateCompat.STATE_PAUSED:
        this.playback.pause();
        break;
      case PlaybackStateCompat.STATE_PLAYING:
        MediaSessionCompat.QueueItem queueItem = queueManager.getCurrentItem();
        if (resumePlaying && queueItem != null) {
          this.playback.play(queueItem);
        } else if (!resumePlaying) {
          this.playback.pause();
        } else {
          this.playback.stop(true);
        }
        break;
      case PlaybackStateCompat.STATE_NONE:
        break;
    }
  }

  private class MediaSessionCallback extends MediaSessionCompat.Callback {
    @Override
    public void onPlay() {
      Timber.d("onPlay");
      if (queueManager.getCurrentItem() != null) {
        handlePlayRequest();
      }
    }

    @Override
    public void onSkipToQueueItem(long id) {
      Timber.d("onSkipToQueueItem %s", id);
      if (queueManager.setCurrentItem(id)) {
        handlePlayRequest();
      } else {
        Timber.e("onSkipToQueueItem: queueId %s could not be found on queue.", id);
      }
    }

    @Override
    public void onSeekTo(long pos) {
      Timber.d("onSeekTo %s", pos);
      playback.seekTo((int) pos);
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
      Timber.d("onPlayFromMediaId %s", mediaId);
      queueManager.setQueue(mediaId);
      if (queueManager.setCurrentItem(mediaId)) {
        handlePlayRequest();
      } else {
        Timber.e("onPlayFromMediaId: mediaId %s could not be found on queue.", mediaId);
      }
    }

    @Override
    public void onPause() {
      Timber.d("onPause");
      handlePauseRequest();
    }

    @Override
    public void onStop() {
      Timber.d("onStop");
      handleStopRequest(null);
    }

    @Override
    public void onSkipToNext() {
      Timber.d("onSkipToNext");
      if (queueManager.skipToNext()) {
        handlePlayRequest();
      } else {
        Timber.e("onSkipToNext: cannot skip to next");
        handleStopRequest("Cannot skip");
      }
    }

    @Override
    public void onSkipToPrevious() {
      Timber.d("onSkipToPrevious");
      if (queueManager.skipToPrevious()) {
        handlePlayRequest();
      } else {
        Timber.e("onSkipToPrevious: cannot skip to previous.");
        handleStopRequest("Cannot skip");
      }
    }
  }

  public interface PlaybackServiceCallback {
    void onPlaybackStart();
    void onNotificationRequired();
    void onPlaybackStop();
    void onPlaybackStateUpdated(PlaybackStateCompat newState);
  }

  public static String getPlaybackStateAsString(int state) {
    switch (state) {
      case PlaybackStateCompat.STATE_NONE:
        return "STATE_NONE";
      case PlaybackStateCompat.STATE_STOPPED:
        return "STATE_STOPPED";
      case PlaybackStateCompat.STATE_PAUSED:
        return "STATE_PAUSED";
      case PlaybackStateCompat.STATE_PLAYING:
        return "STATE_PLAYING";
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
        return "UNKNOWN STATE";
    }
  }
}
