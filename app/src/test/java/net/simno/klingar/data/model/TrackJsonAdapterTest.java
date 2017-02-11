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
package net.simno.klingar.data.model;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.Test;

import java.io.IOException;

import okhttp3.HttpUrl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrackJsonAdapterTest {
  private final Moshi moshi = new Moshi.Builder().build();
  private final JsonAdapter<Track> adapter = Track.jsonAdapter(moshi);

  @Test public void serialization() throws IOException {
    Track exptected = Track.builder()
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

    String json = adapter.toJson(exptected);
    Track actual = adapter.fromJson(json);

    assertThat(actual, is(equalTo(exptected)));
  }
}
