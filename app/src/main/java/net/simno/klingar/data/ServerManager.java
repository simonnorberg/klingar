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

import android.text.TextUtils;

import com.jakewharton.rxrelay2.BehaviorRelay;

import net.simno.klingar.data.api.MediaService;
import net.simno.klingar.data.api.PlexService;
import net.simno.klingar.data.api.model.Device;
import net.simno.klingar.data.model.Library;
import net.simno.klingar.data.model.Server;
import net.simno.klingar.util.Rx;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import okhttp3.HttpUrl;

@Singleton
public class ServerManager {

  private final BehaviorRelay<List<Library>> libsRelay = BehaviorRelay.create();
  private final PlexService plex;
  private final MediaService media;
  private final Rx rx;
  private Disposable disposable;

  @Inject ServerManager(PlexService plex, MediaService media, Rx rx) {
    this.plex = plex;
    this.media = media;
    this.rx = rx;
  }

  public Flowable<List<Library>> libs() {
    return libsRelay.toFlowable(BackpressureStrategy.LATEST);
  }

  public void refresh() {
    Rx.dispose(disposable);
    disposable = plex.resources()
        .flatMap(container -> Observable.fromIterable(container.devices))
        .filter(device -> device.provides.contains("server"))
        .map(this::createServer)
        .flatMap(createLibrary())
        .toList()
        .compose(rx.singleSchedulers())
        .subscribe(libsRelay, Rx::onError);
  }

  private Server createServer(Device device) {
    Server.Builder builder = Server.builder();

    for (Device.Connection connection : device.connections) {
      if (connection.local == 0) {
        HttpUrl parsedUrl = HttpUrl.parse(connection.uri);
        if (parsedUrl != null) {
          builder.uri(parsedUrl.newBuilder()
              .addQueryParameter("X-Plex-Token", device.accessToken)
              .build());
          break;
        }
      }
    }

    return builder.build();
  }

  private Function<Server, Observable<Library>> createLibrary() {
    return server -> media.sections(server.uri())
        .flatMap(container -> Observable.fromIterable(container.directories))
        .filter(section -> TextUtils.equals(section.type, "artist"))
        .flatMap(section -> Observable.just(Library.builder()
            .uuid(section.uuid)
            .key(section.key)
            .name(section.title)
            .uri(server.uri())
            .build()));
  }
}
