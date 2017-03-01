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

import org.junit.Before;
import org.junit.Test;

import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThan;

public class TrackComparatorTest {

  private TrackComparator comparator;

  @Before public void setup() {
    comparator = new TrackComparator();
  }

  @Test public void compareTracks() {
    Track track1 = createTrackWithIndex(10);
    Track track2 = createTrackWithIndex(11);
    int result = comparator.compare(track1, track2);
    assertThat(result, lessThan(0));
  }

  @Test public void compareTracksSameIndex() {
    Track track1 = createTrackWithIndex(10);
    Track track2 = createTrackWithIndex(10);
    int result = comparator.compare(track1, track2);
    assertThat(result, comparesEqualTo(0));
  }

  private Track createTrackWithIndex(int index) {
    return Track.builder()
        .queueItemId(100)
        .libraryId("libraryId")
        .key("key")
        .ratingKey("ratingKey")
        .parentKey("parentKey")
        .title("title")
        .albumTitle("albumTitle")
        .artistTitle("artistTitle")
        .index(index)
        .duration(300)
        .thumb("thumb")
        .source("source")
        .uri(HttpUrl.parse("https://plex.tv"))
        .build();
  }
}
