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
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import net.simno.klingar.data.model.Track;

import java.io.IOException;

import timber.log.Timber;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;
import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.MediaPlayer.OnSeekCompleteListener;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
class LocalPlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
    OnCompletionListener, OnErrorListener, OnPreparedListener, OnSeekCompleteListener {

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
  private Callback callback;
  private MediaPlayer mediaPlayer;
  private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
  @State private int state;
  private boolean playOnFocusGain;
  private volatile boolean audioNoisyReceiverRegistered;
  private volatile int currentPosition;
  private volatile Track currentTrack;

  private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
        Timber.d("Headphones disconnected");
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
    return playOnFocusGain || (mediaPlayer != null && mediaPlayer.isPlaying());
  }

  @Override public int getCurrentStreamPosition() {
    return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : currentPosition;
  }

  @Override public void setCurrentStreamPosition(int position) {
    this.currentPosition = position;
  }

  @Override public void updateLastKnownStreamPosition() {
    if (mediaPlayer != null) {
      currentPosition = mediaPlayer.getCurrentPosition();
    }
  }

  @Override public void play(Track track) {
    playOnFocusGain = true;
    tryToGetAudioFocus();
    registerAudioNoisyReceiver();
    boolean mediaHasChanged = !track.equals(currentTrack);
    if (mediaHasChanged) {
      currentPosition = 0;
      currentTrack = track;
    }

    if (state == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null) {
      configMediaPlayerState();
    } else {
      state = PlaybackStateCompat.STATE_STOPPED;
      relaxResources(false); // release everything except MediaPlayer

      try {
        createMediaPlayerIfNeeded();

        state = PlaybackStateCompat.STATE_BUFFERING;

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(track.uri().toString());

        // Starts preparing the media player in the background. When
        // it's done, it will call our OnPreparedListener (that is,
        // the onPrepared() method on this class, since we set the
        // listener to 'this'). Until the media player is prepared,
        // we *cannot* call start() on it!
        mediaPlayer.prepareAsync();

        // If we are streaming from the internet, we want to hold a
        // Wifi lock, which prevents the Wifi radio from going to
        // sleep while the song is playing.
        wifiLock.acquire();

        if (callback != null) {
          callback.onPlaybackStatusChanged();
        }
      } catch (IOException e) {
        Timber.e(e, "Exception playing song");
        state = PlaybackStateCompat.STATE_ERROR;
        if (callback != null) {
          callback.onPlaybackStatusChanged();
        }
      }
    }
  }

  @Override public void pause() {
    if (state == PlaybackStateCompat.STATE_PLAYING) {
      // Pause media player and cancel the 'foreground service' state.
      if (mediaPlayer != null && mediaPlayer.isPlaying()) {
        mediaPlayer.pause();
        currentPosition = mediaPlayer.getCurrentPosition();
      }
      // while paused, retain the MediaPlayer but give up audio focus
      relaxResources(false);
    }
    state = PlaybackStateCompat.STATE_PAUSED;
    if (callback != null) {
      callback.onPlaybackStatusChanged();
    }
    unregisterAudioNoisyReceiver();
  }

  @Override public void seekTo(int position) {
    if (mediaPlayer == null) {
      // If we do not have a current media player, simply update the current position
      currentPosition = position;
    } else {
      if (mediaPlayer.isPlaying()) {
        state = PlaybackStateCompat.STATE_BUFFERING;
      }
      registerAudioNoisyReceiver();
      mediaPlayer.seekTo(position);
      if (callback != null) {
        callback.onPlaybackStatusChanged();
      }
    }
  }

  @Override public void setCallback(Callback callback) {
    this.callback = callback;
  }

  @Override public Track getCurrentTrack() {
    return currentTrack;
  }

  @Override public void setCurrentTrack(Track track) {
    this.currentTrack = track;
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
   * Reconfigures MediaPlayer according to audio focus settings and
   * starts/restarts it. This method starts/restarts the MediaPlayer
   * respecting the current audio focus state. So if we have focus, it will
   * play normally; if we don't have focus, it will either leave the
   * MediaPlayer paused or set it to a low volume, depending on what is
   * allowed by the current focus settings. This method assumes player != null,
   * so if you are calling it, you have to do so from a context where
   * you are sure this is the case.
   */
  private void configMediaPlayerState() {
    Timber.d("configMediaPlayerState audioFocus %s", audioFocus);
    if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
      // If we don't have audio focus and can't duck, we have to pause,
      if (state == PlaybackStateCompat.STATE_PLAYING) {
        pause();
      }
    } else { // we have audio focus
      registerAudioNoisyReceiver();
      if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
        mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
      } else {
        if (mediaPlayer != null) {
          mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
        } // else do something for remote client
      }
      // If we were playing when we lost focus, we need to resume playing.
      if (playOnFocusGain) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
          Timber.d("configMediaPlayerState startMediaPlayer  seeking to %s", currentPosition);
          if (currentPosition == mediaPlayer.getCurrentPosition()) {
            mediaPlayer.start();
            state = PlaybackStateCompat.STATE_PLAYING;
          } else {
            mediaPlayer.seekTo(currentPosition);
            state = PlaybackStateCompat.STATE_BUFFERING;
          }
        }
        playOnFocusGain = false;
      }
    }
    if (callback != null) {
      callback.onPlaybackStatusChanged();
    }
  }

  /**
   * Called by AudioManager on audio focus changes.
   * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
   */
  @Override public void onAudioFocusChange(int focusChange) {
    Timber.d("onAudioFocusChange focusChange %s", focusChange);
    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      // We have gained focus:
      audioFocus = AUDIO_FOCUSED;
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      // We have lost focus. If we can duck (low playback volume), we can keep playing.
      // Otherwise, we need to pause the playback.
      boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
      audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

      // If we are playing, we need to reset media player by calling configMediaPlayerState
      // with audioFocus properly set.
      if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
        // If we don't have audio focus and can't duck, we save the information that
        // we were playing, so that we can resume playback once we get the focus back.
        playOnFocusGain = true;
      }
    } else {
      Timber.e("onAudioFocusChange: Ignoring unsupported focusChange: %s", focusChange);
    }
    configMediaPlayerState();
  }

  /**
   * Called when MediaPlayer has completed a seek
   *
   * @see OnSeekCompleteListener
   */
  @Override public void onSeekComplete(MediaPlayer mp) {
    Timber.d("onSeekComplete from MediaPlayer: %s", mp.getCurrentPosition());
    currentPosition = mp.getCurrentPosition();
    if (state == PlaybackStateCompat.STATE_BUFFERING) {
      registerAudioNoisyReceiver();
      mediaPlayer.start();
      state = PlaybackStateCompat.STATE_PLAYING;
    }
    if (callback != null) {
      callback.onPlaybackStatusChanged();
    }
  }

  /**
   * Called when media player is done playing current song.
   *
   * @see OnCompletionListener
   */
  @Override public void onCompletion(MediaPlayer player) {
    Timber.d("onCompletion from MediaPlayer");
    // The media player finished playing the current song, so we go ahead and start the next.
    if (callback != null) {
      callback.onCompletion();
    }
  }

  /**
   * Called when media player is done preparing.
   *
   * @see OnPreparedListener
   */
  @Override public void onPrepared(MediaPlayer player) {
    Timber.d("onPrepared from MediaPlayer");
    // The media player is done preparing. That means we can start playing if we have audio focus.
    configMediaPlayerState();
  }

  /**
   * Called when there's an error playing media. When this happens, the media
   * player goes to the Error state. We warn the user about the error and
   * reset the media player.
   *
   * @see OnErrorListener
   */
  @Override public boolean onError(MediaPlayer mp, int what, int extra) {
    Timber.e("Media player error: what=%s, extra=%s", what, extra);
    state = PlaybackStateCompat.STATE_ERROR;
    if (callback != null) {
      callback.onPlaybackStatusChanged();
    }
    return true; // true indicates we handled the error
  }

  /**
   * Makes sure the media player exists and has been reset. This will create
   * the media player if needed, or reset the existing media player if one
   * already exists.
   */
  private void createMediaPlayerIfNeeded() {
    Timber.d("createMediaPlayerIfNeeded %s", mediaPlayer == null);
    if (mediaPlayer == null) {
      mediaPlayer = new MediaPlayer();

      // Make sure the media player will acquire a wake-lock while
      // playing. If we don't do that, the CPU might go to sleep while the
      // song is playing, causing playback to stop.
      mediaPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

      // we want the media player to notify us when it's ready preparing,
      // and when it's done playing:
      mediaPlayer.setOnPreparedListener(this);
      mediaPlayer.setOnCompletionListener(this);
      mediaPlayer.setOnErrorListener(this);
      mediaPlayer.setOnSeekCompleteListener(this);
    } else {
      mediaPlayer.reset();
    }
  }

  /**
   * Releases resources used by the service for playback. This includes the
   * "foreground service" status, the wake locks and possibly the MediaPlayer.
   *
   * @param releaseMediaPlayer Indicates whether the Media Player should also
   *                           be released or not
   */
  private void relaxResources(boolean releaseMediaPlayer) {
    Timber.d("relaxResources releaseMediaPlayer %s", releaseMediaPlayer);
    // stop and release the Media Player, if it's available
    if (releaseMediaPlayer && mediaPlayer != null) {
      mediaPlayer.reset();
      mediaPlayer.release();
      mediaPlayer = null;
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
