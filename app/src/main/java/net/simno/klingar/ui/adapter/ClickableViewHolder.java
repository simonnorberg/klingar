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
package net.simno.klingar.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import butterknife.ButterKnife;

/**
 * ClickableViewHolder adds a click listener to the default ViewHolder
 */
public class ClickableViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

  public interface OnClickListener {
    void onClick(int position);
  }

  private final OnClickListener listener;

  ClickableViewHolder(View view, OnClickListener listener) {
    super(view);
    this.listener = listener;
    ButterKnife.bind(this, view);
    view.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    final int position = getAdapterPosition();
    if (position >= 0 && listener != null) {
      listener.onClick(position);
    }
  }
}
