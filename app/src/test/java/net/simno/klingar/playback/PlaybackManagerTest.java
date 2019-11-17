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

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import net.simno.klingar.AndroidClock;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.PlaybackManager.PlaybackServiceCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import okhttp3.HttpUrl;

import static net.simno.klingar.playback.PlaybackManager.CUSTOM_ACTION_REPEAT;
import static net.simno.klingar.playback.PlaybackManager.CUSTOM_ACTION_SHUFFLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackManagerTest {

  @Mock QueueManager mockQueueManager;
  @Mock PlaybackServiceCallback mockServiceCallback;
  @Mock Playback mockPlayback;
  @Mock AndroidClock mockAndroidClock;
  private PlaybackManager playbackManager;
  private MediaSessionCompat.Callback mediaSessionCallback;

  @Before public void setup() {
    playbackManager = new PlaybackManager(mockQueueManager, mockServiceCallback, mockAndroidClock,
        mockPlayback);
    mediaSessionCallback = playbackManager.getMediaSessionCallback();
  }

  @Test public void onPlayEvent() {
    Track track = createTrack();
    when(mockQueueManager.currentTrack()).thenReturn(track);

    mediaSessionCallback.onPlay();

    verify(mockPlayback, times(1)).play(track);
    verify(mockServiceCallback, times(1)).onPlaybackStart();
  }

  @Test public void onSkipToQueueItemEvent() {
    Track track = createTrack();
    when(mockQueueManager.currentTrack()).thenReturn(track);

    mediaSessionCallback.onSkipToQueueItem(100);

    verify(mockQueueManager, times(1)).setQueuePosition(100);
  }

  @Test public void onPauseEventWhenPlaying() {
    when(mockPlayback.isPlaying()).thenReturn(true);

    mediaSessionCallback.onPause();

    verify(mockPlayback, times(1)).pause();
    verify(mockServiceCallback, times(1)).onPlaybackStop();
  }

  @Test public void onPauseEventWhenNotPlaying() {
    when(mockPlayback.isPlaying()).thenReturn(false);

    mediaSessionCallback.onPause();

    verify(mockPlayback, never()).pause();
    verifyNoInteractions(mockServiceCallback);
  }

  @Test public void onSkipToNextEvent() {
    mediaSessionCallback.onSkipToNext();
    verify(mockQueueManager, times(1)).next();
  }

  @Test public void onSkipToPreviousEventShortProgress() {
    when(mockPlayback.getCurrentStreamPosition()).thenReturn(1500);

    mediaSessionCallback.onSkipToPrevious();

    verify(mockQueueManager, times(1)).previous();
    verify(mockPlayback, never()).seekTo(anyInt());
  }

  @Test public void onSkipToPreviousEventLongProgress() {
    when(mockPlayback.getCurrentStreamPosition()).thenReturn(20000);

    mediaSessionCallback.onSkipToPrevious();

    verify(mockPlayback, times(1)).seekTo(0);
    verifyNoInteractions(mockQueueManager);
  }

  @Test public void onStopEvent() {
    mediaSessionCallback.onStop();
    verify(mockServiceCallback, times(1)).onPlaybackStop();
  }

  @Test public void onSeekToEvent() {
    mediaSessionCallback.onSeekTo(1337);
    verify(mockPlayback, times(1)).seekTo(1337);
  }

  @Test public void onRepeatEvent() {
    mediaSessionCallback.onCustomAction(CUSTOM_ACTION_REPEAT, null);
    verify(mockQueueManager, times(1)).repeat();
  }

  @Test public void onShuffleEvent() {
    mediaSessionCallback.onCustomAction(CUSTOM_ACTION_SHUFFLE, null);
    verify(mockQueueManager, times(1)).shuffle();
  }

  @Test public void onPlaybackStatusChanged() {
    when(mockPlayback.getState())
        .thenReturn(PlaybackStateCompat.STATE_PLAYING)
        .thenReturn(PlaybackStateCompat.STATE_STOPPED);

    playbackManager.onPlaybackStatusChanged();

    verify(mockServiceCallback, times(1)).onPlaybackStateUpdated(any(PlaybackStateCompat.class));
    verify(mockServiceCallback, times(1)).onNotificationRequired();

    playbackManager.onPlaybackStatusChanged();

    verify(mockServiceCallback, times(2)).onPlaybackStateUpdated(any(PlaybackStateCompat.class));
    verifyNoMoreInteractions(mockServiceCallback);
  }

  @Test public void onCompletionShouldPlayNext() {
    when(mockQueueManager.hasNext()).thenReturn(true);
    playbackManager.onCompletion();
    verify(mockQueueManager, times(1)).next();
  }

  @Test public void onCompletionShouldEndPlayback() {
    when(mockQueueManager.hasNext()).thenReturn(false);
    playbackManager.onCompletion();
    verify(mockQueueManager, never()).next();
  }

  @Test public void setCurrentTrack() {
    Track currentTrack = createTrack();
    playbackManager.setCurrentTrack(currentTrack);
    verify(mockQueueManager, times(1)).setCurrentTrack(currentTrack);
  }

  @Test public void switchPlayback() {
    Playback oldPlayback = playbackManager.getPlayback();
    assertThat(oldPlayback, sameInstance(mockPlayback));

    Playback newExpectedPlayback = mock(Playback.class);
    playbackManager.switchToPlayback(newExpectedPlayback, true);

    Playback newActualPlayback = playbackManager.getPlayback();
    assertThat(newActualPlayback, sameInstance(newExpectedPlayback));
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
        .duration(300)
        .thumb("thumb")
        .source("source")
        .uri(HttpUrl.parse("https://plex.tv"))
        .build();
  }
}
