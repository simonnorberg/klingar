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

import androidx.annotation.NonNull;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Header;

import butterknife.BindView;

final class HeaderViewHolder extends BaseViewHolder<Header> {

  @BindView(R.id.header_title) TextView title;

  HeaderViewHolder(View view) {
    super(view);
  }

  @Override void bindModel(@NonNull Header header) {
    title.setText(header.title());
  }
}
