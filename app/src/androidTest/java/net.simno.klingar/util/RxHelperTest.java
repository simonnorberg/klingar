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

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import net.simno.klingar.data.Extra;
import net.simno.klingar.data.Type;
import net.simno.klingar.data.model.Directory;
import net.simno.klingar.data.model.Playlist;
import net.simno.klingar.data.model.Track;

import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.HttpUrl;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
@RunWith(AndroidJUnit4.class)
public class RxHelperTest {

  @Test
  public void testMapPlaylist() {
    Playlist playlist = new Playlist();
    playlist.title = "playlistTitle";
    playlist.ratingKey = "playlistKey";
    playlist.composite = "/playlist/composite";
    playlist.leafCount = 10;
    playlist.duration = 20L;

    String serverId = "serverId";
    String serverTitle = "serverTitle";

    HttpUrl uri = HttpUrl.parse("http://192.168.1.2:32400?X-Plex-Token=token");

    TestSubscriber<MediaItem> testSubscriber = new TestSubscriber<>();
    Observable.just(playlist)
        .map(RxHelper.mapPlaylist(serverId, serverTitle, uri))
        .subscribe(testSubscriber);

    testSubscriber.assertNoErrors();

    MediaDescriptionCompat description = testSubscriber.getOnNextEvents().get(0).getDescription();
    assertThat(description, is(notNullValue()));
    assertThat(description.getTitle(), is("playlistTitle"));

    Bundle extras = description.getExtras();
    assertThat(extras, is(notNullValue()));
    assertThat(extras.getInt(Extra.INT_TYPE), is(Type.PLAYLIST));
    assertThat(extras.getString(Extra.STRING_KEY), is("playlistKey"));
    assertThat(extras.getString(Extra.STRING_ART_URI),
        is("http://192.168.1.2:32400/photo/:/transcode?X-Plex-Token=token&url=/playlist/composite"));
    assertThat(extras.getString(Extra.STRING_SERVER_ID), is("serverId"));
    assertThat(extras.getString(Extra.STRING_SERVER_TITLE), is("serverTitle"));
    assertThat(extras.getInt(Extra.INT_SIZE), is(10));
    assertThat(extras.getLong(Extra.LONG_DURATION), is(20L));
  }

  @Test
  public void testMapLibrary() {
    Directory lib = new Directory();
    lib.title = "libTitle";
    lib.key = "libKey";

    String serverId = "serverId";
    String serverTitle = "serverTitle";

    TestSubscriber<MediaItem> testSubscriber = new TestSubscriber<>();
    Observable.just(lib)
        .map(RxHelper.mapLibrary(serverId, serverTitle))
        .subscribe(testSubscriber);

    testSubscriber.assertNoErrors();

    MediaDescriptionCompat description = testSubscriber.getOnNextEvents().get(0).getDescription();
    assertThat(description, is(notNullValue()));
    assertThat(description.getTitle(), is("libTitle"));

    Bundle extras = description.getExtras();
    assertThat(extras, is(notNullValue()));
    assertThat(extras.getInt(Extra.INT_TYPE), is(Type.LIBRARY));
    assertThat(extras.getString(Extra.STRING_KEY), is("libKey"));
    assertThat(extras.getString(Extra.STRING_SERVER_ID), is("serverId"));
    assertThat(extras.getString(Extra.STRING_SERVER_TITLE), is("serverTitle"));
  }

  @Test
  public void testMapArtist() {
    Directory dir = new Directory();
    dir.art = "/artist/art";
    dir.thumb = "/artist/thumb";
    dir.title = "artistTitle";
    dir.ratingKey = "artistKey";

    HttpUrl uri = HttpUrl.parse("http://192.168.1.2:32400?X-Plex-Token=token");

    TestSubscriber<MediaItem> testSubscriber = new TestSubscriber<>();
    Observable.just(dir)
        .map(RxHelper.mapArtist("libKey", "serverId", uri, "parentId"))
        .subscribe(testSubscriber);

    testSubscriber.assertNoErrors();

    MediaDescriptionCompat description = testSubscriber.getOnNextEvents().get(0).getDescription();
    assertThat(description, is(notNullValue()));
    assertThat(description.getTitle(), is("artistTitle"));

    Bundle extras = description.getExtras();
    assertThat(extras, is(notNullValue()));
    assertThat(extras.getInt(Extra.INT_TYPE), is(Type.ARTIST));
    assertThat(extras.getString(Extra.STRING_KEY), is("artistKey"));
    assertThat(extras.getString(Extra.STRING_THUMB_URI),
        is("http://192.168.1.2:32400/photo/:/transcode?X-Plex-Token=token&url=/artist/thumb"));
    assertThat(extras.getString(Extra.STRING_ART_URI),
        is("http://192.168.1.2:32400/photo/:/transcode?X-Plex-Token=token&url=/artist/art"));
    assertThat(extras.getString(Extra.STRING_LIBRARY_KEY), is("libKey"));
    assertThat(extras.getString(Extra.STRING_SERVER_ID), is("serverId"));
  }

  @Test
  public void testMapAlbum() {
    Directory dir = new Directory();
    dir.art = "/album/art";
    dir.thumb = "/album/thumb";
    dir.title = "albumTitle";
    dir.parentTitle = "artistTitle";
    dir.ratingKey = "albumKey";
    dir.year = "2016";

    String serverId = "serverId";

    HttpUrl uri = HttpUrl.parse("http://192.168.1.2:32400?X-Plex-Token=token");

    TestSubscriber<MediaItem> testSubscriber = new TestSubscriber<>();
    Observable.just(dir)
        .map(RxHelper.mapAlbum(serverId, uri, "parentId"))
        .subscribe(testSubscriber);

    testSubscriber.assertNoErrors();

    MediaDescriptionCompat description = testSubscriber.getOnNextEvents().get(0).getDescription();
    assertThat(description, is(notNullValue()));
    assertThat(description.getTitle(), is("albumTitle"));

    Bundle extras = description.getExtras();
    assertThat(extras, is(notNullValue()));
    assertThat(extras.getInt(Extra.INT_TYPE), is(Type.ALBUM));
    assertThat(extras.getString(Extra.STRING_KEY), is("albumKey"));
    assertThat(extras.getString(Extra.STRING_ARTIST_TITLE), is("artistTitle"));
    assertThat(extras.getString(Extra.STRING_THUMB_URI),
        is("http://192.168.1.2:32400/photo/:/transcode?X-Plex-Token=token&url=/album/thumb"));
    assertThat(extras.getString(Extra.STRING_ART_URI),
        is("http://192.168.1.2:32400/photo/:/transcode?X-Plex-Token=token&url=/album/art"));
    assertThat(extras.getString(Extra.STRING_YEAR), is("2016"));
    assertThat(extras.getString(Extra.STRING_SERVER_ID), is("serverId"));
  }

  @Test
  public void testMapTrack() {
    Track apiTrack = new Track();
    apiTrack.ratingKey = "trackKey";
    apiTrack.title = "trackTitle";
    apiTrack.parentTitle = "albumTitle";
    apiTrack.grandparentTitle = "artistTitle";
    apiTrack.thumb = "thumb";
    apiTrack.index = 1;
    apiTrack.duration = 1000L;
    apiTrack.media = new Track.Media();
    apiTrack.media.part = new Track.Part();
    apiTrack.media.part.key = "/library/parts/1337/file.mp3";
    apiTrack.media.part.container = "mp3";

    HttpUrl uri = HttpUrl.parse("http://192.168.1.2:32400?X-Plex-Token=token");

    TestSubscriber<MediaItem> testSubscriber = new TestSubscriber<>();
    Observable.just(apiTrack)
        .map(RxHelper.mapTrack(uri, "parentId"))
        .subscribe(testSubscriber);

    testSubscriber.assertNoErrors();

    MediaDescriptionCompat description = testSubscriber.getOnNextEvents().get(0).getDescription();
    assertThat(description, is(notNullValue()));
    assertThat(description.getTitle(), is("trackTitle"));

    Bundle extras = description.getExtras();
    assertThat(extras, is(notNullValue()));
    assertThat(extras.getInt(Extra.INT_TYPE), is(Type.TRACK));
    assertThat(extras.getString(Extra.STRING_KEY), is("trackKey"));
    assertThat(extras.getString(Extra.STRING_ALBUM_TITLE), is("albumTitle"));
    assertThat(extras.getString(Extra.STRING_ARTIST_TITLE), is("artistTitle"));
    assertThat(extras.getString(Extra.STRING_THUMB_URI),
        is("http://192.168.1.2:32400/photo/:/transcode?X-Plex-Token=token&url=thumb"));
    assertThat(extras.getString(Extra.STRING_ART_URI),
        is("http://192.168.1.2:32400/thumb?X-Plex-Token=token"));
    assertThat(extras.getString(Extra.STRING_CONTAINER), is("mp3"));
    assertThat(extras.getString(Extra.STRING_URI),
        is("http://192.168.1.2:32400/library/parts/1337/file.mp3?X-Plex-Token=token"));
    assertThat(extras.getInt(Extra.INT_INDEX), is(1));
    assertThat(extras.getLong(Extra.LONG_DURATION), is(1000L));
  }
}
