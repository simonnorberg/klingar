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
package net.simno.klingar.ui.login;

import android.content.Context;
import android.content.Intent;

import com.bluelinelabs.conductor.RouterTransaction;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.ui.BaseActivity;

public class LoginActivity extends BaseActivity {

  public static void newIntent(Context context) {
    Intent intent = new Intent(context, LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    context.startActivity(intent);
  }

  @Override protected void injectDependencies() {
    KlingarApp.get(this).component().inject(this);
  }

  @Override protected int getLayoutResource() {
    return R.layout.activity_login;
  }

  @Override protected void initView() {
    router.setRoot(RouterTransaction.with(new LoginController(null)));
  }
}
