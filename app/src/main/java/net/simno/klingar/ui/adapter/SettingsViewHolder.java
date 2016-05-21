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
package net.simno.klingar.ui.adapter;

import android.view.View;
import android.widget.TextView;

import net.simno.klingar.R;

import butterknife.BindView;

final class SettingsViewHolder extends ClickableViewHolder {

  @BindView(R.id.settings_text) TextView textView;

  SettingsViewHolder(View view, OnClickListener listener) {
    super(view, listener);
  }

  void bindModel(String text) {
    textView.setText(text);
  }
}
