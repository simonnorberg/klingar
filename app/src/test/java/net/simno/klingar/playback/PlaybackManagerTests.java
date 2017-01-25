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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import rx.observers.TestSubscriber;

import static net.simno.klingar.playback.PlayState.PAUSED;
import static net.simno.klingar.playback.PlayState.PLAYING;
import static net.simno.klingar.playback.PlayState.REPEAT_ALL;
import static net.simno.klingar.playback.PlayState.REPEAT_OFF;
import static net.simno.klingar.playback.PlayState.REPEAT_ONE;
import static net.simno.klingar.playback.PlayState.SHUFFLE_ALL;
import static net.simno.klingar.playback.PlayState.SHUFFLE_OFF;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("WeakerAccess")
public class PlaybackManagerTests {

  @Mock Playback mockPlayback;
  PlaybackManager playbackManager;
  List<Track> queue;

  @Before public void setup() {
    MockitoAnnotations.initMocks(this);
    playbackManager = new PlaybackManager(mockPlayback);
    queue = createQueue();
  }

  @Test public void startQueuePlayback() {
    playbackManager.play(queue, 1);
    verify(mockPlayback, times(1)).play(queue.get(1));

    PlayState expectedState = PlayState.builder()
        .playMode(PLAYING)
        .shuffleMode(SHUFFLE_OFF)
        .repeatMode(REPEAT_OFF)
        .build();
    TestSubscriber<PlayState> stateSub = new TestSubscriber<>();
    playbackManager.state().subscribe(stateSub);
    stateSub.assertValue(expectedState);

    PlayQueue expectedQueue = PlayQueue.builder()
        .queue(queue)
        .index(1)
        .build();
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();
    playbackManager.queue().subscribe(queueSub);
    queueSub.assertValue(expectedQueue);
  }

  @Test public void playTrackOnQueue() {
    playbackManager.play(queue, 1);
    playbackManager.playFromQueue(2);

    verify(mockPlayback, times(1)).play(queue.get(1));

    PlayQueue expectedQueue = PlayQueue.builder()
        .queue(queue)
        .index(2)
        .build();
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();
    playbackManager.queue().subscribe(queueSub);
    queueSub.assertValue(expectedQueue);
  }

  @Test public void skipToNextTrack() {
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();

    playbackManager.play(queue, 1);
    playbackManager.queue().subscribe(queueSub);
    playbackManager.next();

    verify(mockPlayback, times(1)).play(queue.get(1));
    verify(mockPlayback, times(1)).play(queue.get(2));

    PlayQueue expectedQueue1 = PlayQueue.builder()
        .index(1)
        .queue(queue)
        .build();
    PlayQueue expectedQueue2 = expectedQueue1.toBuilder()
        .index(2)
        .build();

    queueSub.assertValues(expectedQueue1, expectedQueue2);
  }

  @Test public void skipToPreviousTrack() {
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();

    playbackManager.play(queue, 1);
    playbackManager.queue().subscribe(queueSub);
    playbackManager.previous();

    verify(mockPlayback, times(1)).play(queue.get(1));
    verify(mockPlayback, times(1)).play(queue.get(0));

    PlayQueue expectedQueue0 = PlayQueue.builder()
        .index(0)
        .queue(queue)
        .build();
    PlayQueue expectedQueue1 = expectedQueue0.toBuilder()
        .index(1)
        .build();

    queueSub.assertValues(expectedQueue1, expectedQueue0);
  }

  @Test public void seekToPosition() {
    playbackManager.play(queue, 0);
    playbackManager.seekTo(1000L);
    verify(mockPlayback, times(1)).seekTo(1000L);
  }

  @Test public void playAndPause() {
    PlayState expectedPlayingState = PlayState.builder()
        .playMode(PLAYING)
        .shuffleMode(SHUFFLE_OFF)
        .repeatMode(REPEAT_OFF)
        .build();
    PlayState expectedPausedState = expectedPlayingState.toBuilder()
        .playMode(PAUSED)
        .build();

    playbackManager.play(queue, 0);

    TestSubscriber<PlayState> stateSub = new TestSubscriber<>();
    playbackManager.state().subscribe(stateSub);
    stateSub.assertValuesAndClear(expectedPlayingState);

    playbackManager.playPause();
    stateSub.assertValue(expectedPausedState);
  }

  @Test public void shuffleAll() {
    TestSubscriber<PlayState> stateSub = new TestSubscriber<>();
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();

    playbackManager.play(queue, 0);
    playbackManager.queue().subscribe(queueSub);
    playbackManager.shuffle();

    playbackManager.state().subscribe(stateSub);
    PlayState expectedPlayingState = PlayState.builder()
        .playMode(PLAYING)
        .shuffleMode(SHUFFLE_ALL)
        .repeatMode(REPEAT_OFF)
        .build();
    stateSub.assertValues(expectedPlayingState);

    PlayQueue sortedQueue = PlayQueue.builder()
        .index(0)
        .queue(queue)
        .build();

    PlayQueue shuffleOff = queueSub.getOnNextEvents().get(0);
    PlayQueue shuffleAll = queueSub.getOnNextEvents().get(1);

    assertThat(shuffleOff, is(sortedQueue));
    assertThat(shuffleAll, is(not(sortedQueue)));
  }

  @Test public void repeatAll() {
    TestSubscriber<PlayState> stateSub = new TestSubscriber<>();
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();

    playbackManager.play(queue, 2);
    playbackManager.repeat();
    playbackManager.queue().subscribe(queueSub);
    playbackManager.next();
    playbackManager.previous();

    verify(mockPlayback, times(1)).play(queue.get(0));
    verify(mockPlayback, times(2)).play(queue.get(2));

    playbackManager.state().subscribe(stateSub);
    PlayState expectedPlayingState = PlayState.builder()
        .playMode(PLAYING)
        .shuffleMode(SHUFFLE_OFF)
        .repeatMode(REPEAT_ALL)
        .build();
    stateSub.assertValues(expectedPlayingState);

    PlayQueue expectedQueue0 = PlayQueue.builder()
        .index(0)
        .queue(queue)
        .build();
    PlayQueue expectedQueue2 = expectedQueue0.toBuilder()
        .index(2)
        .build();
    queueSub.assertValues(expectedQueue2, expectedQueue0, expectedQueue2);
  }

  @Test public void repeatOne() {
    TestSubscriber<PlayState> stateSub = new TestSubscriber<>();
    TestSubscriber<PlayQueue> queueSub = new TestSubscriber<>();

    playbackManager.play(queue, 0);
    playbackManager.repeat();
    playbackManager.repeat();
    playbackManager.queue().subscribe(queueSub);
    playbackManager.next();
    playbackManager.previous();

    verify(mockPlayback, times(2)).play(queue.get(0));
    verify(mockPlayback, times(1)).seekTo(0);
    verify(mockPlayback, never()).play(queue.get(1));
    verify(mockPlayback, never()).play(queue.get(2));

    playbackManager.state().subscribe(stateSub);
    PlayState expectedPlayingState = PlayState.builder()
        .playMode(PLAYING)
        .shuffleMode(SHUFFLE_OFF)
        .repeatMode(REPEAT_ONE)
        .build();
    stateSub.assertValues(expectedPlayingState);

    PlayQueue expectedQueue = PlayQueue.builder()
        .index(0)
        .queue(queue)
        .build();
    queueSub.assertValues(expectedQueue);
  }

  List<Track> createQueue() {
    List<Track> queue = new ArrayList<>();
    queue.add(Track.builder()
        .index(1)
        .albumTitle("album")
        .artistTitle("artist")
        .duration(1337)
        .thumb("thumb")
        .title("title 1")
        .uri(HttpUrl.parse("https://track1.test"))
        .build());

    queue.add(Track.builder()
        .index(2)
        .albumTitle("album")
        .artistTitle("artist")
        .duration(1337)
        .thumb("thumb")
        .title("title 2")
        .uri(HttpUrl.parse("https://track2.test"))
        .build());

    queue.add(Track.builder()
        .index(3)
        .albumTitle("album")
        .artistTitle("artist")
        .duration(1337)
        .thumb("thumb")
        .title("title 3")
        .uri(HttpUrl.parse("https://track3.test"))
        .build());
    return queue;
  }
}
