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

import android.content.Context;
import android.content.SharedPreferences;

import net.simno.klingar.data.api.ApiModule;
import net.simno.klingar.data.repository.RepositoryModule;

import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = {
    ApiModule.class,
    RepositoryModule.class
})
public class DataModule {

  @Provides @Singleton Prefs providePrefs(SharedPreferences preferences) {
    return new SharedPrefs(preferences);
  }

  @Provides @Singleton @Named("clientId") String provideClientId(Prefs prefs, Context context) {
    String clientId = prefs.getString("pref_client_id", null);
    if (clientId == null) {
      clientId = context.getPackageName() + "-" + UUID.randomUUID().toString();
      prefs.putString("pref_client_id", clientId);
    }
    return clientId;
  }
}
