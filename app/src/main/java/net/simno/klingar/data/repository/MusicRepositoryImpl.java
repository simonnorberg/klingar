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

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import net.simno.klingar.data.Type;
import net.simno.klingar.data.api.MediaServiceHelper;
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
import net.simno.klingar.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.HttpUrl;
import rx.Observable;
import rx.functions.Func1;

import static net.simno.klingar.util.Strings.valueOrDefault;
import static net.simno.klingar.util.Urls.addPathToUrl;
import static net.simno.klingar.util.Urls.getTranscodeUrl;

class MusicRepositoryImpl implements MusicRepository {

  private static final Func1<MediaContainer, Observable<Directory>> FLATMAP_DIRS = container -> {
    if (container.directories == null) {
      return Observable.from(Collections.emptyList());
    }
    return Observable.from(container.directories);
  };

  private static final Func1<MediaContainer, Observable<Song>> FLATMAP_TRACKS = container -> {
    if (container.tracks == null) {
      return Observable.from(Collections.emptyList());
    }
    return Observable.from(container.tracks);
  };

  private final MediaServiceHelper media;

  MusicRepositoryImpl(MediaServiceHelper media) {
    this.media = media;
  }

  @Override public Observable<List<PlexItem>> browseLibrary(Library lib) {
    return Observable.zip(mediaTypes(lib), recentlyPlayed(lib), (mediaTypes, recentlyPlayed) -> {
      mediaTypes.addAll(recentlyPlayed);
      return mediaTypes;
    });
  }

  private Observable<List<PlexItem>> mediaTypes(Library lib) {
    return Observable.defer(() -> {
      List<PlexItem> mediaTypes = new ArrayList<>();
      mediaTypes.add(MediaType.builder()
          .title("Artists")
          .type(Type.ARTIST)
          .mediaKey("8")
          .libKey(lib.key())
          .uri(lib.uri())
          .build());
      mediaTypes.add(MediaType.builder()
          .title("Albums")
          .type(Type.ALBUM)
          .mediaKey("9")
          .libKey(lib.key())
          .uri(lib.uri())
          .build());
      mediaTypes.add(MediaType.builder()
          .title("Tracks")
          .type(Type.TRACK)
          .mediaKey("10")
          .libKey(lib.key())
          .uri(lib.uri())
          .build());
      return Observable.just(mediaTypes);
    });
  }

  private Observable<List<PlexItem>> recentlyPlayed(Library lib) {
    return Observable.defer(() -> media.recentArtists(lib.uri(), lib.key())
        .flatMap(FLATMAP_DIRS)
        .map(mapArtist(lib.key(), lib.uri()))
        .toList()
        .map(items -> {
          if (!items.isEmpty()) {
            items.add(0, Header.builder().title("Recently played").build());
          }
          return items;
        }));
  }

  @Override public Observable<List<PlexItem>> browseMediaType(MediaType mt, int offset) {
    Observable<List<PlexItem>> browseItems;

    if (mt.type() == Type.ARTIST) {
      browseItems = browseArtists(mt, offset);
    } else if (mt.type() == Type.ALBUM) {
      browseItems = browseAlbums(mt, offset);
    } else {
      browseItems = browseTracks(mt, offset);
    }

    return Observable.zip(browseHeaders(mt), browseItems, (headers, items) -> {
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

  private Observable<List<PlexItem>> browseArtists(MediaType mt, int offset) {
    return Observable.defer(() -> media.browse(mt.uri(), mt.libKey(), mt.mediaKey(), offset)
        .flatMap(FLATMAP_DIRS)
        .map(mapArtist(mt.libKey(), mt.uri()))
        .toList());
  }

  private Observable<List<PlexItem>> browseAlbums(MediaType mt, int offset) {
    return Observable.defer(() -> media.browse(mt.uri(), mt.libKey(), mt.mediaKey(), offset)
        .flatMap(FLATMAP_DIRS)
        .map(mapAlbum(mt.uri()))
        .toList());
  }

  private Observable<List<PlexItem>> browseTracks(MediaType mt, int offset) {
    return Observable.defer(() -> media.browse(mt.uri(), mt.libKey(), mt.mediaKey(), offset)
        .flatMap(FLATMAP_TRACKS)
        .map(mapTrack(mt.uri()))
        .toList());
  }

  private Observable<SimpleArrayMap<Integer, PlexItem>> browseHeaders(MediaType mt) {
    return Observable.defer(() -> media.firstCharacter(mt.uri(), mt.libKey(), mt.mediaKey())
        .flatMap(FLATMAP_DIRS)
        .toList()
        .map(dirs -> {
          SimpleArrayMap<Integer, PlexItem> headers = new SimpleArrayMap<>();

          int offset = 0;
          for (int i = 0; i < dirs.size(); ++i) {
            headers.put(offset, Header.builder().title(dirs.get(i).title).build());
            offset += dirs.get(i).size;
          }

          return headers;
        }));
  }

  @Override public Observable<List<PlexItem>> artistItems(Artist artist) {
    return Observable.zip(popularTracks(artist), albums(artist), (tracks, albums) -> {
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

  private Observable<List<PlexItem>> popularTracks(Artist artist) {
    return Observable.defer(() -> media.popularTracks(artist.uri(), artist.libKey(), artist.key())
        .flatMap(FLATMAP_TRACKS)
        .map(mapTrack(artist.uri()))
        .toList());
  }

  private Observable<List<PlexItem>> albums(Artist artist) {
    return Observable.defer(() -> media.albums(artist.uri(), artist.key())
        .flatMap(FLATMAP_DIRS)
        .map(mapAlbum(artist.uri()))
        .toList());
  }

  @Override public Observable<List<PlexItem>> albumItems(Album album) {
    return Observable.defer(() -> media.tracks(album.uri(), album.key())
        .flatMap(FLATMAP_TRACKS)
        .map(mapTrack(album.uri()))
        .toList());
  }

  @NonNull private Func1<Directory, PlexItem> mapAlbum(HttpUrl uri) {
    return dir -> Album.builder()
        .title(dir.title)
        .key(dir.ratingKey)
        .artistTitle(dir.parentTitle)
        .year(valueOrDefault(dir.year, ""))
        .art(getTranscodeUrl(uri, dir.art))
        .thumb(getTranscodeUrl(uri, dir.thumb))
        .uri(uri)
        .build();
  }

  @NonNull private Func1<Directory, PlexItem> mapArtist(String libKey, HttpUrl uri) {
    return dir -> Artist.builder()
        .title(dir.title)
        .key(dir.ratingKey)
        .libKey(libKey)
        .art(getTranscodeUrl(uri, dir.art))
        .thumb(getTranscodeUrl(uri, dir.thumb))
        .uri(uri)
        .build();
  }

  @NonNull private Func1<Song, PlexItem> mapTrack(HttpUrl uri) {
    return track -> Track.builder()
        .title(track.title)
        .albumTitle(track.parentTitle)
        .artistTitle(track.grandparentTitle)
        .index(track.index)
        .duration(track.duration)
        .thumb(Strings.isBlank(track.thumb) ? null : addPathToUrl(uri, track.thumb).toString())
        .uri(addPathToUrl(uri, track.media.part.key))
        .build();
  }
}
