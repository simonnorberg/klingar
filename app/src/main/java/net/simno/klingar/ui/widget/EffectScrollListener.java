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

import android.support.v7.widget.RecyclerView;

import net.simno.klingar.ui.ToolbarOwner;

public class EffectScrollListener extends DistanceScrollListener {

  private static final int ALPHA_MAX = 0xff;
  private static final int STATE_HIDDEN = 0;
  private static final int STATE_FADING = 1;
  private static final int STATE_SHOWN = 2;

  private final ToolbarOwner toolbarOwner;
  private final BackgroundLayout backgroundLayout;

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
   * Current toolbar title visibility state
   */
  private int toolbarState;

  public EffectScrollListener(ToolbarOwner toolbarOwner, BackgroundLayout backgroundLayout,
                              int backgroundHeight, int toolbarHeight, int distance) {
    super(distance);
    this.toolbarOwner = toolbarOwner;
    this.backgroundLayout = backgroundLayout;
    this.visibleDistance = backgroundHeight - toolbarHeight;
    this.fadeStart = visibleDistance - toolbarHeight;
    this.fadeStop = visibleDistance - fadeStart;
    this.toolbarState = STATE_HIDDEN;
  }

  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    super.onScrolled(recyclerView, dx, dy);

    backgroundLayout.onScrolled(distance);

    if (distance < fadeStart) {
      if (toolbarState != STATE_HIDDEN) {
        toolbarOwner.hideTitleAndBackground();
        toolbarState = STATE_HIDDEN;
      }
    } else if (distance < visibleDistance) {
      toolbarOwner.setTitleColorAlpha((int) (((distance - fadeStart) / fadeStop) * ALPHA_MAX));
      toolbarState = STATE_FADING;
    } else if (toolbarState != STATE_SHOWN) {
      toolbarOwner.showTitleAndBackground();
      toolbarState = STATE_SHOWN;
    }
  }
}
