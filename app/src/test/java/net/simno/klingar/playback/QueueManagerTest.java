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

import net.simno.klingar.data.model.Track;
import net.simno.klingar.util.Pair;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import io.reactivex.subscribers.TestSubscriber;
import okhttp3.HttpUrl;

import static net.simno.klingar.playback.QueueManager.REPEAT_ALL;
import static net.simno.klingar.playback.QueueManager.REPEAT_OFF;
import static net.simno.klingar.playback.QueueManager.REPEAT_ONE;
import static net.simno.klingar.playback.QueueManager.SHUFFLE_ALL;
import static net.simno.klingar.playback.QueueManager.SHUFFLE_OFF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class QueueManagerTest {

  private QueueManager queueManager;
  private List<Track> queue;

  @Before public void setup() {
    queueManager = new QueueManager(new Random(1337));
    queue = Arrays.asList(
        createTrack(100),
        createTrack(200),
        createTrack(300),
        createTrack(400),
        createTrack(500));
    queueManager.setQueue(new ArrayList<>(queue), 1000);
  }

  @Test public void currentQueue() {
    TestSubscriber<Pair<List<Track>, Integer>> test = queueManager.queue().take(1).test();
    test.awaitTerminalEvent();

    List<Track> actualQueue = test.values().get(0).first;
    int actualPosition = test.values().get(0).second;

    assertThat(actualQueue, IsIterableContainingInOrder.contains(
        queue.get(0), queue.get(1), queue.get(2), queue.get(3), queue.get(4)));
    assertThat(actualPosition, is(0));
  }

  @Test public void currentTrack() {
    Track actualTrack = queueManager.currentTrack();
    assertThat(actualTrack, is(queue.get(0)));
  }

  @Test public void setQueuePosition() {
    queueManager.setQueuePosition(queue.get(3).queueItemId());
    assertThat(queueManager.currentTrack(), is(queue.get(3)));

    TestSubscriber<Pair<List<Track>, Integer>> test = queueManager.queue().take(1).test();
    test.awaitTerminalEvent();

    int actualPosition = test.values().get(0).second;
    assertThat(actualPosition, is(3));
  }

  @Test public void setExistingTrack() {
    queueManager.setCurrentTrack(queue.get(3));
    assertThat(queueManager.currentTrack(), is(queue.get(3)));
  }

  @Test public void setNewTrack() {
    Track expectedTrack = createTrack(20);
    queueManager.setCurrentTrack(expectedTrack);
    assertThat(queueManager.currentTrack(), is(expectedTrack));
  }

  @Test public void setNewQueueShouldSetShuffleOff() {
    TestSubscriber<Pair<Integer, Integer>> test = queueManager.mode().take(3).test();

    queueManager.shuffle();
    queueManager.setQueue(new ArrayList<>(queue), 2000);

    test.awaitTerminalEvent();
    List<Pair<Integer, Integer>> modes = test.values();

    assertThat(modes.get(0).first, is(SHUFFLE_OFF));
    assertThat(modes.get(1).first, is(SHUFFLE_ALL));
    assertThat(modes.get(2).first, is(SHUFFLE_OFF));
  }

  @Test public void shuffleModes() {
    TestSubscriber<Pair<Integer, Integer>> test = queueManager.mode().take(3).test();

    queueManager.shuffle();
    queueManager.shuffle();

    test.awaitTerminalEvent();
    List<Pair<Integer, Integer>> modes = test.values();

    assertThat(modes.get(0).first, is(SHUFFLE_OFF));
    assertThat(modes.get(1).first, is(SHUFFLE_ALL));
    assertThat(modes.get(2).first, is(SHUFFLE_OFF));
  }

  @Test public void repeatModes() {
    TestSubscriber<Pair<Integer, Integer>> test = queueManager.mode().take(4).test();

    queueManager.repeat();
    queueManager.repeat();
    queueManager.repeat();

    test.awaitTerminalEvent();
    List<Pair<Integer, Integer>> modes = test.values();

    assertThat(modes.get(0).second, is(REPEAT_OFF));
    assertThat(modes.get(1).second, is(REPEAT_ALL));
    assertThat(modes.get(2).second, is(REPEAT_ONE));
    assertThat(modes.get(3).second, is(REPEAT_OFF));
  }

  @Test public void shouldChangeQueueOrderOnShuffle() {
    queueManager.shuffle();

    TestSubscriber<Pair<List<Track>, Integer>> test = queueManager.queue().take(1).test();
    test.awaitTerminalEvent();

    List<Track> shuffledQueue = test.values().get(0).first;

    assertThat(shuffledQueue, IsIterableContainingInOrder.contains(
        queue.get(3), queue.get(4), queue.get(2), queue.get(0), queue.get(1)));
  }

  @Test public void shouldNotChangeCurrentTrackOnShuffle() {
    Track exptectedTrack = queueManager.currentTrack();
    queueManager.shuffle();
    assertThat(queueManager.currentTrack(), is(exptectedTrack));
  }

  @Test public void skipToNextTrackNoRepeat() {
    queueManager.next();
    queueManager.next();
    queueManager.next();
    queueManager.next();
    queueManager.next();

    assertThat(queueManager.getRepeatMode(), is(REPEAT_OFF));
    assertThat(queueManager.currentTrack(), is(queue.get(4)));
    assertThat(queueManager.hasNext(), is(false));
  }

  @Test public void skipToNextTrackRepeatAll() {
    queueManager.repeat();

    queueManager.next();
    queueManager.next();
    queueManager.next();
    queueManager.next();
    queueManager.next();

    assertThat(queueManager.getRepeatMode(), is(REPEAT_ALL));
    assertThat(queueManager.currentTrack(), is(queue.get(0)));
    assertThat(queueManager.hasNext(), is(true));
  }

  @Test public void skipToNextTrackRepeatOne() {
    queueManager.repeat();
    queueManager.repeat();

    queueManager.next();
    queueManager.next();

    assertThat(queueManager.getRepeatMode(), is(REPEAT_ONE));
    assertThat(queueManager.currentTrack(), is(queue.get(0)));
    assertThat(queueManager.hasNext(), is(true));
  }

  @Test public void skipToPreviousTrackNoRepeat() {
    queueManager.previous();
    queueManager.previous();

    assertThat(queueManager.getRepeatMode(), is(REPEAT_OFF));
    assertThat(queueManager.currentTrack(), is(queue.get(0)));
  }

  @Test public void skipToPreviousTrackRepeatAll() {
    queueManager.repeat();

    queueManager.previous();
    queueManager.previous();

    assertThat(queueManager.getRepeatMode(), is(REPEAT_ALL));
    assertThat(queueManager.currentTrack(), is(queue.get(3)));
  }

  @Test public void skipToPreviousTrackRepeatOne() {
    queueManager.repeat();
    queueManager.repeat();

    queueManager.previous();
    queueManager.previous();

    assertThat(queueManager.getRepeatMode(), is(REPEAT_ONE));
    assertThat(queueManager.currentTrack(), is(queue.get(0)));
  }

  private Track createTrack(int index) {
    return Track.builder()
        .queueItemId(index * 10)
        .libraryId("libraryId")
        .key("key")
        .ratingKey("ratingKey")
        .parentKey("parentKey")
        .title("title")
        .albumTitle("albumTitle")
        .artistTitle("artistTitle")
        .index(index)
        .duration(30000)
        .thumb("thumb")
        .source("source")
        .uri(HttpUrl.parse("https://plex.tv"))
        .build();
  }
}
