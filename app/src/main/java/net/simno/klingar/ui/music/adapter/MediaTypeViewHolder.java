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
package net.simno.klingar.ui.music.adapter;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import net.simno.klingar.R;
import net.simno.klingar.data.model.MediaType;

import butterknife.BindView;

final class MediaTypeViewHolder extends ClickableViewHolder {

  @BindView(R.id.media_type_title) TextView title;

  MediaTypeViewHolder(View view, OnClickListener listener) {
    super(view, listener);
  }

  void bindModel(@NonNull MediaType mediaType) {
    title.setText(mediaType.title());
  }
}
