/*
 * Copyright (C) 2015 Simon Norberg
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
package net.simno.klingar.data.media;

import net.simno.klingar.data.model.MediaContainer;

import okhttp3.HttpUrl;
import rx.Observable;

/**
 * Helper class that holds the service, host and access token for one plex media server.
 */
public class MediaServiceHelper implements MediaService {

  private final HostInterceptor hostInterceptor;
  private final MediaService media;

  public static MediaServiceHelper create(HostInterceptor hostInterceptor, MediaService media) {
    return new MediaServiceHelper(hostInterceptor, media);
  }

  private MediaServiceHelper(HostInterceptor hostInterceptor, MediaService media) {
    this.hostInterceptor = hostInterceptor;
    this.media = media;
  }

  public void setUrl(HttpUrl url) {
    hostInterceptor.setUrl(url);
  }

  @Override
  public Observable<MediaContainer> playlists() {
    return media.playlists();
  }

  @Override
  public Observable<MediaContainer> playlistItems(String playlistKey) {
    return media.playlistItems(playlistKey);
  }

  @Override
  public Observable<MediaContainer> sections() {
    return media.sections();
  }

  @Override
  public Observable<MediaContainer> albums(String artistKey) {
    return media.albums(artistKey);
  }

  @Override
  public Observable<MediaContainer> tracks(String albumKey) {
    return media.tracks(albumKey);
  }

  @Override
  public Observable<MediaContainer> popularTracks(String libKey, String artistKey) {
    return media.popularTracks(libKey, artistKey);
  }

  @Override
  public Observable<MediaContainer> browse(String libKey, int mediaKey, int offset) {
    return media.browse(libKey, mediaKey, offset);
  }

  @Override
  public Observable<MediaContainer> recentArtists(String libKey) {
    return media.recentArtists(libKey);
  }

  @Override
  public Observable<MediaContainer> firstCharacter(String libKey, int mediaKey) {
    return media.firstCharacter(libKey, mediaKey);
  }
}
