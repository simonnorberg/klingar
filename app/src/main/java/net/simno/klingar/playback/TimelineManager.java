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

import net.simno.klingar.data.api.MediaServiceHelper;
import net.simno.klingar.data.api.model.MediaContainer;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

/**
 * Updates the Plex server of current playback status
 */
class TimelineManager {

  private final MusicController musicController;
  private final QueueManager queueManager;
  private final MediaServiceHelper media;
  private Subscription subscription;

  TimelineManager(MusicController musicController, QueueManager queueManager,
                  MediaServiceHelper media) {
    this.musicController = musicController;
    this.queueManager = queueManager;
    this.media = media;
  }

  void start() {
    subscription = Observable.combineLatest(
        musicController.state()
            .filter(state -> state == STATE_PLAYING || state == STATE_PAUSED
                || state == STATE_STOPPED)
            .map(state -> {
              if (state == STATE_PLAYING) {
                return "playing";
              } else if (state == STATE_PAUSED) {
                return "paused";
              }
              return "stopped";
            }),
        queueManager.queue()
            .filter(pair -> pair.second < pair.first.size())
            .map(pair -> pair.first.get(pair.second)),
        musicController.progress()
            .filter(progress -> (progress % 10000) == 0), // Send updates every 10 seconds
        (state, track, time) -> new Timeline(state, time, track))
        .observeOn(Schedulers.io())
        .flatMap(t -> media.timeline(t.track.uri(), t.track.queueItemId(), t.track.key(),
            t.track.ratingKey(), t.state, t.track.duration(), t.time)
            .onErrorReturn(throwable -> new MediaContainer())) // Skip errors
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(new SimpleSubscriber<MediaContainer>() {
          @Override public void onNext(MediaContainer container) {
            Timber.d("Timeline updated");
          }

          @Override public void onError(Throwable e) {
            Timber.e(e, "Timeline update failed");
          }
        });
  }

  void stop() {
    RxHelper.unsubscribe(subscription);
  }

  private static class Timeline {
    private final String state;
    private final long time;
    private final Track track;

    Timeline(String state, long time, Track track) {
      this.state = state;
      this.time = time;
      this.track = track;
    }
  }
}
