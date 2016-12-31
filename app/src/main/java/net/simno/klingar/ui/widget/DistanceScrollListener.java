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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class DistanceScrollListener extends RecyclerView.OnScrollListener {

  private final int orientation;
  int distance;
  private int portraitDistance = Integer.MAX_VALUE;
  private int landscapeDistance = Integer.MAX_VALUE;

  public DistanceScrollListener(int orientation) {
    this.orientation = orientation;
  }

  @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    distance = Math.max(0, distance + dy);
    if (orientation == ORIENTATION_PORTRAIT) {
      portraitDistance = distance;
    } else if (orientation == ORIENTATION_LANDSCAPE) {
      landscapeDistance = distance;
    }
  }

  public void onSaveViewState(@NonNull Bundle outState) {
    Bundle scrollState = new Bundle();
    scrollState.putInt("distance", distance);
    scrollState.putInt("portraitDistance", portraitDistance);
    scrollState.putInt("landscapeDistance", landscapeDistance);
    outState.putBundle("scroll", scrollState);
  }

  public void onRestoreViewState(@NonNull Bundle savedViewState) {
    Bundle scrollState = savedViewState.getBundle("scroll");
    if (scrollState == null) {
      return;
    }
    distance = scrollState.getInt("distance");
    portraitDistance = scrollState.getInt("portraitDistance");
    landscapeDistance = scrollState.getInt("landscapeDistance");

    if (orientation == ORIENTATION_PORTRAIT) {
      distance = Math.min(distance, portraitDistance);
    } else if (orientation == ORIENTATION_LANDSCAPE) {
      distance = Math.min(distance, landscapeDistance);
    }
  }
}
