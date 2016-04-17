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
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import net.simno.klingar.data.Extra;
import net.simno.klingar.data.Type;
import net.simno.klingar.data.model.Directory;
import net.simno.klingar.data.model.MediaContainer;
import net.simno.klingar.data.model.Playlist;
import net.simno.klingar.data.model.Track;

import java.util.Collections;

import okhttp3.HttpUrl;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
import static android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
import static net.simno.klingar.util.MediaIdHelper.createAlbumMediaId;
import static net.simno.klingar.util.MediaIdHelper.createArtistMediaId;
import static net.simno.klingar.util.MediaIdHelper.createLibraryMediaId;
import static net.simno.klingar.util.MediaIdHelper.createPlaylistMediaId;
import static net.simno.klingar.util.MediaIdHelper.createTrackMediaId;
import static net.simno.klingar.util.MediaItemHelper.createMediaItem;

public final class RxHelper {

  private RxHelper() {
    // no instances
  }

  public static void unsubscribe(CompositeSubscription subscriptions) {
    if (subscriptions != null && !subscriptions.isUnsubscribed()) {
      try {
        subscriptions.unsubscribe();
      } catch (CompositeException e) {
        Timber.e(e, "Unsubscribe error");
      }
    }
  }

  public static CompositeSubscription getSubscriptions(CompositeSubscription subscriptions) {
    if (subscriptions == null || subscriptions.isUnsubscribed()) {
      return new CompositeSubscription();
    }
    return subscriptions;
  }

  public static final Func1<MediaContainer, Observable<Directory>> FLATMAP_DIRS =
      mediaContainer -> {
        if (mediaContainer.directories == null) {
          return Observable.from(Collections.<Directory>emptyList());
        }
        return Observable.from(mediaContainer.directories);
      };

  public static final Func1<MediaContainer, Observable<Track>> FLATMAP_TRACKS =
      mediaContainer -> {
        if (mediaContainer.tracks == null) {
          return Observable.from(Collections.<Track>emptyList());
        }
        return Observable.from(mediaContainer.tracks);
      };

  public static final Func1<MediaContainer, Observable<Playlist>> FLATMAP_PLAYLISTS =
      mediaContainer -> {
        if (mediaContainer.playlists == null) {
          return Observable.from(Collections.<Playlist>emptyList());
        }
        return Observable.from(mediaContainer.playlists);
      };

  @SuppressWarnings("RedundantCast")
  private static final Observable.Transformer SCHEDULERS_TRANSFORMER =
      observable -> ((Observable) observable)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());

  @SuppressWarnings("unchecked")
  public static <T> Observable.Transformer<T, T> applySchedulers() {
    return (Observable.Transformer<T, T>) RxHelper.SCHEDULERS_TRANSFORMER;
  }

  public static Func1<Playlist, MediaItem> mapPlaylist(final String serverId,
                                                       final String serverTitle,
                                                       final HttpUrl uri) {
    return playlist -> {
      String art = Urls.getTranscodeUrl(uri, playlist.composite);

      Bundle extras = new Bundle();
      extras.putInt(Extra.INT_TYPE, Type.PLAYLIST);
      extras.putString(Extra.STRING_KEY, playlist.ratingKey);
      extras.putString(Extra.STRING_ART_URI, art);
      extras.putString(Extra.STRING_SERVER_ID, serverId);
      extras.putString(Extra.STRING_SERVER_TITLE, serverTitle);
      extras.putInt(Extra.INT_SIZE, playlist.leafCount);
      extras.putLong(Extra.LONG_DURATION, playlist.duration);

      String mediaId = createPlaylistMediaId(serverId, playlist.ratingKey);
      return createMediaItem(mediaId, playlist.title, extras, FLAG_BROWSABLE);
    };
  }

  public static Func1<Directory, MediaItem> mapLibrary(final String serverId,
                                                       final String serverTitle) {
    return dir -> {
      Bundle extras = new Bundle();
      extras.putInt(Extra.INT_TYPE, Type.LIBRARY);
      extras.putString(Extra.STRING_KEY, dir.key);
      extras.putString(Extra.STRING_SERVER_ID, serverId);
      extras.putString(Extra.STRING_SERVER_TITLE, serverTitle);

      String mediaId = createLibraryMediaId(serverId, dir.key);
      return createMediaItem(mediaId, dir.title, extras, FLAG_BROWSABLE);
    };
  }

  public static Func1<Directory, MediaItem> mapArtist(final String libKey, final String serverId,
                                                      final HttpUrl uri, final String parentId) {
    return dir -> {
      String thumb = Urls.getTranscodeUrl(uri, dir.thumb);
      String art = Urls.getTranscodeUrl(uri, dir.art);

      Bundle extras = new Bundle();
      extras.putInt(Extra.INT_TYPE, Type.ARTIST);
      extras.putString(Extra.STRING_KEY, dir.ratingKey);
      extras.putString(Extra.STRING_THUMB_URI, thumb);
      extras.putString(Extra.STRING_ART_URI, art);
      extras.putString(Extra.STRING_LIBRARY_KEY, libKey);
      extras.putString(Extra.STRING_SERVER_ID, serverId);

      String mediaId = createArtistMediaId(parentId, libKey, dir.ratingKey);
      return createMediaItem(mediaId, dir.title, extras, FLAG_BROWSABLE);
    };
  }

  public static Func1<Directory, MediaItem> mapAlbum(final String serverId, final HttpUrl uri,
                                                     final String parentId) {
    return dir -> {
      String thumb = Urls.getTranscodeUrl(uri, dir.thumb);
      String art = Urls.getTranscodeUrl(uri, dir.art);

      Bundle extras = new Bundle();
      extras.putInt(Extra.INT_TYPE, Type.ALBUM);
      extras.putString(Extra.STRING_KEY, dir.ratingKey);
      extras.putString(Extra.STRING_ARTIST_TITLE, dir.parentTitle);
      extras.putString(Extra.STRING_THUMB_URI, thumb);
      extras.putString(Extra.STRING_ART_URI, art);
      extras.putString(Extra.STRING_YEAR, dir.year);
      extras.putString(Extra.STRING_SERVER_ID, serverId);

      String mediaId = createAlbumMediaId(parentId, dir.ratingKey);
      return createMediaItem(mediaId, dir.title, extras, FLAG_BROWSABLE);
    };
  }

  public static Func1<Track, MediaItem> mapTrack(final HttpUrl uri, final String parentId) {
    return track -> {
      String trackUri = Urls.addPathToUrl(uri, track.media.part.key);
      String thumb = Urls.getTranscodeUrl(uri, track.thumb);
      String art = Urls.addPathToUrl(uri, track.thumb); // thumb!

      Bundle extras = new Bundle();
      extras.putInt(Extra.INT_TYPE, Type.TRACK);
      extras.putString(Extra.STRING_KEY, track.ratingKey);
      extras.putString(Extra.STRING_ALBUM_TITLE, track.parentTitle);
      extras.putString(Extra.STRING_ARTIST_TITLE, track.grandparentTitle);
      extras.putString(Extra.STRING_THUMB_URI, thumb);
      extras.putString(Extra.STRING_ART_URI, art);
      extras.putString(Extra.STRING_CONTAINER, track.media.part.container);
      extras.putString(Extra.STRING_URI, trackUri);
      extras.putInt(Extra.INT_INDEX, track.index);
      extras.putLong(Extra.LONG_DURATION, track.duration);

      String mediaId = createTrackMediaId(parentId, track.ratingKey);
      return createMediaItem(mediaId, track.title, extras, FLAG_PLAYABLE);
    };
  }
}
