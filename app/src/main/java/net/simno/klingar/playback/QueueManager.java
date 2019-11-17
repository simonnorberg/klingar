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

import androidx.annotation.IntDef;

import com.jakewharton.rxrelay2.BehaviorRelay;

import net.simno.klingar.data.model.Track;
import net.simno.klingar.data.model.TrackComparator;
import net.simno.klingar.util.Pair;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class QueueManager {

  public static final int SHUFFLE_OFF = 1;
  public static final int SHUFFLE_ALL = 2;
  public static final int REPEAT_OFF = 3;
  public static final int REPEAT_ALL = 4;
  public static final int REPEAT_ONE = 5;

  private final BehaviorRelay<Pair<Integer, Integer>> modeRelay =
      BehaviorRelay.createDefault(new Pair<>(SHUFFLE_OFF, REPEAT_OFF));
  private final BehaviorRelay<Pair<List<Track>, Integer>> queueRelay =
      BehaviorRelay.createDefault(new Pair<>(Collections.emptyList(), 0));
  private final Random random;

  @ShuffleMode private int shuffleMode = SHUFFLE_OFF;
  @RepeatMode private int repeatMode = REPEAT_OFF;
  private List<Track> queue = Collections.emptyList();
  private int position;

  public QueueManager(Random random) {
    this.random = random;
  }

  public Flowable<Pair<Integer, Integer>> mode() {
    return modeRelay.toFlowable(BackpressureStrategy.LATEST);
  }

  public Flowable<Pair<List<Track>, Integer>> queue() {
    return queueRelay.toFlowable(BackpressureStrategy.LATEST);
  }

  @ShuffleMode int getShuffleMode() {
    return shuffleMode;
  }

  @RepeatMode int getRepeatMode() {
    return repeatMode;
  }

  public void setQueue(List<Track> queue, long queueItemId) {
    this.queue = queue;
    setQueuePosition(queueItemId);
    notifyQueue();

    if (shuffleMode != SHUFFLE_OFF) {
      shuffleMode = SHUFFLE_OFF;
      notifyMode();
    }
  }

  public Track currentTrack() {
    return queue.get(position);
  }

  void setCurrentTrack(Track currentTrack) {
    if (queue.contains(currentTrack)) {
      setQueuePosition(currentTrack.queueItemId());
    } else {
      setQueue(Collections.singletonList(currentTrack), currentTrack.queueItemId());
    }
  }

  void setQueuePosition(long queueItemId) {
    int newPosition = getPositionFromQueueItem(queueItemId);
    if (newPosition != position) {
      position = newPosition;
      notifyQueue();
    }
  }

  void next() {
    if (repeatMode == REPEAT_ONE) {
      return;
    }

    int newPosition = position;

    if ((newPosition + 1) >= queue.size()) {
      if (repeatMode == REPEAT_ALL) {
        newPosition = 0;
      } else {
        newPosition = Math.max(0, queue.size() - 1);
      }
    } else {
      ++newPosition;
    }

    position = newPosition;
    notifyQueue();
  }

  void previous() {
    if (repeatMode == REPEAT_ONE) {
      return;
    }

    int newPosition = position;

    if ((newPosition - 1) < 0) {
      if (repeatMode == REPEAT_ALL) {
        newPosition = Math.max(0, queue.size() - 1);
      } else {
        newPosition = 0;
      }
    } else {
      --newPosition;
    }

    position = newPosition;
    notifyQueue();
  }

  void shuffle() {
    if (shuffleMode == SHUFFLE_OFF) {
      shuffleQueue();
      shuffleMode = SHUFFLE_ALL;
    } else {
      sortQueue();
      shuffleMode = SHUFFLE_OFF;
    }
    notifyQueue();
    notifyMode();
  }

  void repeat() {
    if (repeatMode == REPEAT_OFF) {
      repeatMode = REPEAT_ALL;
    } else if (repeatMode == REPEAT_ALL) {
      repeatMode = REPEAT_ONE;
    } else {
      repeatMode = REPEAT_OFF;
    }
    notifyMode();
  }

  boolean hasNext() {
    return (position + 1) < queue.size() || repeatMode == REPEAT_ONE || repeatMode == REPEAT_ALL;
  }

  private int getPositionFromQueueItem(long id) {
    for (int position = 0; position < queue.size(); ++position) {
      if (queue.get(position).queueItemId() == id) {
        return position;
      }
    }
    return 0;
  }

  private void sortQueue() {
    Track currentTrack = queue.get(position);
    Collections.sort(queue, new TrackComparator());
    position = Math.max(0, queue.indexOf(currentTrack));
  }

  private void shuffleQueue() {
    Track currentTrack = queue.get(position);
    Collections.shuffle(queue, random);
    position = Math.max(0, queue.indexOf(currentTrack));
  }

  private void notifyQueue() {
    queueRelay.accept(new Pair<>(new ArrayList<>(queue), position));
  }

  private void notifyMode() {
    modeRelay.accept(new Pair<>(shuffleMode, repeatMode));
  }

  @Retention(SOURCE)
  @IntDef({SHUFFLE_OFF, SHUFFLE_ALL})
  public @interface ShuffleMode { }

  @Retention(SOURCE)
  @IntDef({REPEAT_OFF, REPEAT_ALL, REPEAT_ONE})
  public @interface RepeatMode { }
}
