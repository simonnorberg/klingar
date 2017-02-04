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

import android.support.annotation.IntDef;
import android.support.v4.util.Pair;

import com.jakewharton.rxrelay.BehaviorRelay;

import net.simno.klingar.data.model.Track;
import net.simno.klingar.data.model.TrackComparator;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Singleton
public class QueueManager {

  public static final int SHUFFLE_OFF = 1;
  public static final int SHUFFLE_ALL = 2;
  public static final int REPEAT_OFF = 3;
  public static final int REPEAT_ALL = 4;
  public static final int REPEAT_ONE = 5;

  private final BehaviorRelay<Pair<Integer, Integer>> modeRelay = BehaviorRelay.create();
  private final BehaviorRelay<Pair<List<Track>, Integer>> queueRelay = BehaviorRelay.create();

  @ShuffleMode private int shuffleMode = SHUFFLE_OFF;
  @RepeatMode private int repeatMode = REPEAT_OFF;
  private List<Track> queue = Collections.emptyList();
  private int position;

  @Inject public QueueManager() {
    notifyQueue();
    notifyMode();
  }

  public Observable<Pair<Integer, Integer>> mode() {
    return modeRelay.onBackpressureLatest();
  }

  public Observable<Pair<List<Track>, Integer>> queue() {
    return queueRelay.onBackpressureLatest();
  }

  @ShuffleMode int getShuffleMode() {
    return shuffleMode;
  }

  @RepeatMode int getRepeatMode() {
    return repeatMode;
  }

  public void setQueue(List<Track> queue, int position) {
    this.queue = queue;
    this.position = position;
    notifyQueue();
  }

  public Track currentTrack() {
    return queue.get(position);
  }

  void setTrackFromRemote(Track remoteTrack) {
    if (queue.contains(remoteTrack)) {
      setQueuePosition(queue.indexOf(remoteTrack));
    } else {
      setQueue(Collections.singletonList(remoteTrack), 0);
    }
  }

  boolean setQueuePosition(int newPosition) {
    if (position == newPosition) {
      return false;
    }
    position = newPosition;
    notifyQueue();
    return true;
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

  private void sortQueue() {
    Track currentTrack = queue.get(position);
    Collections.sort(queue, new TrackComparator());
    position = Math.max(0, queue.indexOf(currentTrack));
  }

  private void shuffleQueue() {
    Track currentTrack = queue.get(position);
    Collections.shuffle(queue);
    position = Math.max(0, queue.indexOf(currentTrack));
  }

  private void notifyQueue() {
    queueRelay.call(new Pair<>(new ArrayList<>(queue), position));
  }

  private void notifyMode() {
    modeRelay.call(new Pair<>(shuffleMode, repeatMode));
  }

  @Retention(SOURCE)
  @IntDef({SHUFFLE_OFF, SHUFFLE_ALL})
  public @interface ShuffleMode { }

  @Retention(SOURCE)
  @IntDef({REPEAT_OFF, REPEAT_ALL, REPEAT_ONE})
  public @interface RepeatMode { }
}
