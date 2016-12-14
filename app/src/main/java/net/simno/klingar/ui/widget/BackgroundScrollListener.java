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

import net.simno.klingar.ui.ToolbarOwner;

import static net.simno.klingar.ui.ToolbarOwner.TITLE_GONE;
import static net.simno.klingar.ui.ToolbarOwner.TITLE_VISIBLE;

public class BackgroundScrollListener extends RecyclerView.OnScrollListener {

  private static final int ALPHA_MAX = 0xff;

  private final BackgroundLayout backgroundLayout;
  private final ToolbarOwner toolbarOwner;

  /**
   * The distance when backgroundlayout is visible
   */
  private final int visibleDistance;

  /**
   * The distance when to start fading toolbar title (title is hidden below this)
   */
  private final int fadeStart;

  /**
   * The distance when to stop fading toolbar title (title is shown above this)
   */
  private final float fadeStop;

  /**
   * Total distance scrolled (absolute value)
   */
  private int distance;

  public BackgroundScrollListener(@NonNull BackgroundLayout backgroundLayout,
                                  @NonNull ToolbarOwner toolbarOwner, int backgroundHeight,
                                  int toolbarHeight) {
    this.backgroundLayout = backgroundLayout;
    this.toolbarOwner = toolbarOwner;
    this.visibleDistance = backgroundHeight - toolbarHeight;
    this.fadeStart = visibleDistance - toolbarHeight;
    this.fadeStop = visibleDistance - fadeStart;
    this.distance = 0;
  }

  @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    distance = Math.abs(distance + dy);

    backgroundLayout.onScrolled(distance);

    ToolbarOwner.Config config = toolbarOwner.getConfig();

    if (distance < fadeStart) {
      if (config.titleAlpha() != TITLE_GONE) {
        toolbarOwner.setConfig(config.toBuilder()
            .background(false)
            .titleAlpha(TITLE_GONE)
            .build());
      }
    } else if (distance < visibleDistance) {
      toolbarOwner.setConfig(config.toBuilder()
          .background(false)
          .titleAlpha((int) (((distance - fadeStart) / fadeStop) * ALPHA_MAX))
          .build());
    } else if (config.titleAlpha() != TITLE_VISIBLE) {
      toolbarOwner.setConfig(config.toBuilder()
          .background(true)
          .titleAlpha(TITLE_VISIBLE)
          .build());
    }
  }

  public Bundle onSaveState() {
    Bundle outState = new Bundle();
    outState.putInt("distance", distance);
    return outState;
  }

  public void onRestoreState(@NonNull Bundle savedState) {
    distance = savedState.getInt("distance", 0);
  }
}
