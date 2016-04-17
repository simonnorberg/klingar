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
package net.simno.klingar.data;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.ArrayMap;
import android.util.SparseArray;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Server;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;

import static net.simno.klingar.util.MediaIdHelper.MEDIA_ID_ROOT;
import static net.simno.klingar.util.MediaIdHelper.getAlbumKey;
import static net.simno.klingar.util.MediaIdHelper.getArtistKey;
import static net.simno.klingar.util.MediaIdHelper.getLastChild;
import static net.simno.klingar.util.MediaIdHelper.getLibraryKey;
import static net.simno.klingar.util.MediaIdHelper.getMediaKey;
import static net.simno.klingar.util.MediaIdHelper.getMediaType;
import static net.simno.klingar.util.MediaIdHelper.getOffset;
import static net.simno.klingar.util.MediaIdHelper.getPlaylistKey;
import static net.simno.klingar.util.MediaIdHelper.getServerId;
import static net.simno.klingar.util.MediaIdHelper.getType;
import static net.simno.klingar.util.MediaItemHelper.createHeaderMediaItem;

@Singleton
public class MusicProvider {

  private final ServerManager serverManager;
  private final ConcurrentHashMap<String, MediaMetadataCompat> trackMetadata = new ConcurrentHashMap<>();
  private final String headerRecentlyPlayed;
  private final String headerPopular;
  private final String headerAlbums;

  @Inject
  public MusicProvider(ServerManager serverManager, Resources resources) {
    this.serverManager = serverManager;
    this.headerRecentlyPlayed = resources.getString(R.string.header_recently_played);
    this.headerPopular = resources.getString(R.string.header_popular);
    this.headerAlbums = resources.getString(R.string.header_albums);
  }

  public Observable<List<MediaItem>> media(String mediaId) {
    if (Strings.equals(mediaId, MEDIA_ID_ROOT)) {
      return mediaRoot();
    }

    String serverId = getServerId(mediaId);
    SparseArray<Object> child = getLastChild(mediaId);

    switch (getType(child)) {
      case Type.PLAYLIST:
        return playlistItems(serverId, getPlaylistKey(child), mediaId);
      case Type.LIBRARY:
        return recentArtists(serverId, getLibraryKey(child), mediaId);
      case Type.MEDIA_TYPE:
        return browse(serverId, getLibraryKey(child), getMediaType(child), getMediaKey(child),
            getOffset(child), mediaId);
      case Type.ARTIST:
        return artistItems(serverId, getLibraryKey(child), getArtistKey(child), mediaId);
      case Type.ALBUM:
        return albumItems(serverId, getAlbumKey(child), mediaId);
    }

    return Observable.just(Collections.<MediaItem>emptyList());
  }

  private Observable<List<MediaItem>> mediaRoot() {
    return serverManager.libs()
        .map((Func1<ArrayList<MediaItem>, List<MediaItem>>) mediaItems -> mediaItems)
        .take(1);
  }

  private Observable<List<MediaItem>> playlistItems(String serverId, String playlistKey,
                                                    String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().playlistItems(playlistKey)
        .flatMap(RxHelper.FLATMAP_TRACKS)
        .map(RxHelper.mapTrack(server.currentUri(), parentId))
        .toList();
  }

  private Observable<List<MediaItem>> albumItems(String serverId, String albumKey, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().tracks(albumKey)
        .flatMap(RxHelper.FLATMAP_TRACKS)
        .map(RxHelper.mapTrack(server.currentUri(), parentId))
        .toList();
  }

  private Observable<List<MediaItem>> recentArtists(String serverId, String libKey, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().recentArtists(libKey)
        .flatMap(RxHelper.FLATMAP_DIRS)
        .map(RxHelper.mapArtist(libKey, serverId, server.currentUri(), parentId))
        .toList()
        .map(artists -> {
          List<MediaItem> mediaItems = new ArrayList<>();
          if (!artists.isEmpty()) {
            mediaItems.add(createHeaderMediaItem(headerRecentlyPlayed));
            mediaItems.addAll(artists);
          }
          return mediaItems;
        });
  }

  private Observable<List<MediaItem>> browse(String serverId, String libKey, int mediaType,
                                             int mediaKey, final int offset, String parentId) {

    Observable<List<MediaItem>> browseItems;
    if (mediaType == Type.ARTIST) {
      browseItems = browseArtists(serverId, libKey, mediaKey, offset, parentId);
    } else if (mediaType == Type.ALBUM) {
      browseItems = browseAlbums(serverId, libKey, mediaKey, offset, parentId);
    } else {
      browseItems = browseTracks(serverId, libKey, mediaKey, offset, parentId);
    }

    return Observable.zip(
        browseHeaders(serverId, libKey, mediaKey),
        browseItems,
        (headers, items) -> {
          List<MediaItem> mediaItems = new ArrayList<>();

          for (int i = 0; i < items.size(); ++i) {
            // The headers need to be offset by the current offset!
            if (headers.containsKey(i + offset)) {
              mediaItems.add(headers.get(i + offset));
            }
            mediaItems.add(items.get(i));
          }

          return mediaItems;
        });
  }

  private Observable<ArrayMap<Integer, MediaItem>> browseHeaders(String serverId, String libKey,
                                                                 int mediaKey) {
    Server server = serverManager.getServer(serverId);
    return server.media().firstCharacter(libKey, mediaKey)
        .flatMap(RxHelper.FLATMAP_DIRS)
        .toList()
        .map(dirs -> {
          ArrayMap<Integer, MediaItem> headers = new ArrayMap<>();

          int offset = 0;
          for (int i = 0; i < dirs.size(); ++i) {
            headers.put(offset, createHeaderMediaItem(dirs.get(i).title));
            offset += dirs.get(i).size;
          }

          return headers;
        });
  }

  private Observable<List<MediaItem>> browseArtists(String serverId, String libKey, int mediaKey,
                                                    int offset, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().browse(libKey, mediaKey, offset)
        .flatMap(RxHelper.FLATMAP_DIRS)
        .map(RxHelper.mapArtist(libKey, serverId, server.currentUri(), parentId))
        .toList();
  }

  private Observable<List<MediaItem>> browseAlbums(String serverId, String libKey, int mediaKey,
                                                   int offset, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().browse(libKey, mediaKey, offset)
        .flatMap(RxHelper.FLATMAP_DIRS)
        .map(RxHelper.mapAlbum(serverId, server.currentUri(), parentId))
        .toList();
  }

  private Observable<List<MediaItem>> browseTracks(String serverId, String libKey, int mediaKey,
                                                   int offset, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().browse(libKey, mediaKey, offset)
        .flatMap(RxHelper.FLATMAP_TRACKS)
        .map(RxHelper.mapTrack(server.currentUri(), parentId))
        .toList();
  }

  private Observable<List<MediaItem>> artistItems(String serverId, String libKey, String artistKey,
                                                  String parentId) {
    return Observable.zip(
        popularTracks(serverId, libKey, artistKey, parentId),
        albums(serverId, artistKey, parentId),
        (tracks, albums) -> {
          List<MediaItem> items = new ArrayList<>();
          if (!tracks.isEmpty()) {
            items.add(createHeaderMediaItem(headerPopular));
            items.addAll(tracks);
          }
          if (!albums.isEmpty()) {
            items.add(createHeaderMediaItem(headerAlbums));
            items.addAll(albums);
          }
          return items;
        });
  }

  private Observable<List<MediaItem>> popularTracks(String serverId, String libKey,
                                                    String artistKey, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().popularTracks(libKey, artistKey)
        .flatMap(RxHelper.FLATMAP_TRACKS)
        .map(RxHelper.mapTrack(server.currentUri(), parentId))
        .toList();
  }

  private Observable<List<MediaItem>> albums(String serverId, String artistKey, String parentId) {
    Server server = serverManager.getServer(serverId);
    return server.media().albums(artistKey)
        .flatMap(RxHelper.FLATMAP_DIRS)
        .map(RxHelper.mapAlbum(serverId, server.currentUri(), parentId))
        .toList();
  }

  public synchronized MediaMetadataCompat getMetadata(String mediaId) {
    return trackMetadata.get(mediaId);
  }

  public synchronized MediaMetadataCompat getMetadata(MediaDescriptionCompat description) {
    String mediaId = description.getMediaId();
    if (Strings.isBlank(mediaId)) {
      return null;
    }
    if (trackMetadata.containsKey(mediaId)) {
      return trackMetadata.get(mediaId);
    }
    Bundle extras = description.getExtras();
    if (extras == null) {
      return null;
    }
    MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, String.valueOf(description.getTitle()))
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, extras.getString(Extra.STRING_ALBUM_TITLE))
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, extras.getString(Extra.STRING_ARTIST_TITLE))
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, extras.getString(Extra.STRING_ART_URI))
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, extras.getLong(Extra.LONG_DURATION))
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, extras.getInt(Extra.INT_INDEX))
        .build();
    trackMetadata.put(description.getMediaId(), metadata);
    return metadata;
  }

  public synchronized void updateMetadata(String mediaId, MediaMetadataCompat metadata) {
    trackMetadata.put(mediaId, metadata);
  }
}
