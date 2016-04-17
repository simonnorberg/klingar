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
package net.simno.klingar.data.plex;

import net.simno.klingar.data.ApiModule;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;


@Module(
    includes = ApiModule.class
)
public class PlexModule {

  private static final String PLEX_URL = "https://plex.tv";

  @Provides @Singleton
  AuthTokenInterceptor provideAuthTokenInterceptor() {
    return new AuthTokenInterceptor();
  }

  @Provides @Singleton @Named("plex")
  OkHttpClient provideOkHttpClient(@Named("shared") OkHttpClient client,
                                   AuthTokenInterceptor authTokenInterceptor) {
    return client.newBuilder()
        .addInterceptor(authTokenInterceptor)
        .build();
  }

  @Provides @Singleton
  Retrofit provideRetrofit(SimpleXmlConverterFactory converterFactory,
                           @Named("plex") OkHttpClient client) {
    return new Retrofit.Builder()
        .baseUrl(PLEX_URL)
        .callFactory(client)
        .addConverterFactory(converterFactory)
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
  }

  @Provides @Singleton
  PlexService providePlexService(Retrofit retrofit) {
    return retrofit.create(PlexService.class);
  }
}
