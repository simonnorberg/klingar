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
package net.simno.klingar.util;

import org.junit.Test;

import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UrlsTest {

  @Test public void addPathToUrl() {
    String expected = "https://plex.tv/library/1337/metadata";
    HttpUrl url = Urls.addPathToUrl(HttpUrl.parse("https://plex.tv/"), "/library/1337/metadata");
    String actual = url.toString();
    assertThat(actual, is(expected));
  }

  @Test public void createImageTranscodeUrl() {
    String expected = "https://plex.tv/photo/:/transcode?url=imageKey";
    String actual = Urls.getTranscodeUrl(HttpUrl.parse("https://plex.tv/"), "imageKey");
    assertThat(actual, is(expected));
  }

  @Test public void addTranscodeDimensionParamsToUrl() {
    String expected = "https://plex.tv/photo/:/transcode?url=imageKey&width=8&height=4";
    String actual = Urls.addTranscodeParams("https://plex.tv/photo/:/transcode?url=imageKey", 8, 4);
    assertThat(actual, is(expected));
  }
}
