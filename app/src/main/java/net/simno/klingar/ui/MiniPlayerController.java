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
package net.simno.klingar.ui;

import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.QueueManager;
import net.simno.klingar.util.Rx;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;

public class MiniPlayerController extends BaseController {

  private static final int[] PLAY = {-R.attr.state_pause};
  private static final int[] PAUSE = {R.attr.state_pause};

  @BindView(R.id.miniplayer_track_title) TextView trackTitle;
  @BindView(R.id.miniplayer_artist_title) TextView artistTitle;
  @BindView(R.id.miniplayer_play_pause) ImageView playPauseButton;
  @BindString(R.string.description_play) String descPlay;
  @BindString(R.string.description_pause) String descPause;
  @Inject MusicController musicController;
  @Inject QueueManager queueManager;
  @Inject Rx rx;

  public MiniPlayerController(Bundle args) {
    super(args);
  }

  @Override protected int getLayoutResource() {
    return R.layout.controller_miniplayer;
  }

  @Override protected void injectDependencies() {
    if (getActivity() != null) {
      KlingarApp.get(getActivity()).component().inject(this);
    }
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    observePlaybackState();
  }

  @OnClick(R.id.miniplayer_play_pause) void onClickPlayPause() {
    musicController.playPause();
  }

  private void observePlaybackState() {
    disposables.add(musicController.state()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(this::updatePlayButton, Rx::onError));
    disposables.add(queueManager.queue()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(pair -> updateTrackInfo(pair.first.get(pair.second)), Rx::onError));
  }

  private void updatePlayButton(@State int state) {
    if (state == PlaybackStateCompat.STATE_PLAYING
        || state == PlaybackStateCompat.STATE_BUFFERING) {
      playPauseButton.setImageState(PAUSE, true);
      playPauseButton.setContentDescription(descPause);
    } else {
      playPauseButton.setImageState(PLAY, true);
      playPauseButton.setContentDescription(descPlay);
    }
  }

  private void updateTrackInfo(@NonNull Track track) {
    trackTitle.setText(track.title());
    artistTitle.setText(" \u2022 ");
    artistTitle.append(track.artistTitle());
  }
}
