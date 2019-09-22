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

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

  private final Drawable divider;
  private final Rect bounds = new Rect();

  public DividerItemDecoration(Drawable divider) {
    this.divider = divider;
  }

  @Override public void onDraw(
      @NonNull Canvas c,
      RecyclerView parent,
      @NonNull RecyclerView.State state
  ) {
    if (parent.getLayoutManager() == null) {
      return;
    }
    drawVertical(c, parent);
  }

  private void drawVertical(Canvas canvas, RecyclerView parent) {
    canvas.save();
    // Drawable width defines left/right padding. Drawable height defines divider height.
    final int left = parent.getPaddingStart() + divider.getIntrinsicWidth();
    final int right = parent.getWidth() - parent.getPaddingEnd() - divider.getIntrinsicWidth();

    final int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = parent.getChildAt(i);
      parent.getDecoratedBoundsWithMargins(child, bounds);
      final int bottom = bounds.bottom + Math.round(child.getTranslationY());
      final int top = bottom - divider.getIntrinsicHeight();
      divider.setBounds(left, top, right, bottom);
      divider.draw(canvas);
    }
    canvas.restore();
  }

  @Override
  public void getItemOffsets(
      Rect outRect,
      @NonNull View view,
      @NonNull RecyclerView parent,
      @NonNull RecyclerView.State state
  ) {
    outRect.set(0, 0, 0, divider.getIntrinsicHeight());
  }
}
