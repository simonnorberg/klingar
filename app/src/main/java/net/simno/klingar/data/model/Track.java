/*
 * Copyright (C) 2016 Simon Norberg
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
package net.simno.klingar.data.model;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.parcel.ParcelAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.simno.klingar.data.HttpUrlTypeAdapter;

import okhttp3.HttpUrl;

@AutoValue
public abstract class Track implements PlexItem {
  public static Builder builder() {
    return new AutoValue_Track.Builder();
  }

  public static JsonAdapter<Track> jsonAdapter(Moshi moshi) {
    return new AutoValue_Track.MoshiJsonAdapter(moshi);
  }

  public abstract long queueItemId();

  public abstract String libraryId();

  public abstract String key();

  public abstract String ratingKey();

  public abstract String parentKey();

  public abstract String title();

  public abstract String albumTitle();

  public abstract String artistTitle();

  public abstract String source();

  public abstract int index();

  public abstract long duration();

  @Nullable public abstract String thumb();

  @ParcelAdapter(HttpUrlTypeAdapter.class) public abstract HttpUrl uri();

  @AutoValue.Builder public abstract static class Builder {
    public abstract Builder queueItemId(long queueItemId);
    public abstract Builder libraryId(String libraryId);
    public abstract Builder key(String key);
    public abstract Builder ratingKey(String ratingKey);
    public abstract Builder parentKey(String parentKey);
    public abstract Builder title(String name);
    public abstract Builder artistTitle(String artistTitle);
    public abstract Builder albumTitle(String albumTitle);
    public abstract Builder source(String source);
    public abstract Builder index(int index);
    public abstract Builder duration(long duration);
    public abstract Builder thumb(String thumb);
    public abstract Builder uri(HttpUrl uri);
    public abstract Track build();
  }
}
