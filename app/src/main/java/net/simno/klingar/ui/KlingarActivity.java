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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.LoginManager;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.MusicService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class KlingarActivity extends AppCompatActivity {

  @BindView(R.id.controller_container) ViewGroup container;

  @Inject LoginManager loginManager;
  @Inject MusicController musicController;
  private Router router;
  private boolean bound;

  private final ServiceConnection connection = new ServiceConnection() {
    @Override public void onServiceConnected(ComponentName className, IBinder service) {
      bound = true;
    }

    @Override public void onServiceDisconnected(ComponentName arg0) {
      bound = false;
    }
  };

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KlingarApp.get(this).component().inject(this);
    setContentView(R.layout.activity_klingar);
    ButterKnife.bind(this);

    router = Conductor.attachRouter(this, container, savedInstanceState);
    if (savedInstanceState == null) {
      if (loginManager.isLoggedOut()) {
        router.setRoot(RouterTransaction.with(new LoginController(null)));
      } else {
        router.setRoot(RouterTransaction.with(new BrowserController(null)));
      }
    }
  }

  @Override protected void onStart() {
    super.onStart();
    bindService(new Intent(this, MusicService.class), connection, Context.BIND_AUTO_CREATE);
  }

  @Override protected void onStop() {
    super.onStop();
    if (bound) {
      unbindService(connection);
      bound = false;
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.licenses:
        showCredits();
        return true;
      case R.id.sign_out:
        logout();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed();
    }
  }

  private void showCredits() {
    startActivity(new Intent(this, OssLicensesMenuActivity.class));
  }

  private void logout() {
    musicController.stop();
    loginManager.logout();
    router.setRoot(RouterTransaction.with(new LoginController(null)));
  }
}
