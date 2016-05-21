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
package net.simno.klingar.ui.adapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import net.simno.klingar.R;
import net.simno.klingar.data.Extra;

import butterknife.BindView;

final class TrackViewHolder extends ClickableViewHolder {

  @BindView(R.id.track_title) TextView title;
  @BindView(R.id.track_subtitle) TextView subtitle;
  @BindView(R.id.track_duration) TextView duration;

  TrackViewHolder(View view, OnClickListener listener) {
    super(view, listener);
  }

  void bindModel(@NonNull MediaDescriptionCompat description) {
    title.setText(description.getTitle());

    Bundle extras = description.getExtras();
    if (extras == null) {
      return;
    }

    subtitle.setText(extras.getString(Extra.STRING_ARTIST_TITLE));

    String time = DateUtils.formatElapsedTime(extras.getLong(Extra.LONG_DURATION) / 1000);
    duration.setText(time);
  }
}
