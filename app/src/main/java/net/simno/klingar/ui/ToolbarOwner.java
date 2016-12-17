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
package net.simno.klingar.ui;

import android.content.Context;
import android.graphics.Paint;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay.PublishRelay;

import net.simno.klingar.R;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

/**
 * Allows shared configuration of the toolbar
 */
@Singleton
public class ToolbarOwner {

  public static final int TITLE_VISIBLE = 255;
  public static final int TITLE_GONE = 0;
  private final PublishRelay<Integer> spinnerRelay = PublishRelay.create();
  private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int transparent;
  private final int primary;

  private Activity activity;
  private Config config;

  @Inject public ToolbarOwner(Context context) {
    this.transparent = ContextCompat.getColor(context, android.R.color.transparent);
    this.primary = ContextCompat.getColor(context, R.color.primary);
    titlePaint.setColor(ContextCompat.getColor(context, android.R.color.white));
  }

  public void takeActivity(Activity activity) {
    this.activity = activity;
    update();
  }

  public void dropActivity() {
    this.activity = null;
  }

  public Config getConfig() {
    return config;
  }

  public void setConfig(Config config) {
    this.config = config;
    update();
  }

  public void spinnerItemSelected(int position) {
    spinnerRelay.call(position);
  }

  public Observable<Integer> spinnerSelection() {
    return spinnerRelay;
  }

  private void update() {
    if (config == null || activity == null) {
      return;
    }

    activity.setToolbarBackgroundColor(config.background() ? primary : transparent);

    activity.setHomeAsUpEnabled(config.backNavigation());

    titlePaint.setAlpha(config.titleAlpha());
    activity.setToolbarTitleColor(titlePaint.getColor());

    if (config.title() != null) {
      activity.setToolbarTitle(config.title());
      activity.setShowTitleEnabled(true);
    } else {
      activity.setShowTitleEnabled(false);
    }

    ArrayList<String> options = config.options();
    Integer selection = config.selection();
    if (options != null && selection != null) {
      activity.showToolbarSpinner(options, selection);
    } else {
      activity.hideToolbarSpinner();
    }
  }

  interface Activity {
    void setShowTitleEnabled(boolean enabled);
    void setHomeAsUpEnabled(boolean enabled);
    void setToolbarTitle(CharSequence title);
    void setToolbarTitleColor(int color);
    void setToolbarBackgroundColor(int color);
    void showToolbarSpinner(List<String> options, int selection);
    void hideToolbarSpinner();
  }

  @AutoValue
  public abstract static class Config implements Parcelable {
    public static Builder builder() {
      return new AutoValue_ToolbarOwner_Config.Builder();
    }

    @Nullable public abstract String title();

    @Nullable public abstract ArrayList<String> options();

    @Nullable public abstract Integer selection();

    public abstract int titleAlpha();

    public abstract boolean background();

    public abstract boolean backNavigation();

    public abstract Builder toBuilder();

    @AutoValue.Builder public abstract static class Builder {
      public abstract Builder title(String title);
      public abstract Builder options(ArrayList<String> options);
      public abstract Builder selection(Integer selection);
      public abstract Builder titleAlpha(int titleAlpha);
      public abstract Builder background(boolean background);
      public abstract Builder backNavigation(boolean backNavigation);
      public abstract Config build();
    }
  }
}
