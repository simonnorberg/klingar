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
import net.simno.klingar.data.api.model.SectionContainer;

import okhttp3.HttpUrl;
import rx.Observable;

public class MediaServiceHelper {

  private static final String TOKEN = "X-Plex-Token";

  private final MediaService media;

  MediaServiceHelper(MediaService media) {
    this.media = media;
  }

  public Observable<SectionContainer> sections(HttpUrl url) {
    return media.sections(url.newBuilder()
        .addPathSegments("library/sections")
        .build());
  }

  public Observable<MediaContainer> playlists(HttpUrl url) {
    return media.playlists(url.newBuilder()
        .addPathSegments("playlists/all")
        .query("type=15&playlistType=audio")
        .addQueryParameter(TOKEN, url.queryParameter(TOKEN))
        .build());
  }

  public Observable<MediaContainer> playlistItems(HttpUrl url, String playlistKey) {
    return media.playlistItems(url.newBuilder()
        .addPathSegment("playlists")
        .addPathSegment(playlistKey)
        .addPathSegment("items")
        .build());
  }

  public Observable<MediaContainer> albums(HttpUrl url, String artistKey) {
    return media.albums(url.newBuilder()
        .addPathSegments("library/metadata")
        .addPathSegment(artistKey)
        .addPathSegment("children")
        .build());
  }

  public Observable<MediaContainer> tracks(HttpUrl url, String albumKey) {
    return media.tracks(url.newBuilder()
        .addPathSegments("library/metadata")
        .addPathSegment(albumKey)
        .addPathSegment("children")
        .build());
  }

  public Observable<MediaContainer> popularTracks(HttpUrl url, String libKey, String artistKey) {
    return media.popularTracks(url.newBuilder()
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
    return media.browse(url.newBuilder()
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
    return media.recentArtists(url.newBuilder()
        .addPathSegments("library/sections")
        .addPathSegment(libKey)
        .addPathSegment("all")
        .query("viewCount>=1&type=8&sort=lastViewedAt:desc")
        .addQueryParameter(TOKEN, url.queryParameter(TOKEN))
        .build());
  }

  public Observable<MediaContainer> firstCharacter(HttpUrl url, String libKey, String mediaKey) {
    return media.firstCharacter(url.newBuilder()
        .addPathSegments("library/sections")
        .addPathSegment(libKey)
        .addPathSegment("firstCharacter")
        .addQueryParameter("type", mediaKey)
        .build());
  }
}
