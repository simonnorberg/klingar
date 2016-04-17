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

import android.support.test.runner.AndroidJUnit4;
import android.util.SparseArray;

import net.simno.klingar.data.Type;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class MediaIdHelperTest {

  @Test
  public void testCreatePlaylistMediaId() {
    String mediaId = MediaIdHelper.createPlaylistMediaId("serverId", "playlistKey");
    assertThat(mediaId, is("serverId#P_TYPE:" + Type.PLAYLIST + "|P_PLIST_KEY:playlistKey"));
  }

  @Test
  public void testCreateLibraryMediaId() {
    String mediaId = MediaIdHelper.createLibraryMediaId("serverId", "libKey");
    assertThat(mediaId, is("serverId#P_TYPE:" + Type.LIBRARY + "|P_LIB_KEY:libKey"));
  }

  @Test
  public void testCreateArtistMediaId() {
    String mediaId = MediaIdHelper.createArtistMediaId("parentId", "libKey", "artistKey");
    assertThat(mediaId, is("parentId#P_TYPE:" + Type.ARTIST + "|P_LIB_KEY:libKey|P_ARTIST_KEY:artistKey"));
  }

  @Test
  public void testCreateAlbumMediaId() {
    String mediaId = MediaIdHelper.createAlbumMediaId("parentId", "albumKey");
    assertThat(mediaId, is("parentId#P_TYPE:" + Type.ALBUM + "|P_ALBUM_KEY:albumKey"));
  }

  @Test
  public void testCreateTrackMediaId() {
    String mediaId = MediaIdHelper.createTrackMediaId("parentId", "trackKey");
    assertThat(mediaId, is("parentId#P_TYPE:" + Type.TRACK + "|P_TRACK_KEY:trackKey"));
  }

  @Test
  public void testCreateBrowseMediaId() {
    String mediaId = MediaIdHelper.createBrowseMediaId("parentId", "libKey", String.valueOf(Type.ARTIST), "8", 50);
    assertThat(mediaId, is("parentId#P_TYPE:" + Type.MEDIA_TYPE + "|P_LIB_KEY:libKey|P_MEDIA_TYPE:"
        + Type.ARTIST + "|P_MEDIA_KEY:8|P_OFFSET:50"));
  }

  @Test
  public void testAddOffsetToBrowseMediaId() {
    String mediaId = MediaIdHelper.createBrowseMediaId("parentId", "libKey", String.valueOf(Type.ARTIST), "8", 2);
    String increasedOffset = MediaIdHelper.addOffsetToBrowseMediaId(mediaId, 33);
    assertThat(increasedOffset, is("parentId#P_TYPE:" + Type.MEDIA_TYPE
        + "|P_LIB_KEY:libKey|P_MEDIA_TYPE:" + Type.ARTIST + "|P_MEDIA_KEY:8|P_OFFSET:35"));
  }

  @Test
  public void testGetServerId() {
    String mediaId = "serverId#P_TYPE:" + Type.PLAYLIST + "|P_PLIST_KEY:playlistKey";
    String serverId = MediaIdHelper.getServerId(mediaId);
    assertThat(serverId, is("serverId"));
  }

  @Test
  public void testGetLastParent() {
    String mediaId = "serverId#P_TYPE:" + Type.LIBRARY + "|P_LIB_KEY:libKey#P_TYPE:T_TRACK|P_TRACK_KEY:trackKey";
    SparseArray<Object> parent = MediaIdHelper.getLastParent(mediaId);
    assertThat(parent, notNullValue());

    assertThat(MediaIdHelper.getType(parent), is(Type.LIBRARY));
    assertThat(MediaIdHelper.getLibraryKey(parent), is("libKey"));
  }

  @Test
  public void testGetLastChild() {
    String mediaId = "serverId#P_TYPE:T_LIB|P_LIB_KEY:libKey#"
        + "P_TYPE:" + Type.MEDIA_TYPE + "|"
        + "P_PLIST_KEY:playlistKey|"
        + "P_LIB_KEY:libKey|"
        + "P_ARTIST_KEY:artistKey|"
        + "P_ALBUM_KEY:albumKey|"
        + "P_TRACK_KEY:trackKey|"
        + "P_MEDIA_TYPE:" + Type.ARTIST + "|"
        + "P_MEDIA_KEY:8|"
        + "P_OFFSET:50";

    SparseArray<Object> child = MediaIdHelper.getLastChild(mediaId);
    assertThat(child, notNullValue());

    assertThat(MediaIdHelper.getType(child), is(Type.MEDIA_TYPE));
    assertThat(MediaIdHelper.getPlaylistKey(child), is("playlistKey"));
    assertThat(MediaIdHelper.getLibraryKey(child), is("libKey"));
    assertThat(MediaIdHelper.getArtistKey(child), is("artistKey"));
    assertThat(MediaIdHelper.getAlbumKey(child), is("albumKey"));
    assertThat(MediaIdHelper.getTrackKey(child), is("trackKey"));
    assertThat(MediaIdHelper.getMediaType(child), is(Type.ARTIST));
    assertThat(MediaIdHelper.getMediaKey(child), is(8));
    assertThat(MediaIdHelper.getOffset(child), is(50));
  }
}
