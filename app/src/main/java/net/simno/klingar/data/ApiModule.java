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
package net.simno.klingar.data;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.SECONDS;

@Module
public class ApiModule {

  @Provides @Singleton
  SimpleXmlConverterFactory provideConverterFactory() {
    return SimpleXmlConverterFactory.create(new Persister(new Format(new HyphenStyle())));
  }

  @Provides @Singleton
  HttpLoggingInterceptor provideLoggingInterceptor() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Timber.tag("OkHttp").d(message));
    logging.setLevel(HttpLoggingInterceptor.Level.BODY);
    return logging;
  }

  @Provides @Singleton @Named("shared")
  OkHttpClient provideOkHttpClient(HttpLoggingInterceptor logging) {
    return new OkHttpClient().newBuilder()
        .connectTimeout(10, SECONDS)
        .readTimeout(10, SECONDS)
        .writeTimeout(10, SECONDS)
        .addInterceptor(logging)
        .build();
  }
}
