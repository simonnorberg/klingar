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

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import net.simno.klingar.util.RxHelper;

import butterknife.ButterKnife;
import rx.subscriptions.CompositeSubscription;

import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;

abstract class BaseActivity extends RxAppCompatActivity {

  CompositeSubscription subscriptions;

  abstract void injectDependencies();
  abstract int getLayoutResource();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    injectDependencies();
    setContentView(getLayoutResource());
    ButterKnife.bind(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    subscriptions = RxHelper.getSubscriptions(subscriptions);
  }

  @Override
  protected void onPause() {
    super.onPause();
    RxHelper.unsubscribe(subscriptions);
  }

  void showToast(int resId) {
    Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show();
  }

  boolean isIntentAvailable(Intent intent) {
    return getPackageManager().queryIntentActivities(intent, MATCH_DEFAULT_ONLY).size() > 0;
  }
}
