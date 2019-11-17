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
package net.simno.klingar.data.repository;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import net.simno.klingar.data.Type;
import net.simno.klingar.data.api.MediaService;
import net.simno.klingar.data.api.model.Directory;
import net.simno.klingar.data.api.model.MediaContainer;
import net.simno.klingar.data.api.model.Song;
import net.simno.klingar.data.model.Album;
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.data.model.Header;
import net.simno.klingar.data.model.Library;
import net.simno.klingar.data.model.MediaType;
import net.simno.klingar.data.model.PlexItem;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.util.Pair;
import net.simno.klingar.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import okhttp3.HttpUrl;

import static net.simno.klingar.util.Urls.addPathToUrl;
import static net.simno.klingar.util.Urls.getTranscodeUrl;

class MusicRepositoryImpl implements MusicRepository {

  private static final Function<MediaContainer, Observable<Directory>> DIRS = container -> {
    if (container.directories == null) {
      return Observable.fromIterable(Collections.emptyList());
    }
    return Observable.fromIterable(container.directories);
  };

  private static final Function<MediaContainer, Observable<Song>> TRACKS = container -> {
    if (container.tracks == null) {
      return Observable.fromIterable(Collections.emptyList());
    }
    return Observable.fromIterable(container.tracks);
  };

  private final MediaService media;

  MusicRepositoryImpl(MediaService media) {
    this.media = media;
  }

  @Override public Single<List<PlexItem>> browseLibrary(Library lib) {
    return Observable.concat(mediaTypes(lib), recentlyPlayed(lib)).toList();
  }

  private Observable<PlexItem> mediaTypes(Library lib) {
    return Observable.fromArray(new MediaType[]{
        MediaType.builder()
            .title("Artists")
            .type(Type.ARTIST)
            .mediaKey("8")
            .libraryKey(lib.key())
            .libraryId(lib.uuid())
            .uri(lib.uri())
            .build(),
        MediaType.builder()
            .title("Albums")
            .type(Type.ALBUM)
            .mediaKey("9")
            .libraryKey(lib.key())
            .libraryId(lib.uuid())
            .uri(lib.uri())
            .build(),
        MediaType.builder()
            .title("Tracks")
            .type(Type.TRACK)
            .mediaKey("10")
            .libraryKey(lib.key())
            .libraryId(lib.uuid())
            .uri(lib.uri())
            .build()
    });
  }

  private Observable<PlexItem> recentlyPlayed(Library lib) {
    return media.recentArtists(lib.uri(), lib.key())
        .flatMap(DIRS)
        .map(artistMapper(lib.key(), lib.uuid(), lib.uri()))
        .startWith(Header.builder().title("Recently played").build());
  }

  @Override public Single<List<PlexItem>> browseMediaType(MediaType mt, int offset) {
    Single<List<PlexItem>> browseItems;

    if (mt.type() == Type.ARTIST) {
      browseItems = browseArtists(mt, offset);
    } else if (mt.type() == Type.ALBUM) {
      browseItems = browseAlbums(mt, offset);
    } else {
      browseItems = browseTracks(mt, offset);
    }

    return Single.zip(browseHeaders(mt), browseItems, (headers, items) -> {
      List<PlexItem> plexItems = new ArrayList<>();

      for (int i = 0; i < items.size(); ++i) {
        // The headers need to be offset by the current offset!
        if (headers.containsKey(i + offset)) {
          plexItems.add(headers.get(i + offset));
        }
        plexItems.add(items.get(i));
      }

      return plexItems;
    });
  }

  private Single<List<PlexItem>> browseArtists(MediaType mt, int offset) {
    return media.browse(mt.uri(), mt.libraryKey(), mt.mediaKey(), offset)
        .flatMap(DIRS)
        .map(artistMapper(mt.libraryKey(), mt.libraryId(), mt.uri()))
        .toList();
  }

  private Single<List<PlexItem>> browseAlbums(MediaType mt, int offset) {
    return media.browse(mt.uri(), mt.libraryKey(), mt.mediaKey(), offset)
        .flatMap(DIRS)
        .map(albumMapper(mt.libraryId(), mt.uri()))
        .toList();
  }

  private Single<List<PlexItem>> browseTracks(MediaType mt, int offset) {
    return media.browse(mt.uri(), mt.libraryKey(), mt.mediaKey(), offset)
        .flatMap(TRACKS)
        .map(trackMapper(mt.libraryId(), mt.uri()))
        .toList();
  }

  private Single<SimpleArrayMap<Integer, PlexItem>> browseHeaders(MediaType mt) {
    return media.firstCharacter(mt.uri(), mt.libraryKey(), mt.mediaKey())
        .flatMap(DIRS)
        .toList()
        .map(dirs -> {
          SimpleArrayMap<Integer, PlexItem> headers = new SimpleArrayMap<>();

          int offset = 0;
          for (int i = 0; i < dirs.size(); ++i) {
            headers.put(offset, Header.builder().title(dirs.get(i).title).build());
            offset += dirs.get(i).size;
          }

          return headers;
        });
  }

  @Override public Single<List<PlexItem>> artistItems(Artist artist) {
    return Single.zip(popularTracks(artist), albums(artist), (tracks, albums) -> {
      List<PlexItem> items = new ArrayList<>();
      if (!tracks.isEmpty()) {
        items.add(Header.builder().title("Popular").build());
        items.addAll(tracks);
      }
      if (!albums.isEmpty()) {
        items.add(Header.builder().title("Albums").build());
        items.addAll(albums);
      }
      return items;
    });
  }

  private Single<List<PlexItem>> popularTracks(Artist artist) {
    return media.popularTracks(artist.uri(), artist.libraryKey(), artist.ratingKey())
        .flatMap(TRACKS)
        .map(trackMapper(artist.libraryId(), artist.uri()))
        .toList();
  }

  private Single<List<PlexItem>> albums(Artist artist) {
    return media.albums(artist.uri(), artist.ratingKey())
        .flatMap(DIRS)
        .map(albumMapper(artist.libraryId(), artist.uri()))
        .toList();
  }

  @Override public Single<List<PlexItem>> albumItems(Album album) {
    return media.tracks(album.uri(), album.ratingKey())
        .flatMap(TRACKS)
        .map(trackMapper(album.libraryId(), album.uri()))
        .toList();
  }

  @Override public Single<Pair<List<Track>, Long>> createPlayQueue(Track track) {
    return media.playQueue(track.uri(), track.key(), track.parentKey(), track.libraryId())
        .flatMap(container -> Observable.just(container)
            .flatMap(TRACKS)
            .map(trackMapper(track.libraryId(), track.uri()))
            .map(plexItem -> (Track) plexItem)
            .toList()
            .map(tracks -> new Pair<>(tracks, container.playQueueSelectedItemID)));
  }

  @NonNull private Function<Directory, PlexItem> albumMapper(String libraryId, HttpUrl uri) {
    return dir -> Album.builder()
        .title(dir.title)
        .ratingKey(dir.ratingKey)
        .artistTitle(dir.parentTitle)
        .libraryId(libraryId)
        .thumb(getTranscodeUrl(uri, dir.thumb))
        .uri(uri)
        .build();
  }

  @NonNull
  private Function<Directory, PlexItem> artistMapper(String libKey, String libraryId, HttpUrl uri) {
    return dir -> Artist.builder()
        .title(dir.title)
        .ratingKey(dir.ratingKey)
        .libraryKey(libKey)
        .libraryId(libraryId)
        .art(getTranscodeUrl(uri, dir.art))
        .thumb(getTranscodeUrl(uri, dir.thumb))
        .uri(uri)
        .build();
  }

  @NonNull private Function<Song, PlexItem> trackMapper(String libraryId, HttpUrl uri) {
    return track -> Track.builder()
        .queueItemId(track.playQueueItemID != null ? track.playQueueItemID : 0)
        .libraryId(libraryId)
        .key(track.key)
        .ratingKey(track.ratingKey)
        .parentKey(track.parentKey)
        .title(track.title)
        .albumTitle(track.parentTitle)
        .artistTitle(track.grandparentTitle)
        .index(track.index)
        .duration(track.duration)
        .thumb(Strings.isBlank(track.thumb) ? null : addPathToUrl(uri, track.thumb).toString())
        .source(addPathToUrl(uri, track.media.part.key).toString())
        .uri(uri)
        .build();
  }
}
