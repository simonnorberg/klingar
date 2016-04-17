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

import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface MediaService {

  @GET("/playlists/all?type=15&playlistType=audio")
  Observable<MediaContainer> playlists();

  @GET("/playlists/{key}/items")
  Observable<MediaContainer> playlistItems(@Path("key") String playlistKey);

  @GET("/library/sections")
  Observable<MediaContainer> sections();

  @GET("/library/metadata/{key}/children")
  Observable<MediaContainer> albums(@Path("key") String artistKey);

  @GET("/library/metadata/{key}/children")
  Observable<MediaContainer> tracks(@Path("key") String albumKey);

  @GET("/library/sections/{key}/all?group=title&limit=5&ratingCount>=1&sort=ratingCount:desc&type=10")
  Observable<MediaContainer> popularTracks(@Path("key") String libKey,
                                           @Query("artist.id") String artistKey);

  @GET("/library/sections/{key}/all?sort=titleSort:asc&X-Plex-Container-Size=50")
  Observable<MediaContainer> browse(@Path("key") String libKey,
                                    @Query("type") int mediaKey,
                                    @Query("X-Plex-Container-Start") int offset);

  @GET("/library/sections/{key}/all?viewCount>=1&type=8&sort=lastViewedAt:desc")
  Observable<MediaContainer> recentArtists(@Path("key") String libKey);

  @GET("/library/sections/{key}/firstCharacter")
  Observable<MediaContainer> firstCharacter(@Path("key") String libKey,
                                            @Query("type") int mediaKey);
}
