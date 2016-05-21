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
package net.simno.klingar.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.ui.ToolbarOwner;

import javax.inject.Inject;

import butterknife.BindView;

public class LicensesFragment extends BaseFragment {

  @BindView(R.id.web_view) WebView webView;

  @Inject ToolbarOwner toolbarOwner;

  public static LicensesFragment newInstance() {
    return new LicensesFragment();
  }

  public LicensesFragment() {
  }

  @Override
  int getLayoutResource() {
    return R.layout.fragment_licenses;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KlingarApp.get(getActivity()).component().inject(this);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.loadUrl("file:///android_asset/licenses.html");
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    toolbarOwner.setTitle(R.string.settings_oss_licenses);
    toolbarOwner.showTitleAndBackground();
  }
}
