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

import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;

import net.simno.klingar.R;
import net.simno.klingar.data.Type;

import java.util.ArrayList;
import java.util.List;

import static net.simno.klingar.util.MediaItemHelper.getViewType;

public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final ArrayList<MediaItem> items = new ArrayList<>();
  private final OnItemClickListener listener;
  private final RequestManager glide;

  public interface OnItemClickListener {
    void onItemClicked(MediaItem mediaItem);
  }

  public MediaAdapter(OnItemClickListener listener, RequestManager glide) {
    this.listener = listener;
    this.glide = glide;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());

    if (viewType == Type.ARTIST) {
      View view = inflater.inflate(R.layout.item_artist, parent, false);
      return new ArtistViewHolder(view, glide, position -> {
        if (listener != null) {
          listener.onItemClicked(items.get(position));
        }
      });
    }

    if (viewType == Type.ALBUM) {
      View view = inflater.inflate(R.layout.item_album, parent, false);
      return new AlbumViewHolder(view, glide, position -> {
        if (listener != null) {
          listener.onItemClicked(items.get(position));
        }
      });
    }

    if (viewType == Type.TRACK) {
      View view = inflater.inflate(R.layout.item_track, parent, false);
      return new TrackViewHolder(view, position -> {
        if (listener != null) {
          listener.onItemClicked(items.get(position));
        }
      });
    }

    if (viewType == Type.PLAYLIST) {
      View view = inflater.inflate(R.layout.item_playlist, parent, false);
      return new PlaylistViewHolder(view, glide, position -> {
        if (listener != null) {
          listener.onItemClicked(items.get(position));
        }
      });
    }

    if (viewType == Type.MEDIA_TYPE) {
      View view = inflater.inflate(R.layout.item_media_type, parent, false);
      return new MediaTypeViewHolder(view, position -> {
        if (listener != null) {
          listener.onItemClicked(items.get(position));
        }
      });
    }

    if (viewType == Type.HEADER) {
      return new HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false));
    }

    return null;
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    MediaItem mediaItem = items.get(position);
    MediaDescriptionCompat description = mediaItem.getDescription();

    switch (getViewType(mediaItem)) {
      case Type.ARTIST:
        ((ArtistViewHolder) holder).bindModel(description);
        break;
      case Type.ALBUM:
        ((AlbumViewHolder) holder).bindModel(description);
        break;
      case Type.TRACK:
        ((TrackViewHolder) holder).bindModel(description);
        break;
      case Type.PLAYLIST:
        ((PlaylistViewHolder) holder).bindModel(description);
        break;
      case Type.MEDIA_TYPE:
        ((MediaTypeViewHolder) holder).bindModel(description);
        break;
      case Type.HEADER:
        ((HeaderViewHolder) holder).bindModel(description);
        break;
    }
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public int getItemViewType(int position) {
    MediaItem mediaItem = items.get(position);
    return getViewType(mediaItem);
  }

  public void add(MediaItem item) {
    this.items.add(item);
    notifyDataSetChanged();
  }

  public void addAll(List<MediaItem> items) {
    this.items.addAll(items);
    notifyDataSetChanged();
  }

  public void set(List<MediaItem> items) {
    this.items.clear();
    this.items.addAll(items);
    notifyDataSetChanged();
  }

  public ArrayList<MediaItem> getItems() {
    return items;
  }
}
