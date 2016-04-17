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
package net.simno.klingar.ui.widget;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Based on https://gist.github.com/polbins/e37206fbc444207c0e92
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

  private final Drawable divider;

  public DividerItemDecoration(Drawable divider) {
    this.divider = divider;
  }

  @Override
  public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
     // Drawable width defines start/end padding. Drawable height defines divider height.
    final int start = parent.getPaddingStart() + divider.getIntrinsicWidth();
    final int end = parent.getWidth() - parent.getPaddingEnd() - divider.getIntrinsicWidth();
    final int height = divider.getIntrinsicHeight();

    for (int i = 0; i < parent.getChildCount(); ++i) {
      View child = parent.getChildAt(i);
      RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

      int top = child.getBottom() + params.bottomMargin;
      int bottom = top + height;

      divider.setBounds(start, top, end, bottom);
      divider.draw(c);
    }
  }
}
