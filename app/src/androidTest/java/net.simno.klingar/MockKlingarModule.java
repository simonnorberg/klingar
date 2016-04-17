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

import android.content.Context;

import com.bumptech.glide.RequestManager;
import com.facebook.crypto.Crypto;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module
public class MockKlingarModule {

  @Provides @Singleton
  Context provideApplicationContext() {
    return mock(Context.class);
  }

  @Provides @Singleton
  RequestManager provideGlide() {
    return mock(RequestManager.class);
  }

  @Provides @Singleton
  Crypto provideCrypto() {
    return mock(Crypto.class);
  }
}
