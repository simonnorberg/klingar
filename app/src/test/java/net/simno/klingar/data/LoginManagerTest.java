/*
 * Copyright (C) 2017 Simon Norberg
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class LoginManagerTest {

  @Mock AuthInterceptor mockAuthInterceptor;
  @Mock Prefs mockPrefs;
  private LoginManager loginManager;

  @Before public void setup() {
    loginManager = new LoginManager(mockAuthInterceptor, mockPrefs);
  }

  @Test public void login() {
    loginManager.login("testToken");
    assertFalse(loginManager.isLoggedOut());
  }

  @Test public void logout() {
    loginManager.login("testToken");
    loginManager.logout();
    assertTrue(loginManager.isLoggedOut());
  }
}
