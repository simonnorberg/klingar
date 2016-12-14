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
package net.simno.klingar.data;

import net.simno.klingar.R;

public final class Type {
  public static final int ALBUM = R.id.album;
  public static final int ARTIST = R.id.artist;
  public static final int HEADER = R.id.header;
  public static final int LIBRARY = R.id.library;
  public static final int MEDIA_TYPE = R.id.media_type;
  public static final int PLAYLIST = R.id.playlist;
  public static final int TRACK = R.id.track;

  private Type() {
    // no instances
  }
}
