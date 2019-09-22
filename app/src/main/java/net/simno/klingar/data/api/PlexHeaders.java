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

import android.os.Build;

import androidx.annotation.NonNull;

import net.simno.klingar.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

class PlexHeaders implements Interceptor {

  private final String clientId;
  private final String appName;

  PlexHeaders(String clientId, String appName) {
    this.clientId = clientId;
    this.appName = appName;
  }

  @Override @NonNull public Response intercept(@NonNull Chain chain) throws IOException {
    return chain.proceed(chain.request().newBuilder()
        .header("X-Plex-Platform", "Android")
        .header("X-Plex-Provides", "player")
        .header("X-Plex_Client-Name", appName)
        .header("X-Plex-Client-Identifier", clientId)
        .header("X-Plex-Version", BuildConfig.VERSION_NAME)
        .header("X-Plex-Product", appName)
        .header("X-Plex-Platform-Version", Build.VERSION.RELEASE)
        .header("X-Plex-Device", Build.MODEL)
        .header("X-Plex-Device-Name", Build.MODEL)
        .build());
  }
}
