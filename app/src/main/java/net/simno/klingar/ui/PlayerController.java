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

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.cast.framework.CastButtonFactory;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.QueueManager;
import net.simno.klingar.playback.QueueManager.RepeatMode;
import net.simno.klingar.playback.QueueManager.ShuffleMode;
import net.simno.klingar.ui.adapter.QueueAdapter;
import net.simno.klingar.ui.widget.DividerItemDecoration;
import net.simno.klingar.util.Rx;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static net.simno.klingar.util.Views.gone;
import static net.simno.klingar.util.Views.visible;

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
  @Inject QueueManager queueManager;
  @Inject MusicController musicController;
  @Inject Rx rx;
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

    ActionBar actionBar = null;
    if (getActivity() != null) {
      actionBar = ((KlingarActivity) getActivity()).getSupportActionBar();
    }
    if (actionBar != null) {
      setHasOptionsMenu(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setDisplayShowTitleEnabled(false);
    }

    queueRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    queueRecyclerView.setHasFixedSize(true);
    queueRecyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));

    if (isQueueVisible) {
      gone(background);
      visible(queueRecyclerView);
    } else {
      visible(background);
      gone(queueRecyclerView);
    }

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        elapsedTime.setText(DateUtils.formatElapsedTime(progress));
      }

      @Override public void onStartTrackingTouch(SeekBar seekBar) {
        isSeeking = true;
      }

      @Override public void onStopTrackingTouch(SeekBar seekBar) {
        musicController.seekTo(seekBar.getProgress() * 1000);
        isSeeking = false;
      }
    });

    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    queueRecyclerView.setAdapter(queueAdapter);
    observePlaybackState();
  }

  @Override protected void onDetach(@NonNull View view) {
    super.onDetach(view);
    queueRecyclerView.setAdapter(null);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_main, menu);
    inflater.inflate(R.menu.menu_player, menu);
    CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
        R.id.media_route_menu_item);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    MenuItem item = menu.findItem(R.id.action_queue_track);
    ImageView actionView = (ImageView) item.getActionView();
    actionView.setImageState(isQueueVisible ? TRACK : QUEUE, true);
    actionView.setContentDescription(isQueueVisible ? descTrack : descQueue);
    actionView.setOnClickListener(view -> {
      actionView.setImageState(isQueueVisible ? QUEUE : TRACK, true);
      actionView.setContentDescription(isQueueVisible ? descQueue : descTrack);
      if (isQueueVisible) {
        visible(background);
        gone(queueRecyclerView);
      } else {
        gone(background);
        visible(queueRecyclerView);
      }
      isQueueVisible = !isQueueVisible;
    });
  }

  @OnClick(R.id.player_shuffle) void onClickShuffle() {
    musicController.shuffle();
  }

  @OnClick(R.id.player_repeat) void onClickRepeat() {
    musicController.repeat();
  }

  @OnClick(R.id.player_play_pause) void onClickPlayPause() {
    musicController.playPause();
  }

  @OnClick(R.id.player_next) void onClickNext(ImageView nextButton) {
    ((Animatable) nextButton.getDrawable()).start();
    musicController.next();
  }

  @OnClick(R.id.player_previous) void onClickPrevious(ImageView previousButton) {
    ((Animatable) previousButton.getDrawable()).start();
    musicController.previous();
  }

  private void observePlaybackState() {
    disposables.add(musicController.progress()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(progress -> {
          if (!isSeeking) {
            seekBar.setProgress((int) (progress / 1000));
          }
        }, Rx::onError));

    disposables.add(musicController.state()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(this::updatePlayButton, Rx::onError));

    disposables.add(queueManager.mode()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(pair -> {
          updateShuffleButton(pair.first);
          updateRepeatButton(pair.second);
        }, Rx::onError));

    disposables.add(queueManager.queue()
        .compose(bindUntilEvent(DETACH))
        .compose(rx.flowableSchedulers())
        .subscribe(pair -> {
          queueAdapter.setQueue(pair.first, pair.second);
          updateTrackInfo(pair.first.get(pair.second));
        }, Rx::onError));
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

  private void updateShuffleButton(@ShuffleMode int shuffleMode) {
    if (shuffleMode == QueueManager.SHUFFLE_OFF) {
      shuffleButton.setImageState(SHUFFLE_OFF, true);
      shuffleButton.setContentDescription(descShuffleOff);
    } else if (shuffleMode == QueueManager.SHUFFLE_ALL) {
      shuffleButton.setImageState(SHUFFLE_ALL, true);
      shuffleButton.setContentDescription(descShuffleAll);
    }
  }

  private void updateRepeatButton(@RepeatMode int repeatMode) {
    if (repeatMode == QueueManager.REPEAT_OFF) {
      repeatButton.setImageState(REPEAT_OFF, true);
      repeatButton.setContentDescription(descRepeatOff);
    } else if (repeatMode == QueueManager.REPEAT_ALL) {
      repeatButton.setImageState(REPEAT_ALL, true);
      repeatButton.setContentDescription(descRepeatAll);
    } else if (repeatMode == QueueManager.REPEAT_ONE) {
      repeatButton.setImageState(REPEAT_ONE, true);
      repeatButton.setContentDescription(descRepeatOne);
    }
  }

  private void updateTrackInfo(@NonNull Track track) {
    contentLoading.hide();

    if (getActivity() != null) {
      Glide.with(getActivity())
          .load(track.thumb())
          .transition(withCrossFade())
          .into(background);
    }

    seekBar.setMax((int) track.duration() / 1000);
    totalTime.setText(DateUtils.formatElapsedTime(track.duration() / 1000));

    trackTitle.setText(track.title());
    artistTitle.setText(track.artistTitle());
  }

  @Override public void onTrackClicked(Track track) {
    musicController.playQueueItem(track.queueItemId());
  }
}
