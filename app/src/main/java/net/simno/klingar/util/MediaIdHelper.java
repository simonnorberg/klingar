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

import android.support.annotation.NonNull;
import android.util.SparseArray;

import net.simno.klingar.data.Type;

/**
 * Create media ids with "browsing history" for the media browser.
 */
@SuppressWarnings("StringBufferReplaceableByString")
public final class MediaIdHelper {

  public static final String MEDIA_ID_ROOT = "__ROOT__";

  private MediaIdHelper() {
    // no instances
  }

  // Media ID syntax = SERVER#KEY:VAL#...#KEY:VAL|...|KEY:VAL

  // Delimiters
  private static final char PATH_DEL = '#';
  private static final char PARAM_DEL = '|';
  private static final char KV_DEL = ':';

  // Parameter keys for a child object
  private static final int TYPE = 0;
  private static final int PLAYLIST_KEY = 1;
  private static final int LIB_KEY = 2;
  private static final int ARTIST_KEY = 3;
  private static final int ALBUM_KEY = 4;
  private static final int TRACK_KEY = 5;
  private static final int MEDIA_TYPE = 6;
  private static final int MEDIA_KEY = 7;
  private static final int OFFSET = 8;

  // Parameter keys for a child in a media id string
  private static final String P_TYPE = "P_TYPE";
  private static final String P_PLIST_KEY = "P_PLIST_KEY";
  private static final String P_LIB_KEY = "P_LIB_KEY";
  private static final String P_ARTIST_KEY = "P_ARTIST_KEY";
  private static final String P_ALBUM_KEY = "P_ALBUM_KEY";
  private static final String P_TRACK_KEY = "P_TRACK_KEY";
  private static final String P_MEDIA_TYPE = "P_MEDIA_TYPE";
  private static final String P_MEDIA_KEY = "P_MEDIA_KEY";
  private static final String P_OFFSET = "P_OFFSET";

  public static String createPlaylistMediaId(String serverId, String playlistKey) {
    return new StringBuilder(serverId).append(PATH_DEL)
        .append(P_TYPE).append(KV_DEL).append(Type.PLAYLIST).append(PARAM_DEL)
        .append(P_PLIST_KEY).append(KV_DEL).append(playlistKey)
        .toString();
  }

  public static String createLibraryMediaId(String serverId, String libKey) {
    return new StringBuilder(serverId).append(PATH_DEL)
        .append(P_TYPE).append(KV_DEL).append(Type.LIBRARY).append(PARAM_DEL)
        .append(P_LIB_KEY).append(KV_DEL).append(libKey)
        .toString();
  }

  public static String createArtistMediaId(String parentId, String libKey, String artistKey) {
    return new StringBuilder(parentId).append(PATH_DEL)
        .append(P_TYPE).append(KV_DEL).append(Type.ARTIST).append(PARAM_DEL)
        .append(P_LIB_KEY).append(KV_DEL).append(libKey).append(PARAM_DEL)
        .append(P_ARTIST_KEY).append(KV_DEL).append(artistKey)
        .toString();
  }

  public static String createAlbumMediaId(String parentId, String albumKey) {
    return new StringBuilder(parentId).append(PATH_DEL)
        .append(P_TYPE).append(KV_DEL).append(Type.ALBUM).append(PARAM_DEL)
        .append(P_ALBUM_KEY).append(KV_DEL).append(albumKey)
        .toString();
  }

  public static String createTrackMediaId(String parentId, String trackKey) {
    return new StringBuilder(parentId).append(PATH_DEL)
        .append(P_TYPE).append(KV_DEL).append(Type.TRACK).append(PARAM_DEL)
        .append(P_TRACK_KEY).append(KV_DEL).append(trackKey)
        .toString();
  }

  public static String createBrowseMediaId(String parentId, String libKey, String mediaType,
                                           String mediaKey, int offset) {
    return new StringBuilder(parentId).append(PATH_DEL)
        .append(P_TYPE).append(KV_DEL).append(Type.MEDIA_TYPE).append(PARAM_DEL)
        .append(P_LIB_KEY).append(KV_DEL).append(libKey).append(PARAM_DEL)
        .append(P_MEDIA_TYPE).append(KV_DEL).append(mediaType).append(PARAM_DEL)
        .append(P_MEDIA_KEY).append(KV_DEL).append(mediaKey).append(PARAM_DEL)
        .append(P_OFFSET).append(KV_DEL).append(offset)
        .toString();
  }

  public static String addOffsetToBrowseMediaId(@NonNull String mediaId, int addOffset) {
    SparseArray<Object> lastChild = getLastChild(mediaId);
    Integer offset = lastChild != null ? getOffset(lastChild) : null;
    if (offset != null) {
      offset += addOffset;
    } else {
      offset = addOffset;
    }
    return mediaId.substring(0, mediaId.lastIndexOf(P_OFFSET + KV_DEL)) + P_OFFSET + KV_DEL + offset;
  }

  public static String getServerId(String mediaId) {
    String[] s = mediaId.split(String.valueOf(PATH_DEL));
    if (s.length > 0) {
      return s[0];
    }
    return null;
  }

  public static SparseArray<Object> getLastParent(@NonNull String mediaId) {
    String[] s = mediaId.split(String.valueOf(PATH_DEL));
    if (s.length > 2) {
      return createChild(s[s.length - 2]);
    }
    return null;
  }

  public static SparseArray<Object> getLastChild(@NonNull String mediaId) {
    String[] s = mediaId.split(String.valueOf(PATH_DEL));
    if (s.length > 1) {
      return createChild(s[s.length - 1]);
    }
    return null;
  }

  private static SparseArray<Object> createChild(@NonNull String s) {
    String[] params = s.split("\\" + PARAM_DEL);
    if (params.length == 0) {
      return null;
    }

    SparseArray<Object> child = new SparseArray<>(params.length);
    for (String param : params) {
      String[] p = param.split(String.valueOf(KV_DEL));
      if (p.length < 2) {
        continue;
      }
      switch (p[0]) {
        case P_TYPE:
          child.put(TYPE, Integer.valueOf(p[1]));
          break;
        case P_PLIST_KEY:
          child.put(PLAYLIST_KEY, p[1]);
          break;
        case P_LIB_KEY:
          child.put(LIB_KEY, p[1]);
          break;
        case P_ARTIST_KEY:
          child.put(ARTIST_KEY, p[1]);
          break;
        case P_ALBUM_KEY:
          child.put(ALBUM_KEY, p[1]);
          break;
        case P_TRACK_KEY:
          child.put(TRACK_KEY, p[1]);
          break;
        case P_MEDIA_TYPE:
          child.put(MEDIA_TYPE, Integer.valueOf(p[1]));
          break;
        case P_MEDIA_KEY:
          child.put(MEDIA_KEY, Integer.valueOf(p[1]));
          break;
        case P_OFFSET:
          child.put(OFFSET, Integer.valueOf(p[1]));
          break;
      }
    }

    return child;
  }

  public static Integer getType(SparseArray<Object> child) {
    return (Integer) child.get(TYPE, null);
  }

  public static String getPlaylistKey(SparseArray<Object> child) {
    return (String) child.get(PLAYLIST_KEY, null);
  }

  public static String getLibraryKey(SparseArray<Object> child) {
    return (String) child.get(LIB_KEY, null);
  }

  public static String getArtistKey(SparseArray<Object> child) {
    return (String) child.get(ARTIST_KEY, null);
  }

  public static String getAlbumKey(SparseArray<Object> child) {
    return (String) child.get(ALBUM_KEY, null);
  }

  public static String getTrackKey(SparseArray<Object> child) {
    return (String) child.get(TRACK_KEY, null);
  }

  public static Integer getMediaType(SparseArray<Object> child) {
    return (Integer) child.get(MEDIA_TYPE, null);
  }

  public static Integer getMediaKey(SparseArray<Object> child) {
    return (Integer) child.get(MEDIA_KEY, null);
  }

  public static Integer getOffset(SparseArray<Object> child) {
    return (Integer) child.get(OFFSET, null);
  }
}
