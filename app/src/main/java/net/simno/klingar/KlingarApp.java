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
package net.simno.klingar;

import android.app.Application;
import android.content.Context;

import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import net.simno.klingar.ui.activity.PlayerActivity;
import net.simno.klingar.util.DebugTree;
import net.simno.klingar.util.ReleaseTree;

import timber.log.Timber;

public class KlingarApp extends Application {

  private final AppComponent appComponent = createComponent();

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG) {
      Timber.plant(new DebugTree());
    } else {
      Timber.plant(new ReleaseTree());
    }

    CastConfiguration options = new CastConfiguration.Builder(BuildConfig.CAST_APP_ID)
        .enableAutoReconnect()
        .enableDebug()
        .enableLockScreen()
        .enableWifiReconnection()
        .setTargetActivity(PlayerActivity.class)
        .build();

    VideoCastManager.initialize(this, options);
  }

  protected AppComponent createComponent() {
    return DaggerKlingarComponent.builder()
        .klingarModule(new KlingarModule(this))
        .build();
  }

  public AppComponent component() {
    return appComponent;
  }

  public static KlingarApp get(Context context) {
    return (KlingarApp) context.getApplicationContext();
  }
}
