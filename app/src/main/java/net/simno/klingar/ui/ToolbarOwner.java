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
package net.simno.klingar.ui;

import android.content.Context;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;

import net.simno.klingar.R;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Allows shared configuration of the toolbar
 */
@Singleton
public class ToolbarOwner {

  private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int transparent;
  private final int primary;
  private final int titleColor;
  private Activity activity;

  @Inject
  public ToolbarOwner(Context context) {
    this.transparent = ContextCompat.getColor(context, android.R.color.transparent);
    this.primary = ContextCompat.getColor(context, R.color.primary);
    this.titleColor = ContextCompat.getColor(context, android.R.color.white);
    titlePaint.setColor(titleColor);
    setActivity(null);
  }

  public void setActivity(Activity activity) {
    if (activity != null) {
      this.activity = activity;
    } else {
      this.activity = new DummyActivity();
    }
  }

  public void setTitle(CharSequence title) {
    activity.setToolbarTitle(title);
  }

  public void setTitle(int resId) {
    activity.setToolbarTitle(resId);
  }

  public void setTitleColorAlpha(int alpha) {
    activity.setToolbarBackgroundColor(transparent);
    titlePaint.setAlpha(alpha);
    activity.setToolbarTitleColor(titlePaint.getColor());
  }

  public void hideTitleAndBackground() {
    activity.setToolbarBackgroundColor(transparent);
    activity.setToolbarTitleColor(transparent);
  }

  public void showTitleAndBackground() {
    activity.setToolbarBackgroundColor(primary);
    activity.setToolbarTitleColor(titleColor);
  }

  private static class DummyActivity implements Activity {

    @Override
    public void setToolbarTitle(CharSequence title) {
    }

    @Override
    public void setToolbarTitle(int resId) {
    }

    @Override
    public void setToolbarTitleColor(int color) {
    }

    @Override
    public void setToolbarBackgroundColor(int color) {
    }
  }

  public interface Activity {
    void setToolbarTitle(CharSequence title);
    void setToolbarTitle(int resId);
    void setToolbarTitleColor(int color);
    void setToolbarBackgroundColor(int color);
  }
}
