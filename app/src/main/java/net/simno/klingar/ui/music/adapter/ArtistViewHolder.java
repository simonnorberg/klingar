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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.ui.widget.CircleImageViewTarget;
import net.simno.klingar.util.Urls;

import butterknife.BindDimen;
import butterknife.BindView;

final class ArtistViewHolder extends ClickableViewHolder {

  @BindView(R.id.artist_thumb) ImageView thumb;
  @BindView(R.id.artist_title) TextView title;
  @BindDimen(R.dimen.item_height) int height;

  ArtistViewHolder(View view, OnClickListener listener) {
    super(view, listener);
  }

  void bindModel(@NonNull Artist artist) {
    title.setText(artist.title());

    //noinspection SuspiciousNameCombination
    Glide.with(itemView.getContext())
        .load(Urls.addTranscodeParams(artist.thumb(), height, height))
        .asBitmap()
        .centerCrop()
        .into(new CircleImageViewTarget(thumb));
  }
}
