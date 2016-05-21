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
package net.simno.klingar.ui.fragment;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.simno.klingar.BuildConfig;
import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.LoginManager;
import net.simno.klingar.ui.ToolbarOwner;
import net.simno.klingar.ui.adapter.SettingsAdapter;
import net.simno.klingar.ui.callback.DummySettingsListener;
import net.simno.klingar.ui.callback.SettingsListener;
import net.simno.klingar.ui.widget.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindDrawable;
import butterknife.BindView;

public class SettingsFragment extends BaseFragment implements SettingsAdapter.OnItemClickListener {

  @BindView(R.id.recycler_view) RecyclerView recyclerView;
  @BindDrawable(R.drawable.item_divider) Drawable itemDivider;

  @Inject ToolbarOwner toolbarOwner;
  @Inject LoginManager loginManager;

  private SettingsListener settingsListener;

  public static SettingsFragment newInstance() {
    return new SettingsFragment();
  }

  public SettingsFragment() {
  }

  @Override
  int getLayoutResource() {
    return R.layout.fragment_settings;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    settingsListener = (SettingsListener) activity;
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

    List<String> settings = new ArrayList<>();
    settings.add(getString(R.string.settings_sign_out, loginManager.getUsername()));
    settings.add(getString(R.string.settings_oss_licenses));
    settings.add(getString(R.string.settings_repo));
    settings.add(getString(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.GIT_SHA));
    SettingsAdapter adapter = new SettingsAdapter(this, settings);

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setHasFixedSize(true);
    recyclerView.addItemDecoration(new DividerItemDecoration(itemDivider));
    recyclerView.setAdapter(adapter);

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    toolbarOwner.setTitle(R.string.settings);
    toolbarOwner.showTitleAndBackground();
  }

  @Override
  public void onDetach() {
    settingsListener = new DummySettingsListener();
    super.onDetach();
  }

  @Override
  public void onItemClicked(int position) {
    switch (position) {
      case 0:
        settingsListener.signOut();
        break;
      case 1:
        settingsListener.showLicenses();
        break;
      case 2:
        settingsListener.showRepo();
        break;
    }
  }
}
