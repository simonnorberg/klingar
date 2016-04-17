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
package net.simno.klingar.data.model;

import net.simno.klingar.data.media.MediaService;
import net.simno.klingar.data.media.MediaServiceHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import okhttp3.HttpUrl;

public final class Server {

  private final String id;
  private final String name;
  private final MediaServiceHelper mediaHelper;
  private final List<Connection> connections = new ArrayList<>();

  public Server(String id, String name, MediaServiceHelper mediaHelper, Connection connection) {
    this.id = id;
    this.name = name;
    this.mediaHelper = mediaHelper;
    addConnection(connection);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public MediaService media() {
    return mediaHelper;
  }

  public HttpUrl currentUri() {
    if (connections.isEmpty()) {
      return null;
    }
    return connections.get(0).uri;
  }

  public void addConnection(Connection connection) {
    if (hasConnection(connection)) {
      return;
    }
    connections.add(connection);
    Collections.sort(connections);
    mediaHelper.setUrl(connections.get(0).uri);
  }

  public int getConnectionCount() {
    return connections.size();
  }

  private boolean hasConnection(Connection connection) {
    for (Connection c : connections) {
      if (Objects.equals(c.uri, connection.uri)) {
        return true;
      }
    }
    return false;
  }
}
