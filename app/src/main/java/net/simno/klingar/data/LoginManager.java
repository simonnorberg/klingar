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

import net.simno.klingar.data.api.AuthInterceptor;
import net.simno.klingar.util.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LoginManager {

  private static final String PREF_USERNAME = "pref_username";
  private static final String PREF_AUTH_TOKEN = "pref_auth_token";

  private final AuthInterceptor authInterceptor;
  private final Prefs prefs;
  private String username;
  private String authToken;

  @Inject LoginManager(AuthInterceptor authInterceptor, Prefs prefs) {
    this.authInterceptor = authInterceptor;
    this.prefs = prefs;
    setUsername(prefs.getString(PREF_USERNAME, null));
    setAuthToken(prefs.getString(PREF_AUTH_TOKEN, null));
  }

  public void login(String username, String authToken) {
    setUsername(username);
    setAuthToken(authToken);
    prefs.putString(PREF_USERNAME, username);
    prefs.putString(PREF_AUTH_TOKEN, authToken);
  }

  public void logout() {
    setUsername(null);
    setAuthToken(null);
    prefs.remove(PREF_USERNAME);
    prefs.remove(PREF_AUTH_TOKEN);
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
    authInterceptor.setAuthToken(authToken);
  }
}
