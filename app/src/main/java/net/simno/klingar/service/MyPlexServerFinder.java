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
package net.simno.klingar.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.data.ServerManager;
import net.simno.klingar.data.model.Connection;
import net.simno.klingar.data.model.Device;
import net.simno.klingar.data.model.MediaContainer;
import net.simno.klingar.data.plex.PlexService;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import okhttp3.HttpUrl;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Finds plex servers using the plex cloud service if logged in.
 */
public class MyPlexServerFinder extends IntentService {

  @Inject ServerManager serverManager;
  @Inject PlexService plex;

  public static void start(Context context) {
    Intent intent = new Intent(context, MyPlexServerFinder.class);
    intent.addFlags(START_NOT_STICKY);
    context.startService(intent);
  }

  public MyPlexServerFinder() {
    super("MyPlexServerFinder");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    KlingarApp.get(this).component().inject(this);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      Response<MediaContainer> response = plex.resources().execute();
      if (!response.isSuccessful()) {
        return;
      }
      List<Device> devices = response.body().devices;
      if (devices != null && !devices.isEmpty()) {
        for (Device device : devices) {
          if (device.provides.contains("server")) {
            addServer(device);
          }
        }
      }
    } catch (IOException e) {
      Timber.e(e, e.getMessage());
    }
  }

  private void addServer(Device server) {
    for (Device.Connection c : server.connections) {
      if (c.local == 0 && server.accessToken != null) {

        HttpUrl url = HttpUrl.parse(c.uri).newBuilder()
            .addQueryParameter("X-Plex-Token", server.accessToken)
            .build();

        Connection connection = Connection.create(url);
        serverManager.addConnection(server.clientIdentifier, server.name, connection);
        return;
      }
    }
  }
}
