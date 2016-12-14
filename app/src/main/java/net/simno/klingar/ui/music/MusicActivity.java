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
package net.simno.klingar.ui.music;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;

import com.bluelinelabs.conductor.RouterTransaction;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.ui.BaseActivity;
import net.simno.klingar.ui.ToolbarOwner;

import javax.inject.Inject;

public class MusicActivity extends BaseActivity {

  @Inject ToolbarOwner toolbarOwner;

  public static void newIntent(Context context) {
    context.startActivity(new Intent(context, MusicActivity.class));
  }

  @Override protected void injectDependencies() {
    KlingarApp.get(this).component().inject(this);
  }

  @Override protected int getLayoutResource() {
    return R.layout.activity_music;
  }

  @Override protected void initView() {
    router.setRoot(RouterTransaction.with(new BrowserController(null)));
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    toolbarOwner.takeActivity(this);
  }

  @Override protected void onDestroy() {
    toolbarOwner.dropActivity();
    super.onDestroy();
  }

  @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    toolbarOwner.spinnerItemSelected(i);
  }
}
