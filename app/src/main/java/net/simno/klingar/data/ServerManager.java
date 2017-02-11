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

import com.jakewharton.rxrelay.BehaviorRelay;

import net.simno.klingar.data.api.MediaServiceHelper;
import net.simno.klingar.data.api.PlexService;
import net.simno.klingar.data.api.model.Device;
import net.simno.klingar.data.api.model.Directory;
import net.simno.klingar.data.model.Library;
import net.simno.klingar.data.model.Server;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.HttpUrl;
import rx.Observable;

@Singleton
public class ServerManager {

  private final BehaviorRelay<List<Library>> libsRelay = BehaviorRelay.create();
  private final PlexService plex;
  private final MediaServiceHelper media;

  @Inject ServerManager(PlexService plex, MediaServiceHelper media) {
    this.plex = plex;
    this.media = media;
  }

  public Observable<List<Library>> libs() {
    return libsRelay.onBackpressureLatest();
  }

  public void refresh() {
    plex.resources()
        .flatMap(container -> Observable.from(container.devices))
        .filter(device -> device.provides.contains("server"))
        .map(this::parseServer)
        .flatMap(server -> Observable.combineLatest(
            Observable.just(server),
            media.sections(server.uri())
                .flatMap(container -> Observable.from(container.directories))
                .filter(section -> TextUtils.equals(section.type, "artist")),
            this::parseLibrary))
        .toList()
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<List<Library>>() {
          @Override public void onNext(List<Library> libs) {
            libsRelay.call(libs);
          }
        });
  }

  private Server parseServer(Device device) {
    Server.Builder builder = Server.builder();

    for (Device.Connection connection : device.connections) {
      if (connection.local == 0) {
        HttpUrl uri = HttpUrl.parse(connection.uri).newBuilder()
            .addQueryParameter("X-Plex-Token", device.accessToken)
            .build();
        builder.uri(uri);
        break;
      }
    }

    return builder.build();
  }

  private Library parseLibrary(Server server, Directory section) {
    return Library.builder()
        .uuid(section.uuid)
        .key(section.key)
        .name(section.title)
        .uri(server.uri())
        .build();
  }
}
