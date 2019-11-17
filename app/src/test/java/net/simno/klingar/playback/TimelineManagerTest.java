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

import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;

import net.simno.klingar.data.api.MediaService;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.util.Pair;
import net.simno.klingar.util.Rx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import okhttp3.HttpUrl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TimelineManagerTest {

  @NonNull private static final HttpUrl TEST_URL =
      Objects.requireNonNull(HttpUrl.parse("https://plex.tv"));

  @Mock MusicController mockMusicController;
  @Mock QueueManager mockQueueManager;
  @Mock MediaService mockMedia;
  private TimelineManager timelineManager;

  @Before public void setup() {
    timelineManager = new TimelineManager(mockMusicController, mockQueueManager, mockMedia,
        Rx.test());
    when(mockMusicController.progress()).thenReturn(testProgress());
    when(mockMusicController.state()).thenReturn(testState());
    when(mockQueueManager.queue()).thenReturn(testQueue());
  }

  @Test public void sendRegularTimelineUpdates() {
    when(mockMedia.timeline(any(HttpUrl.class), anyLong(), anyString(), anyString(), anyString(),
        anyLong(), anyLong()))
        .thenReturn(Completable.complete());

    timelineManager.start();

    verify(mockMedia, times(1))
        .timeline(TEST_URL, 100, "key", "ratingKey", "playing", 35000, 0);

    verify(mockMedia, times(1))
        .timeline(TEST_URL, 100, "key", "ratingKey", "playing", 35000, 10000);

    verify(mockMedia, times(1))
        .timeline(TEST_URL, 100, "key", "ratingKey", "playing", 35000, 20000);

    verify(mockMedia, times(1))
        .timeline(TEST_URL, 100, "key", "ratingKey", "playing", 35000, 30000);
  }

  @Test public void keepSendingUpdatesAfterTimelineError() {
    when(mockMedia.timeline(any(HttpUrl.class), anyLong(), anyString(), anyString(), anyString(),
        anyLong(), anyLong()))
        .thenReturn(Completable.complete())
        .thenReturn(Completable.error(new IOException()))
        .thenReturn(Completable.error(new IOException()))
        .thenReturn(Completable.complete());

    timelineManager.start();

    verify(mockMedia, times(4)).timeline(any(HttpUrl.class), anyLong(), anyString(), anyString(),
        anyString(), anyLong(), anyLong());
  }

  private Flowable<Pair<List<Track>, Integer>> testQueue() {
    return Flowable.just(new Pair<>(Collections.singletonList(createTrack()), 0));
  }

  private Flowable<Integer> testState() {
    return Flowable.just(PlaybackStateCompat.STATE_PLAYING);
  }

  private Flowable<Long> testProgress() {
    return Flowable.rangeLong(0, 35)
        .map(second -> second * 1000)
        .onBackpressureBuffer();
  }

  private Track createTrack() {
    return Track.builder()
        .queueItemId(100)
        .libraryId("libraryId")
        .key("key")
        .ratingKey("ratingKey")
        .parentKey("parentKey")
        .title("title")
        .albumTitle("albumTitle")
        .artistTitle("artistTitle")
        .index(200)
        .duration(35000)
        .thumb("thumb")
        .source("source")
        .uri(TEST_URL)
        .build();
  }
}
