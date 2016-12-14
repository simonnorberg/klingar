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

import net.simno.klingar.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static net.simno.klingar.util.Views.gone;
import static net.simno.klingar.util.Views.visible;

public abstract class BaseActivity extends AppCompatActivity implements ToolbarOwner.Activity,
    AdapterView.OnItemSelectedListener {

  protected Router router;
  @BindView(R.id.controller_container) ViewGroup container;
  @BindView(R.id.toolbar) @Nullable Toolbar toolbar;
  @BindView(R.id.toolbar_libs_spinner) @Nullable AppCompatSpinner spinner;

  protected abstract void injectDependencies();
  protected abstract int getLayoutResource();
  protected abstract void initView();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    injectDependencies();
    setContentView(getLayoutResource());
    ButterKnife.bind(this);
    initActionBar();
    router = Conductor.attachRouter(this, container, savedInstanceState);
    if (savedInstanceState == null) {
      initView();
    }
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
    if (toolbar != null) {
      setSupportActionBar(toolbar);
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
    if (toolbar != null) {
      toolbar.setTitleTextColor(color);
    }
  }

  @Override public void setToolbarBackgroundColor(int color) {
    if (toolbar != null) {
      toolbar.setBackgroundColor(color);
    }
  }

  @Override public void showToolbarSpinner(List<String> options, int selection) {
    if (toolbar != null && spinner != null) {
      ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, options);
      spinner.setAdapter(adapter);
      spinner.setSelection(selection);
      spinner.setOnItemSelectedListener(this);
      visible(spinner);
    }
  }

  @Override public void hideToolbarSpinner() {
    if (toolbar != null && spinner != null) {
      gone(spinner);
      spinner.setOnItemSelectedListener(null);
      spinner.setAdapter(null);
    }
  }

  @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
  }

  @Override public void onNothingSelected(AdapterView<?> adapterView) {
  }
}
