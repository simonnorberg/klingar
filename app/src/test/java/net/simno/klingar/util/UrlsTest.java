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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import okhttp3.HttpUrl;

import static net.simno.klingar.util.Urls.addPathToUrl;
import static net.simno.klingar.util.Urls.addTranscodeParams;
import static net.simno.klingar.util.Urls.getTranscodeUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class UrlsTest {

  @Test
  public void testAddPathToUrl() {
    HttpUrl host = HttpUrl.parse("http://192.168.1.1:32400/");
    String uri = addPathToUrl(host, "/first/second/third/");

    assertThat(addPathToUrl(host, null), nullValue());
    assertThat(uri, is("http://192.168.1.1:32400/first/second/third"));
  }

  @Test
  public void testGetTranscodeUrl() {
    HttpUrl serverUri = HttpUrl.parse("http://192.168.1.1:32400/");
    String image = "/library/metadata/6405/thumb/1436840696";
    String transcodeUrl = getTranscodeUrl(serverUri, image);
    assertThat(transcodeUrl, is("http://192.168.1.1:32400/photo/:/transcode?url=/library/metadata/6405/thumb/1436840696"));
  }

  @Test
  public void testAddTranscodeParams() {
    String transcodeUrl = "http://192.168.1.1:32400/photo/:/transcode?url=/library/metadata/6405/thumb/1436840696";
    String addedParams = addTranscodeParams(transcodeUrl, 300, 400);
    assertThat(addedParams, is("http://192.168.1.1:32400/photo/:/transcode?url=/library/metadata/6405/thumb/1436840696&width=300&height=400"));
  }
}
