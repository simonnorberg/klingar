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

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

  private volatile String authToken;

  AuthInterceptor() {
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  @NonNull @Override public Response intercept(@NonNull Chain chain) throws IOException {
    Request request = chain.request();

    String authToken = this.authToken;
    if (authToken != null) {
      HttpUrl newUrl = request.url().newBuilder()
          .setQueryParameter("auth_token", authToken)
          .build();
      request = request.newBuilder()
          .url(newUrl)
          .build();
    }

    return chain.proceed(request);
  }
}
