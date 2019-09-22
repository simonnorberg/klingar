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
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.util.Urls;

import butterknife.BindDimen;
import butterknife.BindView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

final class ArtistViewHolder extends ClickableViewHolder<Artist> {

  @BindView(R.id.artist_thumb) ImageView thumb;
  @BindView(R.id.artist_title) TextView title;
  @BindDimen(R.dimen.item_height) int height;

  ArtistViewHolder(View view, ViewHolderListener listener) {
    super(view, listener);
  }

  @Override void bindModel(@NonNull Artist artist) {
    title.setText(artist.title());

    //noinspection SuspiciousNameCombination
    Glide.with(itemView.getContext())
        .load(Urls.addTranscodeParams(artist.thumb(), height, height))
        .apply(RequestOptions.circleCropTransform())
        .transition(withCrossFade())
        .into(thumb);
  }
}
