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

import com.google.auto.value.AutoValue;

import net.simno.klingar.data.model.Track;

import java.util.List;

@AutoValue
public abstract class PlayQueue {

  public static Builder builder() {
    return new AutoValue_PlayQueue.Builder();
  }

  public abstract int index();

  public abstract List<Track> queue();

  public abstract Builder toBuilder();

  public PlayQueue withIndex(int index) {
    return toBuilder().index(index).build();
  }

  public Track currentTrack() {
    return queue().get(index());
  }

  @AutoValue.Builder public abstract static class Builder {
    public abstract Builder index(int index);
    public abstract Builder queue(List<Track> queue);
    public abstract PlayQueue build();
  }
}
