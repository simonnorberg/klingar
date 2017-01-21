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

import android.animation.ObjectAnimator;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.PlayQueue;
import net.simno.klingar.playback.PlayState;
import net.simno.klingar.playback.PlayState.PlayMode;
import net.simno.klingar.playback.PlayState.RepeatMode;
import net.simno.klingar.playback.PlayState.ShuffleMode;
import net.simno.klingar.playback.PlaybackManager;
import net.simno.klingar.ui.adapter.QueueAdapter;
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;

import static net.simno.klingar.ui.ToolbarOwner.TITLE_GONE;

public class PlayerController extends BaseController implements QueueAdapter.OnTrackClickListener {

  private static final int[] PLAY = {-R.attr.state_pause};
  private static final int[] PAUSE = {R.attr.state_pause};
  private static final int[] SHUFFLE_OFF = {-R.attr.state_shuffle_all};
  private static final int[] SHUFFLE_ALL = {R.attr.state_shuffle_all};
  private static final int[] REPEAT_OFF = {-R.attr.state_repeat_all, -R.attr.state_repeat_one};
  private static final int[] REPEAT_ALL = {R.attr.state_repeat_all, -R.attr.state_repeat_one};
  private static final int[] REPEAT_ONE = {-R.attr.state_repeat_all, R.attr.state_repeat_one};
  private static final int[] QUEUE = {-R.attr.state_track};
  private static final int[] TRACK = {R.attr.state_track};
  private final QueueAdapter queueAdapter;
  @BindView(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindView(R.id.player_background_image) ImageView background;
  @BindView(R.id.player_queue) RecyclerView queueRecyclerView;
  @BindView(R.id.player_track_title) TextView trackTitle;
  @BindView(R.id.player_artist_title) TextView artistTitle;
  @BindView(R.id.player_seekbar) SeekBar seekBar;
  @BindView(R.id.player_elapsed_time) TextView elapsedTime;
  @BindView(R.id.player_total_time) TextView totalTime;
  @BindView(R.id.player_shuffle) ImageView shuffleButton;
  @BindView(R.id.player_repeat) ImageView repeatButton;
  @BindView(R.id.player_play_pause) ImageView playPauseButton;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;
  @BindString(R.string.description_play) String descPlay;
  @BindString(R.string.description_pause) String descPause;
  @BindString(R.string.description_shuffle_off) String descShuffleOff;
  @BindString(R.string.description_shuffle_all) String descShuffleAll;
  @BindString(R.string.description_repeat_off) String descRepeatOff;
  @BindString(R.string.description_repeat_all) String descRepeatAll;
  @BindString(R.string.description_repeat_one) String descRepeatOne;
  @BindString(R.string.description_queue) String descQueue;
  @BindString(R.string.description_track) String descTrack;
  @Inject ToolbarOwner toolbarOwner;
  @Inject PlaybackManager playbackManager;
  private boolean isSeeking;
  private boolean isQueueVisible;

  public PlayerController(Bundle args) {
    super(args);
    queueAdapter = new QueueAdapter(this);
  }

  @Override protected int getLayoutResource() {
    return R.layout.controller_player;
  }

  @Override protected void injectDependencies() {
    if (getActivity() != null) {
      KlingarApp.get(getActivity()).component().inject(this);
    }
  }

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    View view = super.onCreateView(inflater, container);

    toolbarOwner.setConfig(ToolbarOwner.Config.builder()
        .background(false)
        .backNavigation(true)
        .titleAlpha(TITLE_GONE)
        .build());

    setHasOptionsMenu(true);

    queueRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    queueRecyclerView.setHasFixedSize(true);
    queueRecyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));
    queueRecyclerView.setAdapter(queueAdapter);

    queueRecyclerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    if (isQueueVisible) {
      queueRecyclerView.animate().alpha(1).setDuration(0).withLayer();
      background.setImageAlpha(0);
    } else {
      queueRecyclerView.animate().alpha(0).setDuration(0).withLayer();
      background.setImageAlpha(255);
    }

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        elapsedTime.setText(DateUtils.formatElapsedTime(progress));
      }

      @Override public void onStartTrackingTouch(SeekBar seekBar) {
        isSeeking = true;
      }

      @Override public void onStopTrackingTouch(SeekBar seekBar) {
        long milliseconds = seekBar.getProgress() * 1000;
        playbackManager.seekTo(milliseconds);
        isSeeking = false;
      }
    });

    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    observePlaybackState();
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    inflater.inflate(R.menu.menu_player, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    MenuItem item = menu.findItem(R.id.action_queue_track);
    ImageView actionView = (ImageView) MenuItemCompat.getActionView(item);
    actionView.setImageState(isQueueVisible ? TRACK : QUEUE, true);
    actionView.setContentDescription(isQueueVisible ? descTrack : descQueue);
    actionView.setOnClickListener(view -> {
      actionView.setImageState(isQueueVisible ? QUEUE : TRACK, true);
      actionView.setContentDescription(isQueueVisible ? descQueue : descTrack);
      toggleQueue();
    });
  }

  private void toggleQueue() {
    if (isQueueVisible) {
      queueRecyclerView.animate().alpha(0).setDuration(200).withLayer();
      background.setImageAlpha(255);
      int width = background.getWidth();
      ViewAnimationUtils.createCircularReveal(background, width, 0, 100, width).start();
    } else {
      ObjectAnimator.ofInt(background, "imageAlpha", 0).setDuration(200).start();
      queueRecyclerView.animate().alpha(1).setDuration(0).withLayer();
      int height = queueRecyclerView.getHeight();
      ViewAnimationUtils.createCircularReveal(queueRecyclerView, 0, height, 100, height).start();
    }
    isQueueVisible = !isQueueVisible;
  }

  @OnClick(R.id.player_shuffle) void onClickShuffle() {
    playbackManager.shuffle();
  }

  @OnClick(R.id.player_repeat) void onClickRepeat() {
    playbackManager.repeat();
  }

  @OnClick(R.id.player_play_pause) void onClickPlayPause() {
    playbackManager.playPause();
  }

  @OnClick(R.id.player_next) void onClickNext(ImageView nextButton) {
    ((Animatable) nextButton.getDrawable()).start();
    playbackManager.next();
  }

  @OnClick(R.id.player_previous) void onClickPrevious(ImageView previousButton) {
    ((Animatable) previousButton.getDrawable()).start();
    playbackManager.previous();
  }

  private void observePlaybackState() {
    subscriptions.add(playbackManager.progress()
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<Long>() {
          @Override public void onNext(Long progress) {
            if (!isSeeking) {
              seekBar.setProgress((int) (progress / 1000));
            }
          }
        }));
    subscriptions.add(playbackManager.state()
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<PlayState>() {
          @Override public void onNext(PlayState state) {
            updatePlayButton(state.playMode());
            updateShuffleButton(state.shuffleMode());
            updateRepeatButton(state.repeatMode());
          }
        }));
    subscriptions.add(playbackManager.queue()
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<PlayQueue>() {
          @Override public void onNext(PlayQueue queue) {
            queueAdapter.setQueue(queue);
            updateTrackInfo(queue.currentTrack());
          }
        }));
  }

  private void updatePlayButton(@PlayMode int playMode) {
    if (playMode == PlayState.PLAYING) {
      playPauseButton.setImageState(PAUSE, true);
      playPauseButton.setContentDescription(descPause);
    } else if (playMode == PlayState.PAUSED) {
      playPauseButton.setImageState(PLAY, true);
      playPauseButton.setContentDescription(descPlay);
    }
  }

  private void updateShuffleButton(@ShuffleMode int shuffleMode) {
    if (shuffleMode == PlayState.SHUFFLE_OFF) {
      shuffleButton.setImageState(SHUFFLE_OFF, true);
      shuffleButton.setContentDescription(descShuffleOff);
    } else if (shuffleMode == PlayState.SHUFFLE_ALL) {
      shuffleButton.setImageState(SHUFFLE_ALL, true);
      shuffleButton.setContentDescription(descShuffleAll);
    }
  }

  private void updateRepeatButton(@RepeatMode int repeatMode) {
    if (repeatMode == PlayState.REPEAT_OFF) {
      repeatButton.setImageState(REPEAT_OFF, true);
      repeatButton.setContentDescription(descRepeatOff);
    } else if (repeatMode == PlayState.REPEAT_ALL) {
      repeatButton.setImageState(REPEAT_ALL, true);
      repeatButton.setContentDescription(descRepeatAll);
    } else if (repeatMode == PlayState.REPEAT_ONE) {
      repeatButton.setImageState(REPEAT_ONE, true);
      repeatButton.setContentDescription(descRepeatOne);
    }
  }

  private void updateTrackInfo(@NonNull Track track) {
    contentLoading.hide();
    Glide.with(getActivity())
        .load(track.thumb())
        .crossFade()
        .into(background);

    seekBar.setMax((int) track.duration() / 1000);
    totalTime.setText(DateUtils.formatElapsedTime(track.duration() / 1000));

    trackTitle.setText(track.title());
    artistTitle.setText(track.artistTitle());
  }

  @Override public void onTrackClicked(int position) {
    playbackManager.playFromQueue(position);
  }
}
