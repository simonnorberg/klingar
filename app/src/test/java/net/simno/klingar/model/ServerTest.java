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
package net.simno.klingar.model;

import net.simno.klingar.data.media.HostInterceptor;
import net.simno.klingar.data.media.MediaService;
import net.simno.klingar.data.media.MediaServiceHelper;
import net.simno.klingar.data.model.Connection;
import net.simno.klingar.data.model.Server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import okhttp3.HttpUrl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class ServerTest {

  private static final Connection CONNECTION = Connection.create(
      HttpUrl.parse("https://12-34-56-78-identifier.plex.direct:32400"));

  private Server server;

  @Before
  public void createServer() {
    HostInterceptor hostInterceptor = mock(HostInterceptor.class);
    MediaService mediaService = mock(MediaService.class);
    MediaServiceHelper mediaServiceHelper = MediaServiceHelper.create(hostInterceptor, mediaService);
    server = new Server("id", "name", mediaServiceHelper, CONNECTION);
  }

  @Test
  public void testConnectionsPriority() {
    Connection connection1 = Connection.create(HttpUrl.parse("http://12.34.56.78:32400"));
    Connection connection2 = Connection.create(HttpUrl.parse("http://23.45.67.89:32400"));

    server.addConnection(connection1);
    assertThat(server.currentUri(), is(CONNECTION.uri));
    assertThat(server.getConnectionCount(), is(2));

    server.addConnection(connection2);
    assertThat(server.currentUri(), is(CONNECTION.uri));
    assertThat(server.getConnectionCount(), is(3));
  }

  @Test
  public void testConnectionsMerge() {
    Connection merge1a = Connection.create(HttpUrl.parse("https://192.168.1.1:32400"));
    Connection merge1b = Connection.create(HttpUrl.parse("http://192.168.1.1:32400"));
    Connection merge1c = Connection.create(HttpUrl.parse("https://192.168.1.1:32400"));

    Connection merge2a = Connection.create(HttpUrl.parse("https://192.168.1.2:32400"));
    Connection merge2b = Connection.create(HttpUrl.parse("https://192.168.1.2:32400"));

    server.addConnection(merge1a);
    assertThat(server.getConnectionCount(), is(2));

    server.addConnection(merge1b);
    assertThat(server.getConnectionCount(), is(3));

    server.addConnection(merge1c);
    assertThat(server.getConnectionCount(), is(3));

    server.addConnection(merge2a);
    assertThat(server.getConnectionCount(), is(4));

    server.addConnection(merge2b);
    assertThat(server.getConnectionCount(), is(4));
  }
}
