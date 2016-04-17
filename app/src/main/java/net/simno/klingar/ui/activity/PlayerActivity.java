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
package net.simno.klingar.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.format.DateUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.service.MusicService;
import net.simno.klingar.ui.widget.SquareImageView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindDrawable;
import butterknife.OnClick;
import timber.log.Timber;

public class PlayerActivity extends CastBaseActivity {

  private static final long PROGRESS_UPDATE_INTERNAL = 1000;
  private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

  @Inject RequestManager glide;

  @Bind(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @Bind(R.id.player_background_image) SquareImageView background;
  @Bind(R.id.player_track_title) TextView trackTitle;
  @Bind(R.id.player_artist_title) TextView artistTitle;
  @Bind(R.id.player_extra) TextView extra;
  @Bind(R.id.player_seekbar) SeekBar seekBar;
  @Bind(R.id.player_elapsed_time) TextView elapsedTime;
  @Bind(R.id.player_total_time) TextView totalTime;
  @Bind(R.id.playback_controls_play_pause) ImageView playPause;
  @Bind(R.id.playback_controls_shuffle) ImageView shuffle;
  @Bind(R.id.playback_controls_repeat) ImageView repeat;
  @BindDrawable(R.drawable.ic_play_arrow_white_36dp) Drawable playDrawable;
  @BindDrawable(R.drawable.ic_pause_white_36dp) Drawable pauseDrawable;
  @BindDrawable(R.drawable.ic_repeat_white_36dp) Drawable repeatDrawable;
  @BindDrawable(R.drawable.ic_repeat_one_white_36dp) Drawable repeatOneDrawable;

  // Temporary
  private boolean testIsShuffle;
  private boolean testIsRepeat;
  private boolean testIsRepeatOne;

  private MediaBrowserCompat mediaBrowser;
  private PlaybackStateCompat lastPlaybackState;
  private ScheduledFuture<?> scheduleFuture;
  private final Handler handler = new Handler();
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
  private final Runnable updateProgressTask = this::updateProgress;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
    @Override
    public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
      updatePlaybackState(state);
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
      if (metadata != null) {
        updateMediaDescription(metadata.getDescription());
        updateDuration(metadata);
      }
    }
  };

  private final MediaBrowserCompat.ConnectionCallback connectionCallback =
      new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
          try {
            connectToSession(mediaBrowser.getSessionToken());
          } catch (RemoteException e) {
            Timber.e(e, "Could not connect media controller");
          }
        }
      };

  @Override
  void injectDependencies() {
    KlingarApp.get(this).component().inject(this);
  }

  @Override
  int getLayoutResource() {
    return R.layout.activity_player;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("");
    }

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        elapsedTime.setText(DateUtils.formatElapsedTime(progress / 1000));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        stopSeekbarUpdate();
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        getSupportMediaController().getTransportControls().seekTo(seekBar.getProgress());
        scheduleSeekbarUpdate();
      }
    });

    // Only update from the intent if we are not recreating from a config change:
    if (savedInstanceState == null) {
      updateFromParams(getIntent());
    }

    mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class),
        connectionCallback, null);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mediaBrowser != null) {
      mediaBrowser.connect();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mediaBrowser != null) {
      mediaBrowser.disconnect();
    }
    if (getSupportMediaController() != null) {
      getSupportMediaController().unregisterCallback(callback);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopSeekbarUpdate();
    executorService.shutdown();
  }

  private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
    MediaControllerCompat mediaController = new MediaControllerCompat(PlayerActivity.this, token);
    if (mediaController.getMetadata() == null) {
      finish();
      return;
    }
    setSupportMediaController(mediaController);
    mediaController.registerCallback(callback);
    PlaybackStateCompat state = mediaController.getPlaybackState();
    updatePlaybackState(state);
    MediaMetadataCompat metadata = mediaController.getMetadata();
    if (metadata != null) {
      updateMediaDescription(metadata.getDescription());
      updateDuration(metadata);
    }
    updateProgress();
    if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
        state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
      scheduleSeekbarUpdate();
    }
  }

  private void updateFromParams(Intent intent) {
    if (intent != null) {
      MediaDescriptionCompat description = intent.getParcelableExtra(BrowserActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
      if (description != null) {
        updateMediaDescription(description);
      }
    }
  }

  private void scheduleSeekbarUpdate() {
    stopSeekbarUpdate();
    if (!executorService.isShutdown()) {
      scheduleFuture = executorService.scheduleAtFixedRate(
          (Runnable) () -> handler.post(updateProgressTask),
          PROGRESS_UPDATE_INITIAL_INTERVAL,
          PROGRESS_UPDATE_INTERNAL,
          TimeUnit.MILLISECONDS);
    }
  }

  private void stopSeekbarUpdate() {
    if (scheduleFuture != null) {
      scheduleFuture.cancel(false);
    }
  }

  private void updateDuration(MediaMetadataCompat metadata) {
    if (metadata == null) {
      return;
    }
    int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
    seekBar.setMax(duration);
    totalTime.setText(DateUtils.formatElapsedTime(duration / 1000));
  }

  private void updateMediaDescription(MediaDescriptionCompat description) {
    if (description == null) {
      return;
    }
    trackTitle.setText(description.getTitle());
    artistTitle.setText(description.getSubtitle());
    glide.load(description.getIconUri())
        .crossFade()
        .into(background);
  }

  private void updatePlaybackState(PlaybackStateCompat state) {
    if (state == null) {
      return;
    }
    lastPlaybackState = state;
    if (getSupportMediaController() != null && getSupportMediaController().getExtras() != null) {
      String castName = getSupportMediaController().getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
      String castText = castName == null ? "" : getString(R.string.casting_to_device, castName);
      extra.setText(castText);
    }

    switch (state.getState()) {
      case PlaybackStateCompat.STATE_PLAYING:
        playPause.setImageDrawable(pauseDrawable);
        contentLoading.hide();
        scheduleSeekbarUpdate();
        break;
      case PlaybackStateCompat.STATE_PAUSED:
        playPause.setImageDrawable(playDrawable);
        contentLoading.hide();
        stopSeekbarUpdate();
        break;
      case PlaybackStateCompat.STATE_NONE:
      case PlaybackStateCompat.STATE_STOPPED:
        playPause.setImageDrawable(playDrawable);
        contentLoading.hide();
        stopSeekbarUpdate();
        break;
      case PlaybackStateCompat.STATE_BUFFERING:
        contentLoading.show();
        playPause.setImageDrawable(playDrawable);
        extra.setText(R.string.loading);
        stopSeekbarUpdate();
        break;
      default:
        Timber.d("Unhandled state %s", state.getState());
    }
  }

  private void updateProgress() {
    if (lastPlaybackState == null) {
      return;
    }
    long currentPosition = lastPlaybackState.getPosition();
    if (lastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
      // Calculate the elapsed time between the last position update and now and unless paused,
      // we can assume (delta * speed) + current position is approximately the latest position.
      // This ensure that we do not repeatedly call the getPlaybackState() on MediaController.
      long timeDelta = SystemClock.elapsedRealtime() - lastPlaybackState.getLastPositionUpdateTime();
      currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
    }
    seekBar.setProgress((int) currentPosition);
  }

  @OnClick(R.id.playback_controls_shuffle)
  public void onClickShuffle() {
    testIsShuffle = !testIsShuffle;
    shuffle.setSelected(testIsShuffle);
  }

  @OnClick(R.id.playback_controls_skip_prev)
  public void onClickSkipPrev() {
    MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
    controls.skipToPrevious();
  }

  @OnClick(R.id.playback_controls_play_pause)
  public void onClickPlayPause() {
    PlaybackStateCompat state = getSupportMediaController().getPlaybackState();
    if (state != null) {
      MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
      switch (state.getState()) {
        case PlaybackStateCompat.STATE_PLAYING:
        case PlaybackStateCompat.STATE_BUFFERING:
          controls.pause();
          stopSeekbarUpdate();
          break;
        case PlaybackStateCompat.STATE_PAUSED:
        case PlaybackStateCompat.STATE_STOPPED:
          controls.play();
          scheduleSeekbarUpdate();
          break;
        default:
          Timber.d("onClick with state %s", state.getState());
      }
    }
  }

  @OnClick(R.id.playback_controls_skip_next)
  public void onClickSkipNext() {
    MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
    controls.skipToNext();
  }

  @OnClick(R.id.playback_controls_repeat)
  public void onClickRepeat() {
    if (testIsRepeat) {
      testIsRepeat = false;
      testIsRepeatOne = true;
      repeat.setImageDrawable(repeatOneDrawable);
      repeat.setSelected(true);
    } else if (testIsRepeatOne) {
      testIsRepeat = false;
      testIsRepeatOne = false;
      repeat.setImageDrawable(repeatDrawable);
      repeat.setSelected(false);
    } else {
      testIsRepeat = true;
      testIsRepeatOne = false;
      repeat.setImageDrawable(repeatDrawable);
      repeat.setSelected(true);
    }
  }
}
