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

import okhttp3.HttpUrl;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;
import rx.Observable;

interface MediaService {
  @GET Observable<MediaContainer> sections(@Url HttpUrl url);
  @GET Observable<MediaContainer> albums(@Url HttpUrl url);
  @GET Observable<MediaContainer> tracks(@Url HttpUrl url);
  @GET Observable<MediaContainer> popularTracks(@Url HttpUrl url);
  @GET Observable<MediaContainer> browse(@Url HttpUrl url);
  @GET Observable<MediaContainer> recentArtists(@Url HttpUrl url);
  @GET Observable<MediaContainer> firstCharacter(@Url HttpUrl url);
  @POST Observable<MediaContainer> playQueue(@Url HttpUrl url);
}
