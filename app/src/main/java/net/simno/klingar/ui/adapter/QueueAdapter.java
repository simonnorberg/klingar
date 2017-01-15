/*
 * Copyright (C) 2017 Simon Norberg
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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.PlayQueue;
import net.simno.klingar.playback.PlayState;

import java.util.ArrayList;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueViewHolder> {

  private final OnTrackClickListener listener;
  private List<Track> items = new ArrayList<>();
  private int index;

  public QueueAdapter(OnTrackClickListener listener) {
    this.listener = listener;
  }

  @Override public QueueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == PlayState.PLAYING) {
      return new PlayingViewHolder(inflater.inflate(R.layout.item_playing, parent, false),
          listener::onTrackClicked);
    }
    return new QueueViewHolder(inflater.inflate(R.layout.item_queue, parent, false),
        listener::onTrackClicked);
  }

  @Override public void onBindViewHolder(QueueViewHolder holder, int position) {
    holder.bindModel(items.get(position));
  }

  @Override public int getItemCount() {
    return items.size();
  }

  @Override public int getItemViewType(int position) {
    return position == index ? PlayState.PLAYING : PlayState.PAUSED;
  }

  public void setQueue(PlayQueue queue) {
    this.items = queue.queue();
    this.index = queue.index();
    notifyDataSetChanged();
  }

  public interface OnTrackClickListener {
    void onTrackClicked(int position);
  }
}
