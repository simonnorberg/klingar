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

import com.jakewharton.rxrelay.BehaviorRelay;

import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.PlayState.PlayMode;
import net.simno.klingar.playback.PlayState.RepeatMode;
import net.simno.klingar.playback.PlayState.ShuffleMode;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

import static net.simno.klingar.playback.PlayState.PAUSED;
import static net.simno.klingar.playback.PlayState.PLAYING;
import static net.simno.klingar.playback.PlayState.REPEAT_ALL;
import static net.simno.klingar.playback.PlayState.REPEAT_OFF;
import static net.simno.klingar.playback.PlayState.REPEAT_ONE;
import static net.simno.klingar.playback.PlayState.SHUFFLE_ALL;
import static net.simno.klingar.playback.PlayState.SHUFFLE_OFF;

@Singleton
public class PlaybackManager implements Playback.Listener {

  private final BehaviorRelay<Long> progressRelay = BehaviorRelay.create();
  private final BehaviorRelay<PlayState> stateRelay = BehaviorRelay.create();
  private final BehaviorRelay<PlayQueue> queueRelay = BehaviorRelay.create();
  private final BehaviorRelay<Boolean> playingRelay = BehaviorRelay.create();

  private final Playback playback;
  private PlayQueue queue;
  private PlayState state;
  private long progress;

  @Inject PlaybackManager(Playback playback) {
    this.playback = playback;
    this.playback.setListener(this);
    this.state = PlayState.builder()
        .playMode(PAUSED)
        .shuffleMode(SHUFFLE_OFF)
        .repeatMode(REPEAT_OFF)
        .build();
    this.queue = PlayQueue.builder()
        .index(0)
        .queue(Collections.emptyList())
        .build();
    notifyQueue();
    notifyState();
    playingRelay.call(false);
  }

  public Observable<Long> progress() {
    return progressRelay;
  }

  public Observable<PlayState> state() {
    return stateRelay;
  }

  public Observable<PlayQueue> queue() {
    return queueRelay;
  }

  public Observable<Boolean> isPlaying() {
    return playingRelay;
  }

  public void play(List<Track> queue, int index) {
    this.queue = PlayQueue.builder()
        .index(index)
        .queue(queue)
        .build();
    notifyQueue();
    playCurrentTrack();
  }

  public void playFromQueue(int index) {
    this.queue = queue.withIndex(index);
    notifyQueue();
    playCurrentTrack();
  }

  public void next() {
    @RepeatMode int repeatMode = state.repeatMode();
    if (repeatMode == REPEAT_ONE) {
      seekTo(0);
      return;
    }

    int index = queue.index();

    if ((index + 1) >= queue.queue().size()) {
      if (repeatMode == REPEAT_ALL) {
        index = 0;
      } else {
        index = Math.max(0, queue.queue().size() - 1);
      }
    } else {
      ++index;
    }

    queue = queue.withIndex(index);
    notifyQueue();

    playCurrentTrack();
  }

  public void previous() {
    if (progress > 2000L) {
      seekTo(0);
      return;
    }

    @RepeatMode int repeatMode = state.repeatMode();
    if (repeatMode == REPEAT_ONE) {
      seekTo(0);
      return;
    }

    int index = queue.index();

    if ((index - 1) < 0) {
      if (repeatMode == REPEAT_ALL) {
        index = Math.max(0, queue.queue().size() - 1);
      } else {
        index = 0;
      }
    } else {
      --index;
    }

    queue = queue.withIndex(index);
    notifyQueue();

    playCurrentTrack();
  }

  public void seekTo(long milliseconds) {
    playback.seekTo(milliseconds);
  }

  public void playPause() {
    @PlayMode int playMode = state.playMode();
    if (playMode == PLAYING) {
      playback.pause();
      state = state.withPlayMode(PAUSED);
    } else {
      playback.resume();
      state = state.withPlayMode(PLAYING);
    }
    notifyState();
  }

  public void shuffle() {
    @ShuffleMode int shuffleMode = state.shuffleMode();
    if (shuffleMode == SHUFFLE_OFF) {
      shuffleQueue();
      state = state.withShuffleMode(SHUFFLE_ALL);
    } else {
      sortQueue();
      state = state.withShuffleMode(SHUFFLE_OFF);
    }
    notifyQueue();
    notifyState();
  }

  public void repeat() {
    @RepeatMode int repeatMode = state.repeatMode();
    if (repeatMode == REPEAT_OFF) {
      state = state.withRepeatMode(REPEAT_ALL);
    } else if (repeatMode == REPEAT_ALL) {
      state = state.withRepeatMode(REPEAT_ONE);
    } else {
      state = state.withRepeatMode(REPEAT_OFF);
    }
    notifyState();
  }

  @Override public void onProgress(long progress) {
    this.progress = progress;
    progressRelay.call(progress);
  }

  @Override public void onCompleted() {
    @RepeatMode int repeatMode = state.repeatMode();
    if ((queue.index() + 1) < queue.queue().size() || repeatMode == REPEAT_ONE
        || repeatMode == REPEAT_ALL) {
      next();
    } else {
      state = state.withPlayMode(PAUSED);
      notifyState();
    }
  }

  private void playCurrentTrack() {
    Track track = queue.currentTrack();
    playback.play(track);
    state = state.withPlayMode(PLAYING);
    notifyState();
    playingRelay.call(true);
  }

  private void notifyQueue() {
    queueRelay.call(queue);
  }

  private void notifyState() {
    stateRelay.call(state);
  }

  private void sortQueue() {
    Track currentTrack = queue.currentTrack();
    List<Track> sortedQueue = queue.queue();
    Collections.sort(sortedQueue);
    int index = 0;
    for (int i = 0; i < sortedQueue.size(); ++i) {
      if (currentTrack.equals(sortedQueue.get(i))) {
        index = i;
        break;
      }
    }
    queue = PlayQueue.builder()
        .queue(sortedQueue)
        .index(index)
        .build();
  }

  private void shuffleQueue() {
    Track currentTrack = queue.currentTrack();
    List<Track> shuffledQueue = queue.queue();
    Collections.shuffle(shuffledQueue);
    int index = 0;
    for (int i = 0; i < shuffledQueue.size(); ++i) {
      if (currentTrack.equals(shuffledQueue.get(i))) {
        index = i;
        break;
      }
    }
    queue = PlayQueue.builder()
        .queue(shuffledQueue)
        .index(index)
        .build();
  }
}
