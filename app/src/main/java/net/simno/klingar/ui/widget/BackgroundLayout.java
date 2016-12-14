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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Layout that will add parallax and fading to its child imageview
 */
@SuppressLint("ViewConstructor")
public class BackgroundLayout extends RelativeLayout {

  private static final int ALPHA_MAX = 0xff;
  private static final float PARALLAX = 1.4f;

  private final ImageView background;
  private int visibleDistance;

  public BackgroundLayout(Context context, @NonNull ImageView background, int visibleDistance) {
    super(context);
    addView(background);
    this.background = background;
    this.background.setImageAlpha(ALPHA_MAX);
    if (visibleDistance <= 0) {
      // We will divide by visibleDistance so it cannot be zero!
      throw new IllegalArgumentException("visibleDistance must be positive.");
    }
    this.visibleDistance = visibleDistance;
  }

  public void onScrolled(int distance) {
    // Move layout the total scrolled distance
    setTranslationY(-distance);

    // Create parallax on the background image
    background.setTranslationY(distance / PARALLAX);

    // Fade background image
    if (distance <= visibleDistance) {
      background.setImageAlpha(
          (int) (ALPHA_MAX - (((float) distance / visibleDistance) * ALPHA_MAX)));
    }
  }
}
