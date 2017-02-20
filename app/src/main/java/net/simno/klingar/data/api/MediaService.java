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
package net.simno.klingar.data.api;

import net.simno.klingar.data.api.model.MediaContainer;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

public class MediaService {

  private static final String TOKEN = "X-Plex-Token";

  private final Api api;

  MediaService(Api api) {
    this.api = api;
  }

  public Observable<MediaContainer> sections(HttpUrl url) {
    return api.get(url.newBuilder()
        .addPathSegments("library/sections")
        .build());
  }

  public Observable<MediaContainer> albums(HttpUrl url, String artistKey) {
    return api.get(url.newBuilder()
        .addPathSegments("library/metadata")
        .addPathSegment(artistKey)
        .addPathSegment("children")
        .build());
  }

  public Observable<MediaContainer> tracks(HttpUrl url, String albumKey) {
    return api.get(url.newBuilder()
        .addPathSegments("library/metadata")
        .addPathSegment(albumKey)
        .addPathSegment("children")
        .build());
  }

  public Observable<MediaContainer> popularTracks(HttpUrl url, String libKey, String artistKey) {
    return api.get(url.newBuilder()
        .addPathSegments("library/sections")
        .addPathSegment(libKey)
        .addPathSegment("all")
        .query("group=title&limit=5&ratingCount>=1&sort=ratingCount:desc&type=10")
        .addQueryParameter("artist.id", artistKey)
        .addQueryParameter(TOKEN, url.queryParameter(TOKEN))
        .build());
  }

  public Observable<MediaContainer> browse(HttpUrl url, String libKey, String mediaKey,
                                           int offset) {
    return api.get(url.newBuilder()
        .addPathSegments("library/sections")
        .addPathSegment(libKey)
        .addPathSegment("all")
        .query("sort=titleSort:asc&X-Plex-Container-Size=50")
        .addQueryParameter("type", mediaKey)
        .addQueryParameter("X-Plex-Container-Start", String.valueOf(offset))
        .addQueryParameter(TOKEN, url.queryParameter(TOKEN))
        .build());
  }

  public Observable<MediaContainer> recentArtists(HttpUrl url, String libKey) {
    return api.get(url.newBuilder()
        .addPathSegments("library/sections")
        .addPathSegment(libKey)
        .addPathSegment("all")
        .query("viewCount>=1&type=8&sort=lastViewedAt:desc")
        .addQueryParameter(TOKEN, url.queryParameter(TOKEN))
        .build());
  }

  public Observable<MediaContainer> firstCharacter(HttpUrl url, String libKey, String mediaKey) {
    return api.get(url.newBuilder()
        .addPathSegments("library/sections")
        .addPathSegment(libKey)
        .addPathSegment("firstCharacter")
        .addQueryParameter("type", mediaKey)
        .build());
  }

  public Completable timeline(HttpUrl url, long queueItemId, String trackKey, String trackRatingKey,
                              String state, long duration, long time) {
    return api.get(url.newBuilder()
        .addPathSegments(":/timeline")
        .addQueryParameter("playQueueItemID", String.valueOf(queueItemId))
        .addQueryParameter("key", trackKey)
        .addQueryParameter("ratingKey", trackRatingKey)
        .addQueryParameter("state", state)
        .addQueryParameter("duration", String.valueOf(duration))
        .addQueryParameter("time", String.valueOf(time))
        .build())
        .firstOrError()
        .toCompletable();
  }

  public Single<MediaContainer> playQueue(HttpUrl url, String trackKey, String trackParentKey,
                                          String libraryId) {
    return api.post(url.newBuilder()
        .addPathSegments("playQueues")
        .query("repeat=0&shuffle=0&type=audio&continuous=0")
        .addQueryParameter("key", trackKey)
        .addQueryParameter("uri", "library://" + libraryId + "/item/" + trackParentKey)
        .addQueryParameter(TOKEN, url.queryParameter(TOKEN))
        .build());
  }

  interface Api {
    @GET Observable<MediaContainer> get(@Url HttpUrl url);
    @POST Single<MediaContainer> post(@Url HttpUrl url);
  }
}
