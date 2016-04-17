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

import android.support.v4.media.MediaBrowserCompat.MediaItem;

import net.simno.klingar.data.media.HostInterceptor;
import net.simno.klingar.data.media.MediaService;
import net.simno.klingar.data.media.MediaServiceHelper;
import net.simno.klingar.data.model.Connection;
import net.simno.klingar.data.model.Server;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;
import net.simno.klingar.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

@Singleton
public class ServerManager {

  private final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, List<MediaItem>> libs = new ConcurrentHashMap<>();
  private final BehaviorSubject<ArrayList<MediaItem>> libsObservable = BehaviorSubject.create();
  private final OkHttpClient sharedClient;
  private final SimpleXmlConverterFactory converterFactory;

  @Inject
  public ServerManager(@Named("shared") OkHttpClient client,
                       SimpleXmlConverterFactory converterFactory) {
    this.sharedClient = client;
    this.converterFactory = converterFactory;
  }

  public synchronized void addConnection(String id, String name, Connection connection) {
    if (servers.containsKey(id)) {
      Server server = servers.get(id);
      server.addConnection(connection);
    } else {
      MediaServiceHelper mediaHelper = createMediaServiceHelper(connection.uri);
      servers.put(id, new Server(id, name, mediaHelper, connection));
    }

    updateLibs(id);
  }

  private MediaServiceHelper createMediaServiceHelper(HttpUrl baseUrl) {
    HostInterceptor hostInterceptor = new HostInterceptor();

    OkHttpClient mediaClient = sharedClient.newBuilder()
        .addInterceptor(hostInterceptor)
        .build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(baseUrl)
        .callFactory(mediaClient)
        .addConverterFactory(converterFactory)
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();

    MediaService mediaService = retrofit.create(MediaService.class);

    return MediaServiceHelper.create(hostInterceptor, mediaService);
  }

  public Server getServer(String id) {
    return servers.get(id);
  }

  public Observable<ArrayList<MediaItem>> libs() {
    return libsObservable;
  }

  private void updateLibs(final String id) {
    final Server server = servers.get(id);
    final String serverName = server.getName();

    Timber.d("updateLibs %s", serverName);

    server.media().sections()
        .flatMap(RxHelper.FLATMAP_DIRS)
        .filter(dir -> Strings.equals(dir.type, "artist"))
        .map(RxHelper.mapLibrary(id, serverName))
        .toList()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(new SimpleSubscriber<List<MediaItem>>() {
          @Override
          public void onNext(List<MediaItem> newLibs) {
            Timber.d("onNext newLibs %s %s", id, newLibs.size());
            libs.put(id, newLibs); // Overwrite all existing libs for this server
            notifyLibsUpdate();
          }
        });
  }

  private void notifyLibsUpdate() {
    Timber.d("notifyLibsUpdate");
    // Make a list of all libs from all servers
    ArrayList<MediaItem> allLibs = new ArrayList<>();

    for (List<MediaItem> list : libs.values()) {
      allLibs.addAll(list);
    }

    libsObservable.onNext(allLibs);
  }
}
