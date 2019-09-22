/*
 * Copyright (C) 2017 Simon Norberg
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

import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;

final class PlayingViewHolder extends QueueViewHolder {

  PlayingViewHolder(View view, ViewHolderListener listener) {
    super(view, listener);
  }

  @Override void bindModel(@NonNull Track track) {
    super.bindModel(track);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      AnimatedVectorDrawable avd = (AnimatedVectorDrawable) ContextCompat.getDrawable(
          itemView.getContext(), R.drawable.equalizer);
      if (avd != null) {
        avd.registerAnimationCallback(new Animatable2.AnimationCallback() {
          @Override public void onAnimationEnd(Drawable drawable) {
            avd.start();
          }
        });
        avd.start();
      }
      title.setCompoundDrawablesWithIntrinsicBounds(avd, null, null, null);
    }
  }
}
