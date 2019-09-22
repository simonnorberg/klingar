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

import net.simno.klingar.data.HttpUrlTypeAdapter;

import okhttp3.HttpUrl;

@AutoValue
public abstract class Artist implements PlexItem {
  public static Builder builder() {
    return new AutoValue_Artist.Builder();
  }

  public abstract String title();

  public abstract String ratingKey();

  public abstract String libraryKey();

  public abstract String libraryId();

  @Nullable public abstract String art();

  @Nullable public abstract String thumb();

  @ParcelAdapter(HttpUrlTypeAdapter.class) public abstract HttpUrl uri();

  @AutoValue.Builder public abstract static class Builder {
    public abstract Builder title(String name);
    public abstract Builder art(String art);
    public abstract Builder ratingKey(String ratingKey);
    public abstract Builder libraryKey(String libraryKey);
    public abstract Builder libraryId(String libraryId);
    public abstract Builder thumb(String thumb);
    public abstract Builder uri(HttpUrl uri);
    public abstract Artist build();
  }
}
