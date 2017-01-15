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

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@AutoValue
public abstract class PlayState {

  public static final int PLAYING = 1;
  public static final int PAUSED = 2;
  public static final int SHUFFLE_OFF = 3;
  public static final int SHUFFLE_ALL = 4;
  public static final int REPEAT_OFF = 5;
  public static final int REPEAT_ALL = 6;
  public static final int REPEAT_ONE = 7;

  public static Builder builder() {
    return new AutoValue_PlayState.Builder();
  }

  @PlayState.PlayMode public abstract int playMode();

  @PlayState.ShuffleMode public abstract int shuffleMode();

  @PlayState.RepeatMode public abstract int repeatMode();

  public abstract Builder toBuilder();

  public PlayState withPlayMode(@PlayMode int playMode) {
    return toBuilder().playMode(playMode).build();
  }

  public PlayState withShuffleMode(@ShuffleMode int shuffleMode) {
    return toBuilder().shuffleMode(shuffleMode).build();
  }

  public PlayState withRepeatMode(@RepeatMode int repeatMode) {
    return toBuilder().repeatMode(repeatMode).build();
  }

  @Retention(SOURCE)
  @IntDef({PLAYING, PAUSED})
  public @interface PlayMode { }

  @Retention(SOURCE)
  @IntDef({SHUFFLE_OFF, SHUFFLE_ALL})
  public @interface ShuffleMode { }

  @Retention(SOURCE)
  @IntDef({REPEAT_OFF, REPEAT_ALL, REPEAT_ONE})
  public @interface RepeatMode { }

  @AutoValue.Builder public abstract static class Builder {
    public abstract Builder playMode(@PlayMode int playMode);
    public abstract Builder shuffleMode(@ShuffleMode int shuffleMode);
    public abstract Builder repeatMode(@RepeatMode int repeatMode);
    public abstract PlayState build();
  }
}
