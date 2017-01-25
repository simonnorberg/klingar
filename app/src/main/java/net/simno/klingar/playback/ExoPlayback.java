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
import android.net.Uri;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
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
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.google.android.exoplayer2.C.TIME_UNSET;

class ExoPlayback implements Playback, ExoPlayer.EventListener {

  private final SimpleExoPlayer player;
  private final DefaultDataSourceFactory dataSourceFactory;
  private final DefaultExtractorsFactory extractorsFactory;

  private Listener listener;
  private Subscription progressSubscription;

  ExoPlayback(Context context) {
    String agent = Util.getUserAgent(context, context.getResources().getString(R.string.app_name));
    dataSourceFactory = new DefaultDataSourceFactory(context, agent, null);
    extractorsFactory = new DefaultExtractorsFactory();
    player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(),
        new DefaultLoadControl());
    player.addListener(this);
  }

  @Override public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override public void play(Track track) {
    Uri uri = Uri.parse(track.uri().toString());
    ExtractorMediaSource source = new ExtractorMediaSource(uri, dataSourceFactory,
        extractorsFactory, null, null);
    player.setPlayWhenReady(true);
    player.prepare(source);
  }

  @Override public void resume() {
    player.setPlayWhenReady(true);
  }

  @Override public void pause() {
    player.setPlayWhenReady(false);
  }

  @Override public void seekTo(long milliseconds) {
    long duration = player.getDuration();
    long seekPosition = duration == TIME_UNSET ? 0 : Math.min(Math.max(0, milliseconds), duration);
    player.seekTo(seekPosition);
  }

  @Override public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    switch (playbackState) {
      case ExoPlayer.STATE_IDLE:
        break;
      case ExoPlayer.STATE_BUFFERING:
        stopProgress();
        break;
      case ExoPlayer.STATE_READY:
        if (playWhenReady) {
          startProgress();
        } else {
          stopProgress();
        }
        break;
      case ExoPlayer.STATE_ENDED:
        stopProgress();
        listener.onCompleted();
        break;
      default:
        break;
    }
  }

  private void startProgress() {
    final long duration = player.getDuration();
    final long startPosition = player.getCurrentPosition();
    final int remainingSeconds = (int) ((duration - startPosition) / 1000);

    stopProgress();
    listener.onProgress(startPosition);
    progressSubscription = Observable.interval(1, TimeUnit.SECONDS)
            .take(remainingSeconds)
            .subscribeOn(Schedulers.newThread())
            .subscribe(new SimpleSubscriber<Long>() {
              @Override public void onNext(Long count) {
                listener.onProgress(((count + 1) * 1000) + startPosition);
              }
            });
  }

  private void stopProgress() {
    RxHelper.unsubscribe(progressSubscription);
  }

  @Override public void onTimelineChanged(Timeline timeline, Object manifest) {
  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
  }

  @Override public void onLoadingChanged(boolean isLoading) {
  }

  @Override public void onPlayerError(ExoPlaybackException error) {
  }

  @Override public void onPositionDiscontinuity() {
  }
}
