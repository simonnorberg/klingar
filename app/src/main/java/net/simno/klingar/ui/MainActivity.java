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
package net.simno.klingar.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.data.LoginManager;
import net.simno.klingar.ui.login.LoginActivity;
import net.simno.klingar.ui.music.MusicActivity;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

  @Inject LoginManager loginManager;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KlingarApp.get(this).component().inject(this);
    if (loginManager.isLoggedOut()) {
      LoginActivity.newIntent(this);
    } else {
      MusicActivity.newIntent(this);
    }
    finish();
  }
}
