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
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.PlayQueue;
import net.simno.klingar.playback.PlayState;
import net.simno.klingar.playback.PlaybackManager;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;

public class MiniPlayerController extends BaseController {

  private static final int[] PLAY = {-R.attr.state_pause};
  private static final int[] PAUSE = {R.attr.state_pause};

  @BindView(R.id.miniplayer_track_title) TextView trackTitle;
  @BindView(R.id.miniplayer_artist_title) TextView artistTitle;
  @BindView(R.id.miniplayer_play_pause) ImageView playPauseButton;
  @BindString(R.string.description_play) String descPlay;
  @BindString(R.string.description_pause) String descPause;
  @Inject PlaybackManager playbackManager;

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
    playbackManager.playPause();
  }

  private void observePlaybackState() {
    subscriptions.add(playbackManager.state()
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<PlayState>() {
          @Override public void onNext(PlayState state) {
            updatePlayButton(state.playMode());
          }
        }));
    subscriptions.add(playbackManager.queue()
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<PlayQueue>() {
          @Override public void onNext(PlayQueue queue) {
            updateTrackInfo(queue.currentTrack());
          }
        }));
  }

  private void updatePlayButton(@PlayState.PlayMode int playMode) {
    if (playMode == PlayState.PLAYING) {
      playPauseButton.setImageState(PAUSE, true);
      playPauseButton.setContentDescription(descPause);
    } else if (playMode == PlayState.PAUSED) {
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
