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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import net.simno.klingar.R;
import net.simno.klingar.data.Extra;
import net.simno.klingar.ui.widget.CircleImageViewTarget;
import net.simno.klingar.util.Urls;

import butterknife.Bind;
import butterknife.BindDimen;

public final class ArtistViewHolder extends ClickableViewHolder {

  @Bind(R.id.artist_thumb) ImageView thumb;
  @Bind(R.id.artist_title) TextView title;
  @BindDimen(R.dimen.item_height) int height;

  private final RequestManager glide;

  public ArtistViewHolder(View view, RequestManager glide, OnClickListener listener) {
    super(view, listener);
    this.glide = glide;
  }

  public void bindModel(@NonNull MediaDescriptionCompat description) {
    title.setText(description.getTitle());

    Bundle extras = description.getExtras();
    if (extras == null) {
      return;
    }

    //noinspection SuspiciousNameCombination
    glide.load(Urls.addTranscodeParams(extras.getString(Extra.STRING_THUMB_URI), height, height))
        .asBitmap()
        .centerCrop()
        .into(new CircleImageViewTarget(thumb));

  }
}
