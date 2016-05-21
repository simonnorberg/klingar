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
package net.simno.klingar.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import net.simno.klingar.R;

import butterknife.BindView;

public abstract class CastBaseActivity extends BaseActivity {

  @BindView(R.id.toolbar) Toolbar toolbar;

  private VideoCastManager castManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    VideoCastManager.checkGooglePlayServices(this);

    castManager = VideoCastManager.getInstance();
    castManager.reconnectSessionIfPossible();

    initializeToolbar();
  }

  @Override
  protected void onResume() {
    super.onResume();
    castManager.incrementUiCounter();
  }

  @Override
  protected void onPause() {
    super.onPause();
    castManager.decrementUiCounter();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_cast, menu);
    castManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
    return true;
  }

  private void initializeToolbar() {
    toolbar.inflateMenu(R.menu.menu_cast);
    setSupportActionBar(toolbar);
  }
}
