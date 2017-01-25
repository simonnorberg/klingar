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
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

class MockPlayback implements Playback {

  private Subscription progressSubscription;
  private Listener listener;
  private Track track;
  private long currentPosition;

  @Override public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override public void play(Track track) {
    this.track = track;
    currentPosition = 0;
    startProgress();
  }

  @Override public void resume() {
    startProgress();
  }

  @Override public void pause() {
    stopProgress();
  }

  @Override public void seekTo(long milliseconds) {
    boolean isPlaying = progressSubscription != null && !progressSubscription.isUnsubscribed();
    stopProgress();
    currentPosition = milliseconds;
    if (isPlaying) {
      startProgress();
    }
  }

  private void startProgress() {
    final long duration = track.duration();
    final long startPosition = currentPosition;
    final int remainingSeconds = (int) ((duration - currentPosition) / 1000);

    stopProgress();
    listener.onProgress(currentPosition);
    progressSubscription = Observable.interval(1, TimeUnit.SECONDS)
        .take(remainingSeconds)
        .subscribeOn(Schedulers.newThread())
        .subscribe(new SimpleSubscriber<Long>() {
          @Override public void onCompleted() {
            if (listener != null) {
              listener.onCompleted();
            }
          }

          @Override public void onNext(Long count) {
            currentPosition = ((count + 1) * 1000) + startPosition;
            listener.onProgress(currentPosition);
          }
        });
  }

  private void stopProgress() {
    RxHelper.unsubscribe(progressSubscription);
  }
}
