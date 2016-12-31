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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.LoginManager;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static net.simno.klingar.util.Views.gone;
import static net.simno.klingar.util.Views.visible;

public class KlingarActivity extends AppCompatActivity implements ToolbarOwner.Activity,
    AdapterView.OnItemSelectedListener {

  @BindView(R.id.controller_container) ViewGroup container;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.toolbar_libs_spinner) AppCompatSpinner spinner;

  @Inject LoginManager loginManager;
  @Inject ToolbarOwner toolbarOwner;
  private Router router;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KlingarApp.get(this).component().inject(this);
    setContentView(R.layout.activity_klingar);
    ButterKnife.bind(this);
    initActionBar();
    toolbarOwner.takeActivity(this);
    router = Conductor.attachRouter(this, container, savedInstanceState);
    if (savedInstanceState == null) {
      if (loginManager.isLoggedOut()) {
        router.setRoot(RouterTransaction.with(new LoginController(null)));
      } else {
        router.setRoot(RouterTransaction.with(new BrowserController(null)));
      }
    }
  }

  @Override protected void onDestroy() {
    toolbarOwner.dropActivity();
    super.onDestroy();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed();
    }
  }

  private void initActionBar() {
    setSupportActionBar(toolbar);
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
}
