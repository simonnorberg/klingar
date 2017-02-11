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
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "Track", strict = false)
public final class Song {
  @Attribute public String key;
  @Attribute public String ratingKey;
  @Attribute public String parentKey;
  @Attribute public String title;
  @Attribute public String parentTitle;
  @Attribute public String grandparentTitle;
  @Attribute(required = false) public Long playQueueItemID;
  @Attribute(required = false) public String thumb;
  @Attribute(required = false) public int index;
  @Attribute(required = false) public long duration;
  @Element public Media media;

  @Root(strict = false)
  public static class Media {
    @Element public Part part;
  }

  @Root(strict = false)
  public static class Part {
    @Attribute public String key;
  }
}
