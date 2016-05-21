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

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.keychain.KeyChain;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class KlingarModule {

  private final KlingarApp app;

  public KlingarModule(KlingarApp app) {
    this.app = app;
  }

  @Provides @Singleton
  Context provideApplicationContext() {
    return app.getApplicationContext();
  }

  @Provides @Singleton
  RequestManager provideGlide(Context context) {
    return Glide.with(context);
  }

  @Provides @Singleton
  Crypto provideCrypto(Context context) {
    KeyChain keyChain = new SharedPrefsBackedKeyChain(context, CryptoConfig.KEY_256);
    return AndroidConceal.get().createDefaultCrypto(keyChain);
  }
}
