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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.AppCompatEditText;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.squareup.okhttp.Credentials;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.LoginManager;
import net.simno.klingar.data.model.User;
import net.simno.klingar.data.plex.PlexService;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;
import net.simno.klingar.util.Strings;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindString;
import rx.android.schedulers.AndroidSchedulers;

public class LoginActivity extends BaseActivity {

  @Bind(R.id.login_form) LinearLayout loginForm;
  @Bind(R.id.username_edit) AppCompatEditText usernameEdit;
  @Bind(R.id.password_edit) AppCompatEditText passwordEdit;
  @Bind(R.id.content_loading) ContentLoadingProgressBar contentLoading;
  @BindString(R.string.app_name) String appName;
  @BindString(R.string.invalid_username) String invalidUsername;
  @BindString(R.string.invalid_password) String invalidPassword;

  @Inject PlexService plex;
  @Inject InputMethodManager imm;
  @Inject LoginManager loginManager;

  public static void newIntent(Context context) {
    Intent intent = new Intent(context, LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    context.startActivity(intent);
  }

  @Override
  void injectDependencies() {
    KlingarApp.get(this).component().inject(this);
  }

  @Override
  int getLayoutResource() {
    return R.layout.activity_login;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    contentLoading.hide();

    usernameEdit.requestFocus();

    passwordEdit.setOnEditorActionListener((v, actionId, event) -> {
      if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
          || (actionId == EditorInfo.IME_ACTION_DONE)) {
          login();
          return true;
      }
      return false;
    });
  }

  private void login() {
    disableInput();

    String username = usernameEdit.getText().toString();
    String password = passwordEdit.getText().toString();

    if (Strings.isBlank(username)) {
      usernameEdit.setError(invalidUsername);
      enableInput();
      return;
    }
    if (Strings.isBlank(password) || password.length() < 8) {
      passwordEdit.setError(invalidPassword);
      enableInput();
      return;
    }

    hideInputMethod();
    loginForm.setVisibility(View.INVISIBLE);
    contentLoading.show();
    subscriptions.add(plex.signIn(
        appName, Build.VERSION.RELEASE, Build.MODEL, Credentials.basic(username, password))
        .compose(bindToLifecycle())
        .compose(RxHelper.applySchedulers())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SimpleSubscriber<User>() {
          @Override
          public void onError(Throwable e) {
            super.onError(e);
            loginManager.logout();
            contentLoading.hide();
            loginForm.setVisibility(View.VISIBLE);
            showToast(R.string.sign_in_failed);
            enableInput();
          }

          @Override
          public void onNext(User user) {
            loginManager.login(user.username, user.authenticationToken);
            startMain();
          }
        }));
  }

  private void enableInput() {
    usernameEdit.setEnabled(true);
    passwordEdit.setEnabled(true);
  }

  private void disableInput() {
    usernameEdit.setEnabled(false);
    passwordEdit.setEnabled(false);
  }

  private void hideInputMethod() {
    imm.hideSoftInputFromWindow(usernameEdit.getWindowToken(), 0);
    imm.hideSoftInputFromWindow(passwordEdit.getWindowToken(), 0);
  }

  private void startMain() {
    BrowserActivity.newIntent(this);
    finish();
  }
}
