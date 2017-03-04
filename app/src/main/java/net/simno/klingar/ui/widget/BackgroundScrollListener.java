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

import android.support.v7.widget.RecyclerView;

import static net.simno.klingar.ui.ToolbarOwner.TITLE_GONE;
import static net.simno.klingar.ui.ToolbarOwner.TITLE_VISIBLE;

public class BackgroundScrollListener extends DistanceScrollListener {

  private static final int ALPHA_MAX = 0xff;

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

  private Controller controller;

  public BackgroundScrollListener(int orientation, int backgroundHeight, int toolbarHeight) {
    super(orientation);
    this.visibleDistance = backgroundHeight - toolbarHeight;
    this.fadeStart = visibleDistance - toolbarHeight;
    this.fadeStop = visibleDistance - fadeStart;
  }

  @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    super.onScrolled(recyclerView, dx, dy);
    if (controller == null) {
      return;
    }

    controller.onScrolled(distance);

    int titleAlpha = controller.getTitleAlpha();

    if (distance < fadeStart) {
      if (titleAlpha != TITLE_GONE) {
        controller.setConfig(TITLE_GONE, false);
      }
    } else if (distance < visibleDistance) {
      controller.setConfig((int) (((distance - fadeStart) / fadeStop) * ALPHA_MAX), false);
    } else if (titleAlpha != TITLE_VISIBLE) {
      controller.setConfig(TITLE_VISIBLE, true);
    }
  }

  public void setController(Controller controller) {
    this.controller = controller;
  }

  public interface Controller {
    void onScrolled(int distance);
    int getTitleAlpha();
    void setConfig(int titleAlpha, boolean background);
  }
}
