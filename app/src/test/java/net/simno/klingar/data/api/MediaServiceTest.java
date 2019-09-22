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
package net.simno.klingar.data.api;

import androidx.annotation.NonNull;

import net.simno.klingar.data.api.model.MediaContainer;

import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import retrofit2.http.Url;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MediaServiceTest {

  @NonNull private static final HttpUrl URL =
      Objects.requireNonNull(HttpUrl.parse("https://plex.tv?X-Plex-Token=token"));

  private TestApi api;
  private MediaService media;

  @Before public void setup() {
    api = new TestApi();
    media = new MediaService(api);
  }

  @Test public void sections() {
    media.sections(URL);
    assertThat(api.actual, is("https://plex.tv/library/sections?X-Plex-Token=token"));
  }

  @Test public void albums() {
    media.albums(URL, "artistKey");
    assertThat(api.actual,
        is("https://plex.tv/library/metadata/artistKey/children?X-Plex-Token=token"));
  }

  @Test public void tracks() {
    media.tracks(URL, "albumKey");
    assertThat(api.actual,
        is("https://plex.tv/library/metadata/albumKey/children?X-Plex-Token=token"));
  }

  @Test public void popularTracks() {
    media.popularTracks(URL, "libKey", "artistKey");
    assertThat(api.actual, is("https://plex.tv/library/sections/libKey/all?group=title&limit=5" +
        "&ratingCount%3E=1&sort=ratingCount:desc&type=10&artist.id=artistKey&X-Plex-Token=token"));
  }

  @Test public void browse() {
    media.browse(URL, "libKey", "mediaKey", 100);
    assertThat(api.actual, is("https://plex.tv/library/sections/libKey/all?sort=titleSort:asc" +
        "&X-Plex-Container-Size=50&type=mediaKey&X-Plex-Container-Start=100&X-Plex-Token=token"));
  }

  @Test public void recentArtists() {
    media.recentArtists(URL, "libKey");
    assertThat(api.actual, is("https://plex.tv/library/sections/libKey/all?viewCount%3E=1&type=8" +
        "&sort=lastViewedAt:desc&X-Plex-Token=token"));
  }

  @Test public void firstCharacter() {
    media.firstCharacter(URL, "libKey", "mediaKey");
    assertThat(api.actual, is("https://plex.tv/library/sections/libKey/firstCharacter?" +
        "X-Plex-Token=token&type=mediaKey"));
  }

  @Test public void timeline() {
    media.timeline(URL, 123L, "trackKey", "trackRatingKey", "playing", 300000L, 10000L);
    assertThat(api.actual, is("https://plex.tv/:/timeline?X-Plex-Token=token&playQueueItemID=123" +
        "&key=trackKey&ratingKey=trackRatingKey&state=playing&duration=300000&time=10000"));
  }

  @Test public void playQueue() {
    media.playQueue(URL, "trackKey", "trackParentKey", "libraryId");
    assertThat(api.actual, is("https://plex.tv/playQueues?repeat=0&shuffle=0&type=audio" +
        "&continuous=0&key=trackKey&uri=library%3A%2F%2FlibraryId%2Fitem%2FtrackParentKey" +
        "&X-Plex-Token=token"));
  }

  private static class TestApi implements MediaService.Api {
    private String actual;

    @Override public Observable<MediaContainer> get(@Url HttpUrl url) {
      actual = url.toString();
      return Observable.just(new MediaContainer());
    }

    @Override public Single<MediaContainer> post(@Url HttpUrl url) {
      actual = url.toString();
      return Single.just(new MediaContainer());
    }
  }
}
