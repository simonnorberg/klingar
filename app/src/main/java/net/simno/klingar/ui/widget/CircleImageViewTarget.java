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
package net.simno.klingar.ui.widget;

import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

/**
 * View target for Glide that clips the image to a circle
 */
public class CircleImageViewTarget extends BitmapImageViewTarget {

  public CircleImageViewTarget(ImageView view) {
    super(view);
  }

  @Override
  public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
    super.onResourceReady(bitmap, glideAnimation);

    RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(view.getResources(),
        bitmap);
    rounded.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()));
    rounded.setAntiAlias(true);

    view.setImageDrawable(rounded);
  }
}
