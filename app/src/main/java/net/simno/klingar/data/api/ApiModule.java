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

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

@Module
public class ApiModule {

  private static final HttpUrl PLEX_URL = HttpUrl.parse("https://plex.tv");

  @Provides @Singleton AuthInterceptor provideAuthInterceptor() {
    return new AuthInterceptor();
  }

  @Provides @Singleton SimpleXmlConverterFactory provideConverterFactory() {
    return SimpleXmlConverterFactory.create(new Persister(new Format(new HyphenStyle())));
  }

  @Provides @Singleton @Named("plex")
  OkHttpClient providePlexClient(@Named("default") OkHttpClient client,
                                 AuthInterceptor authInterceptor) {
    return client.newBuilder()
        .addInterceptor(authInterceptor)
        .build();
  }

  @Provides @Singleton @Named("plex")
  Retrofit providePlexRetrofit(@Named("plex") OkHttpClient client,
                               SimpleXmlConverterFactory converterFactory) {
    return new Retrofit.Builder()
        .baseUrl(PLEX_URL)
        .callFactory(client)
        .addConverterFactory(converterFactory)
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
  }

  @Provides @Singleton PlexService providePlexService(@Named("plex") Retrofit retrofit) {
    return retrofit.create(PlexService.class);
  }

  @Provides @Singleton @Named("media")
  Retrofit provideMediaRetrofit(@Named("default") OkHttpClient client,
                                SimpleXmlConverterFactory converterFactory) {
    return new Retrofit.Builder()
        .baseUrl(PLEX_URL)
        .callFactory(client)
        .addConverterFactory(converterFactory)
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
  }

  @Provides @Singleton
  MediaServiceHelper provideMediaServiceHelper(@Named("media") Retrofit retrofit) {
    return new MediaServiceHelper(retrofit.create(MediaService.class));
  }
}
