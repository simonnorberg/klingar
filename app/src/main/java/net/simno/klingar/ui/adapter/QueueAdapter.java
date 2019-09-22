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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Track;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;

public class QueueAdapter extends RecyclerView.Adapter<QueueViewHolder>
    implements ClickableViewHolder.ViewHolderListener {

  private final OnTrackClickListener listener;
  private List<Track> items = new ArrayList<>();
  private int position;

  public QueueAdapter(OnTrackClickListener listener) {
    this.listener = listener;
  }

  @Override @NonNull public QueueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == STATE_PLAYING) {
      return new PlayingViewHolder(inflater.inflate(R.layout.item_playing, parent, false), this);
    }
    return new QueueViewHolder(inflater.inflate(R.layout.item_queue, parent, false), this);
  }

  @Override public void onBindViewHolder(QueueViewHolder holder, int position) {
    holder.bindModel(items.get(position));
  }

  @Override public int getItemCount() {
    return items.size();
  }

  @Override public int getItemViewType(int position) {
    return this.position == position ? STATE_PLAYING : STATE_PAUSED;
  }

  @Override public void onClick(int position) {
    listener.onTrackClicked(items.get(position));
  }

  public void setQueue(List<Track> queue, int position) {
    this.items = queue;
    this.position = position;
    notifyDataSetChanged();
  }

  public interface OnTrackClickListener {
    void onTrackClicked(Track track);
  }
}
