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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;

import timber.log.Timber;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;
import static com.google.android.exoplayer2.C.TIME_UNSET;

/**
 * A class that implements local media playback using
 * {@link com.google.android.exoplayer2.ExoPlayer}
 */
class LocalPlayback implements Playback, ExoPlayer.EventListener,
    AudioManager.OnAudioFocusChangeListener {

  private static final float VOLUME_DUCK = 0.2f;
  private static final float VOLUME_NORMAL = 1.0f;
  private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
  private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
  private static final int AUDIO_FOCUSED = 2;

  private final IntentFilter audioNoisyIntentFilter = new IntentFilter(ACTION_AUDIO_BECOMING_NOISY);
  private final Context context;
  private final WifiManager.WifiLock wifiLock;
  private final AudioManager audioManager;
  private final MusicController musicController;
  private final DefaultDataSourceFactory dataSourceFactory;
  private final DefaultExtractorsFactory extractorsFactory;
  private SimpleExoPlayer exoPlayer;
  private Callback callback;
  private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
  @State private int state;
  private int exoPlayerState;
  private boolean playOnFocusGain;
  private boolean configWhenReady;
  private volatile boolean audioNoisyReceiverRegistered;
  private volatile long currentPosition;
  private volatile Track currentTrack;

  private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
        if (isPlaying()) {
          musicController.pause();
        }
      }
    }
  };

  LocalPlayback(Context context, MusicController musicController, AudioManager audioManager,
                WifiManager wifiManager) {
    this.context = context;
    this.musicController = musicController;
    this.audioManager = audioManager;
    this.wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "klingar_lock");
    this.state = PlaybackStateCompat.STATE_NONE;
    String agent = Util.getUserAgent(context, context.getResources().getString(R.string.app_name));
    this.dataSourceFactory = new DefaultDataSourceFactory(context, agent, null);
    this.extractorsFactory = new DefaultExtractorsFactory();
  }

  private static String getExoPlayerState(int state) {
    switch (state) {
      case ExoPlayer.STATE_IDLE:
        return "STATE_IDLE";
      case ExoPlayer.STATE_BUFFERING:
        return "STATE_BUFFERING";
      case ExoPlayer.STATE_READY:
        return "STATE_READY";
      case ExoPlayer.STATE_ENDED:
        return "STATE_ENDED";
      default:
        return "UNKNOWN";
    }
  }

  @Override public void start() {
  }

  @Override public void stop(boolean notifyListeners) {
    state = PlaybackStateCompat.STATE_STOPPED;
    if (notifyListeners && callback != null) {
      callback.onPlaybackStatusChanged();
    }
    currentPosition = getCurrentStreamPosition();
    giveUpAudioFocus();
    unregisterAudioNoisyReceiver();
    relaxResources(true);
  }

  @Override @State public int getState() {
    return state;
  }

  @Override public boolean isConnected() {
    return true;
  }

  @Override public boolean isPlaying() {
    return playOnFocusGain || (exoPlayer != null && exoPlayer.getPlayWhenReady());
  }

  @Override public int getCurrentStreamPosition() {
    return exoPlayer != null ? (int) exoPlayer.getCurrentPosition() : (int) currentPosition;
  }

  @Override public void setCurrentStreamPosition(int position) {
    this.currentPosition = position;
  }

  @Override public void updateLastKnownStreamPosition() {
    if (exoPlayer != null) {
      currentPosition = exoPlayer.getCurrentPosition();
    }
  }

  @Override public void play(Track track) {
    Timber.d("play %s", track);
    playOnFocusGain = true;
    tryToGetAudioFocus();
    registerAudioNoisyReceiver();
    boolean mediaHasChanged = !track.equals(currentTrack);
    if (mediaHasChanged) {
      currentPosition = 0;
      currentTrack = track;
    }

    if (state == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && exoPlayer != null) {
      configExoPlayerState();
    } else {
      state = PlaybackStateCompat.STATE_STOPPED;
      relaxResources(false); // release everything except ExoPlayer

      createExoPlayerIfNeeded();

      state = PlaybackStateCompat.STATE_BUFFERING;
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }

      Uri uri = Uri.parse(track.source());
      ExtractorMediaSource source = new ExtractorMediaSource(uri, dataSourceFactory,
          extractorsFactory, null, null);
      configWhenReady = true;
      exoPlayer.setPlayWhenReady(false);
      exoPlayer.prepare(source);

      // If we are streaming from the internet, we want to hold a Wifi lock, which prevents the
      // Wifi radio from going to sleep while the song is playing.
      wifiLock.acquire();
    }
  }

  @Override public void pause() {
    Timber.d("pause");
    if (state == PlaybackStateCompat.STATE_PLAYING) {
      // Pause ExoPlayer and cancel the 'foreground service' state.
      if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
        exoPlayer.setPlayWhenReady(false);
        currentPosition = exoPlayer.getCurrentPosition();
      }
      // while paused, retain ExoPlayer but give up audio focus
      relaxResources(false);
    }
    state = PlaybackStateCompat.STATE_PAUSED;
    if (callback != null) {
      callback.onPlaybackStatusChanged();
    }
    unregisterAudioNoisyReceiver();
  }

  @Override public void seekTo(int position) {
    Timber.d("seekTo %s", position);
    if (exoPlayer == null) {
      // If we do not have a current ExoPlayer, simply update the current position
      currentPosition = position;
    } else {
      if (exoPlayer.getPlayWhenReady()) {
        state = PlaybackStateCompat.STATE_BUFFERING;
        if (callback != null) {
          callback.onPlaybackStatusChanged();
        }
      }
      registerAudioNoisyReceiver();
      long duration = exoPlayer.getDuration();
      long seekPosition = duration == TIME_UNSET ? 0 : Math.min(Math.max(0, position), duration);
      exoPlayer.seekTo(seekPosition);
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

  @Override public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    boolean playerStateChanged = exoPlayerState != playbackState;
    exoPlayerState = playbackState;

    Timber.d("onPlayerStateChanged %s playWhenReady %s playerStateChanged %s configWhenReady %s",
        getExoPlayerState(playbackState), playWhenReady, playerStateChanged, configWhenReady);

    switch (playbackState) {
      case ExoPlayer.STATE_READY:
        if (configWhenReady) {
          configWhenReady = false;
          configExoPlayerState();
        } else if (playWhenReady) { // seek complete
          currentPosition = exoPlayer.getCurrentPosition();
          state = PlaybackStateCompat.STATE_PLAYING;
          if (callback != null) {
            callback.onPlaybackStatusChanged();
          }
        }
        break;
      case ExoPlayer.STATE_ENDED:
        if (playerStateChanged) { // only call onCompletion once
          currentPosition = 0;
          callback.onCompletion();
        }
        break;
      default:
        break;
    }
  }

  @Override public void onTimelineChanged(Timeline timeline, Object manifest) {
  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
  }

  @Override public void onLoadingChanged(boolean isLoading) {
  }

  @Override public void onPlayerError(ExoPlaybackException error) {
    Timber.e(error, "Exception playing song");
    state = PlaybackStateCompat.STATE_ERROR;
    if (callback != null) {
      callback.onPlaybackStatusChanged();
    }
  }

  @Override public void onPositionDiscontinuity() {
  }

  @Override public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
  }

  private void tryToGetAudioFocus() {
    Timber.d("tryToGetAudioFocus");
    int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN);
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      audioFocus = AUDIO_FOCUSED;
    } else {
      audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    }
  }

  private void giveUpAudioFocus() {
    Timber.d("giveUpAudioFocus");
    if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    }
  }

  /**
   * Reconfigures ExoPlayer according to audio focus settings and starts/restarts it.
   * This method starts/restarts the ExoPlayer respecting the current audio focus state.
   * So if we have focus, it will play normally; if we don't have focus, it will either leave
   * ExoPlayer paused or set it to a low volume, depending on what is  allowed by the current
   * focus settings. This method assumes exoPlayer != null, so if you are calling it,
   * you have to do so from a context where you are sure this is the case.
   */
  private void configExoPlayerState() {
    Timber.d("configExoPlayerState audioFocus %s", audioFocus);
    if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
      // If we don't have audio focus and can't duck, we have to pause,
      if (state == PlaybackStateCompat.STATE_PLAYING) {
        pause();
      }
    } else { // we have audio focus
      registerAudioNoisyReceiver();
      if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
        if (exoPlayer != null) {
          exoPlayer.setVolume(VOLUME_DUCK); // we'll be relatively quiet
        }
      } else {
        if (exoPlayer != null) {
          exoPlayer.setVolume(VOLUME_NORMAL); // we can be loud again
        }
      }
      // If we were playing when we lost focus, we need to resume playing.
      if (playOnFocusGain) {
        if (exoPlayer != null && !exoPlayer.getPlayWhenReady()) {
          Timber.d("configExoPlayerState seeking to %s", currentPosition);
          if (currentPosition == exoPlayer.getCurrentPosition()) {
            state = PlaybackStateCompat.STATE_PLAYING;
            exoPlayer.setPlayWhenReady(true);
          } else {
            state = PlaybackStateCompat.STATE_BUFFERING;
            seekTo((int) currentPosition);
            exoPlayer.setPlayWhenReady(true);
          }
          if (callback != null) {
            callback.onPlaybackStatusChanged();
          }
        }
        playOnFocusGain = false;
      }
    }
  }

  @Override public void onAudioFocusChange(int focusChange) {
    Timber.d("onAudioFocusChange focusChange %s", focusChange);
    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      audioFocus = AUDIO_FOCUSED; // We have gained focus
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      // We have lost focus. If we can duck (low playback volume), we can keep playing.
      // Otherwise, we need to pause the playback.
      boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
      audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

      // If we are playing, we need to reset ExoPlayer by calling configExoPlayerState
      // with audioFocus properly set.
      if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
        // If we don't have audio focus and can't duck, we save the information that
        // we were playing, so that we can resume playback once we get the focus back.
        playOnFocusGain = true;
      }
    } else {
      Timber.e("onAudioFocusChange: Ignoring unsupported focusChange: %s", focusChange);
    }
    configExoPlayerState();
  }

  private void createExoPlayerIfNeeded() {
    Timber.d("createExoPlayerIfNeeded %s", exoPlayer == null);
    if (exoPlayer == null) {
      exoPlayer = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(),
          new DefaultLoadControl());
      exoPlayer.addListener(this);
    }
  }

  /**
   * Releases resources used by the service for playback. This includes the
   * "foreground service" status, the wake locks and possibly the ExoPlayer.
   *
   * @param releaseExoPlayer Indicates whether ExoPlayer should also be released or not
   */
  private void relaxResources(boolean releaseExoPlayer) {
    Timber.d("relaxResources releaseExoPlayer %s", releaseExoPlayer);
    // stop and release the ExoPlayer, if it's available
    if (releaseExoPlayer && exoPlayer != null) {
      exoPlayer.removeListener(this);
      exoPlayer.release();
      exoPlayer = null;
    }

    // we can also release the Wifi lock, if we're holding it
    if (wifiLock.isHeld()) {
      wifiLock.release();
    }
  }

  private void registerAudioNoisyReceiver() {
    if (!audioNoisyReceiverRegistered) {
      context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
      audioNoisyReceiverRegistered = true;
    }
  }

  private void unregisterAudioNoisyReceiver() {
    if (audioNoisyReceiverRegistered) {
      context.unregisterReceiver(audioNoisyReceiver);
      audioNoisyReceiverRegistered = false;
    }
  }
}
