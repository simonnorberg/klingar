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

import android.content.SharedPreferences;
import android.util.Base64;

import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;

import net.simno.klingar.data.plex.AuthTokenInterceptor;
import net.simno.klingar.util.Strings;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class LoginManager {

  private static final Entity ENTITY_USERNAME = new Entity("entity_username");
  private static final Entity ENTITY_AUTH_TOKEN = new Entity("entity_auth_token");
  private static final String PREF_USERNAME = "pref_username";
  private static final String PREF_AUTH_TOKEN = "pref_auth_token";

  private final AuthTokenInterceptor authTokenInterceptor;
  private final SharedPreferences preferences;
  private final Crypto crypto;
  private String username;
  private String authToken;

  @Inject
  public LoginManager(AuthTokenInterceptor authTokenInterceptor, SharedPreferences preferences,
                      Crypto crypto) {
    this.authTokenInterceptor = authTokenInterceptor;
    this.preferences = preferences;
    this.crypto = crypto;
    setUsername(decrypt(preferences.getString(PREF_USERNAME, null), ENTITY_USERNAME));
    setAuthToken(decrypt(preferences.getString(PREF_AUTH_TOKEN, null), ENTITY_AUTH_TOKEN));
  }

  public void login(String username, String authToken) {
    setUsername(username);
    setAuthToken(authToken);
    preferences.edit()
        .putString(PREF_USERNAME, encrypt(username, ENTITY_USERNAME))
        .putString(PREF_AUTH_TOKEN, encrypt(authToken, ENTITY_AUTH_TOKEN))
        .apply();
  }

  public void logout() {
    setUsername(null);
    setAuthToken(null);
    preferences.edit()
        .remove(PREF_USERNAME)
        .remove(PREF_AUTH_TOKEN)
        .apply();
  }

  public boolean isLoggedOut() {
    return Strings.isBlank(username) || Strings.isBlank(authToken);
  }

  public String getUsername() {
    return username;
  }

  private void setUsername(String username) {
    this.username = username;
  }

  private void setAuthToken(String authToken) {
    this.authToken = authToken;
    authTokenInterceptor.setAuthToken(authToken);
  }

  private String encrypt(final String plain, final Entity entity) {
    if (!crypto.isAvailable()) {
      return null;
    }

    if (Strings.isBlank(plain) || entity == null) {
      return null;
    }

    String cipher = null;
    try {
      byte[] cipherBytes = crypto.encrypt(plain.getBytes(), entity);
      cipher = Base64.encodeToString(cipherBytes, Base64.NO_WRAP);
    } catch (KeyChainException | CryptoInitializationException | IOException e) {
      Timber.e(e, e.getMessage());
    }

    return cipher;
  }

  private String decrypt(final String cipher, final Entity entity) {
    if (!crypto.isAvailable()) {
      return null;
    }

    if (Strings.isBlank(cipher) || entity == null) {
      return null;
    }

    String plain = null;
    try {
      byte[] plainBytes = crypto.decrypt(Base64.decode(cipher, Base64.NO_WRAP), entity);
      plain = new String(plainBytes);
    } catch (KeyChainException | CryptoInitializationException | IOException e) {
      Timber.e(e, e.getMessage());
    }

    return plain;
  }
}
