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
package net.simno.klingar.util;

import okhttp3.HttpUrl;

public final class Urls {

  private Urls() {
    // no instances
  }

  public static String addPathToUrl(HttpUrl url, String path) {
    if (url == null || Strings.isBlank(path)) {
      return null;
    }

    HttpUrl.Builder builder = url.newBuilder();
    String[] segments = path.split("/");
    for (String segment : segments) {
      builder.addPathSegment(segment);
    }

    return builder.build().toString();
  }

  public static String getTranscodeUrl(HttpUrl url, String imageKey) {
    if (url == null || Strings.isBlank(imageKey)) {
      return null;
    }

    return url.newBuilder().addPathSegment("photo")
        .addPathSegment(":")
        .addPathSegment("transcode")
        .addQueryParameter("url", imageKey)
        .build()
        .toString();
  }

  public static String addTranscodeParams(String transcodeUrl, int width, int height) {
    if (Strings.isBlank(transcodeUrl)) {
      return null;
    }

    return HttpUrl.parse(transcodeUrl).newBuilder()
        .addQueryParameter("width", String.valueOf(width))
        .addQueryParameter("height", String.valueOf(height))
        .build()
        .toString();
  }
}
