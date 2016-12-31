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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Header;

import butterknife.BindView;
import butterknife.ButterKnife;

final class HeaderViewHolder extends RecyclerView.ViewHolder {

  @BindView(R.id.header_title) TextView title;

  HeaderViewHolder(View view) {
    super(view);
    ButterKnife.bind(this, view);
  }

  void bindModel(@NonNull Header header) {
    title.setText(header.title());
  }
}
