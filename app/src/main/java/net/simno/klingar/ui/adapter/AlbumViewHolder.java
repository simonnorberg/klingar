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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Album;
import net.simno.klingar.util.Urls;

import butterknife.BindDimen;
import butterknife.BindView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

final class AlbumViewHolder extends ClickableViewHolder<Album> {

  @BindView(R.id.album_thumb) ImageView thumb;
  @BindView(R.id.album_title) TextView title;
  @BindView(R.id.album_subtitle) TextView subtitle;
  @BindDimen(R.dimen.item_height) int height;

  AlbumViewHolder(View view, ViewHolderListener listener) {
    super(view, listener);
  }

  @Override void bindModel(@NonNull Album album) {
    title.setText(album.title());

    //noinspection SuspiciousNameCombination
    Glide.with(itemView.getContext())
        .load(Urls.addTranscodeParams(album.thumb(), height, height))
        .apply(RequestOptions.centerCropTransform())
        .transition(withCrossFade())
        .into(thumb);

    subtitle.setText(album.artistTitle());
  }
}
