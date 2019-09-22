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
package net.simno.klingar;

import android.app.Application;
import android.content.Context;

import net.simno.klingar.util.DebugTree;

import timber.log.Timber;

public class KlingarApp extends Application {

  private final AppComponent appComponent = createComponent();

  public static KlingarApp get(Context context) {
    return (KlingarApp) context.getApplicationContext();
  }

  @Override public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Timber.plant(new DebugTree());
    }
  }

  private AppComponent createComponent() {
    return DaggerKlingarComponent.builder()
        .klingarModule(new KlingarModule(this))
        .build();
  }

  public AppComponent component() {
    return appComponent;
  }
}
