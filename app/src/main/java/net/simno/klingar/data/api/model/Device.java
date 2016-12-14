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
package net.simno.klingar.data.api.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false)
public final class Device {
  @Attribute public String name;
  @Attribute public String device;
  @Attribute public String clientIdentifier;
  @Attribute public long createdAt;
  @Attribute public long lastSeenAt;
  @Attribute public String provides;
  @Attribute public String accessToken;
  @ElementList(inline = true) public List<Connection> connections;

  @Root(strict = false)
  public static class Connection {
    @Attribute public String protocol;
    @Attribute public String address;
    @Attribute public int port;
    @Attribute public String uri;
    @Attribute public int local;
  }
}
