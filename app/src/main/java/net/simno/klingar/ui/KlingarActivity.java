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
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.LoginManager;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.MusicService;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.psdev.licensesdialog.LicensesDialog;

import static net.simno.klingar.util.Views.gone;
import static net.simno.klingar.util.Views.visible;

public class KlingarActivity extends AppCompatActivity implements ToolbarOwner.Activity,
    AdapterView.OnItemSelectedListener {

  @BindView(R.id.controller_container) ViewGroup container;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.toolbar_libs_spinner) Spinner spinner;

  @Inject LoginManager loginManager;
  @Inject ToolbarOwner toolbarOwner;
  @Inject MusicController musicController;
  private Router router;
  private CastContext castContext;
  private MenuItem mediaRouteMenuItem;
  private boolean bound;

  private final CastStateListener castStateListener = newState -> {
    if (newState != CastState.NO_DEVICES_AVAILABLE) {
      new Handler().postDelayed(() -> {
        if (mediaRouteMenuItem != null && mediaRouteMenuItem.isVisible()) {
          showCastIntroductoryOverlay();
        }
      }, 1000);
    }
  };

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
    initActionBar();
    toolbarOwner.takeActivity(this);
    castContext = CastContext.getSharedInstance(this);
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

  @Override protected void onResume() {
    super.onResume();
    castContext.addCastStateListener(castStateListener);
  }

  @Override protected void onPause() {
    super.onPause();
    castContext.removeCastStateListener(castStateListener);
  }

  @Override protected void onStop() {
    super.onStop();
    if (bound) {
      unbindService(connection);
      bound = false;
    }
  }

  @Override protected void onDestroy() {
    toolbarOwner.dropActivity();
    super.onDestroy();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    if (loginManager.isLoggedOut()) {
      return super.onCreateOptionsMenu(menu);
    }
    getMenuInflater().inflate(R.menu.menu_main, menu);
    mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
        R.id.media_route_menu_item);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.credits:
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

  @Override public void setShowTitleEnabled(boolean enabled) {
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(enabled);
    }
  }

  @Override public void setHomeAsUpEnabled(boolean enabled) {
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(enabled);
    }
  }

  @Override public void setToolbarTitle(CharSequence title) {
    setTitle(title);
  }

  @Override public void setToolbarTitleColor(int color) {
    toolbar.setTitleTextColor(color);
  }

  @Override public void setToolbarBackgroundColor(int color) {
    toolbar.setBackgroundColor(color);
  }

  @Override public void showToolbarSpinner(List<String> options, int selection) {
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, options);
    spinner.setAdapter(adapter);
    spinner.setSelection(selection);
    spinner.setOnItemSelectedListener(this);
    visible(spinner);
  }

  @Override public void hideToolbarSpinner() {
    gone(spinner);
    spinner.setOnItemSelectedListener(null);
    spinner.setAdapter(null);
  }

  @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    toolbarOwner.spinnerItemSelected(i);
  }

  @Override public void onNothingSelected(AdapterView<?> adapterView) {
  }

  private void initActionBar() {
    toolbar.inflateMenu(R.menu.menu_main);
    setSupportActionBar(toolbar);
  }

  private void showCastIntroductoryOverlay() {
    Menu menu = toolbar.getMenu();
    View view = menu.findItem(R.id.media_route_menu_item).getActionView();
    if (view != null && view instanceof MediaRouteButton) {
      IntroductoryOverlay overlay = new IntroductoryOverlay.Builder(this, mediaRouteMenuItem)
          .setTitleText(R.string.touch_to_cast)
          .setSingleTime()
          .build();
      overlay.show();
    }
  }

  private void showCredits() {
    new LicensesDialog.Builder(this)
        .setIncludeOwnLicense(true)
        .setShowFullLicenseText(false)
        .setNotices(R.raw.notices)
        .setNoticesCssStyle(R.string.notices_style)
        .build()
        .show();
  }

  private void logout() {
    musicController.stop();
    loginManager.logout();
    router.setRoot(RouterTransaction.with(new LoginController(null)));
    supportInvalidateOptionsMenu();
  }
}
