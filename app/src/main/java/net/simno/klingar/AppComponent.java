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

import net.simno.klingar.service.MusicService;
import net.simno.klingar.service.MyPlexServerFinder;
import net.simno.klingar.ui.activity.BrowserActivity;
import net.simno.klingar.ui.activity.LoginActivity;
import net.simno.klingar.ui.activity.MainActivity;
import net.simno.klingar.ui.activity.PlayerActivity;
import net.simno.klingar.ui.fragment.BrowserFragment;
import net.simno.klingar.ui.fragment.DetailFragment;
import net.simno.klingar.ui.fragment.LicensesFragment;
import net.simno.klingar.ui.fragment.SettingsFragment;

public interface AppComponent {
  void inject(BrowserActivity activity);
  void inject(LoginActivity activity);
  void inject(MainActivity activity);
  void inject(PlayerActivity activity);

  void inject(BrowserFragment fragment);
  void inject(DetailFragment fragment);
  void inject(LicensesFragment fragment);
  void inject(SettingsFragment fragment);

  void inject(MusicService service);
  void inject(MyPlexServerFinder service);

  void inject(MediaNotificationManager manager);
}
