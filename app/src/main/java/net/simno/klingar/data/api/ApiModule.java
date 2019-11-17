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

import android.content.res.Resources;

import net.simno.klingar.R;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressWarnings("deprecation")
@Module
public class ApiModule {

  private static final String PLEX_URL = "https://plex.tv";

  @Provides @Singleton AuthInterceptor provideAuthInterceptor() {
    return new AuthInterceptor();
  }

  @Provides @Singleton PlexHeaders providePlexHeaders(@Named("clientId") String clientId,
                                                      Resources resources) {
    return new PlexHeaders(clientId, resources.getString(R.string.app_name));
  }

  @Provides @Singleton SimpleXmlConverterFactory provideSimpleXmlConverterFactory() {
    return SimpleXmlConverterFactory.create(new Persister(new Format(new HyphenStyle())));
  }

  @Provides @Singleton RxJava2CallAdapterFactory provideRxJava2CallAdapterFactory() {
    return RxJava2CallAdapterFactory.create();
  }

  @Provides @Singleton HttpLoggingInterceptor provideLoggingInterceptor() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message ->
        Timber.tag("OkHttp").d(message));
    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
    return logging;
  }

  @Provides @Singleton @Named("default")
  OkHttpClient provideOkHttpClient(HttpLoggingInterceptor logging, PlexHeaders plexHeaders) {
    return new OkHttpClient().newBuilder()
        .connectTimeout(15, SECONDS)
        .readTimeout(15, SECONDS)
        .writeTimeout(15, SECONDS)
        .addInterceptor(logging)
        .addInterceptor(plexHeaders)
        .build();
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
                               SimpleXmlConverterFactory simpleXml,
                               RxJava2CallAdapterFactory rxJava) {
    return new Retrofit.Builder()
        .baseUrl(PLEX_URL)
        .callFactory(client)
        .addConverterFactory(simpleXml)
        .addCallAdapterFactory(rxJava)
        .build();
  }

  @Provides @Singleton PlexService providePlexService(@Named("plex") Retrofit retrofit) {
    return retrofit.create(PlexService.class);
  }

  @Provides @Singleton @Named("media")
  Retrofit provideMediaRetrofit(@Named("default") OkHttpClient client,
                                SimpleXmlConverterFactory simpleXml,
                                RxJava2CallAdapterFactory rxJava) {
    return new Retrofit.Builder()
        .baseUrl(PLEX_URL) // never used
        .callFactory(client)
        .addConverterFactory(simpleXml)
        .addCallAdapterFactory(rxJava)
        .build();
  }

  @Provides @Singleton MediaService provideMediaService(@Named("media") Retrofit retrofit) {
    return new MediaService(retrofit.create(MediaService.Api.class));
  }
}
