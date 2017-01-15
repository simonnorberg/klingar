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
package net.simno.klingar.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.simno.klingar.R;
import net.simno.klingar.data.model.Album;
import net.simno.klingar.data.model.Artist;
import net.simno.klingar.data.model.Header;
import net.simno.klingar.data.model.MediaType;
import net.simno.klingar.data.model.Playlist;
import net.simno.klingar.data.model.PlexItem;
import net.simno.klingar.data.model.Track;

import java.util.ArrayList;
import java.util.List;

import static net.simno.klingar.data.Type.ALBUM;
import static net.simno.klingar.data.Type.ARTIST;
import static net.simno.klingar.data.Type.HEADER;
import static net.simno.klingar.data.Type.MEDIA_TYPE;
import static net.simno.klingar.data.Type.PLAYLIST;
import static net.simno.klingar.data.Type.TRACK;

public class MusicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final OnPlexItemClickListener listener;
  private List<PlexItem> items = new ArrayList<>();

  public MusicAdapter(OnPlexItemClickListener listener) {
    this.listener = listener;
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    switch (viewType) {
      case ARTIST:
        return new ArtistViewHolder(inflater.inflate(R.layout.item_artist, parent, false),
            position -> listener.onPlexItemClicked(items.get(position), position));
      case ALBUM:
        return new AlbumViewHolder(inflater.inflate(R.layout.item_album, parent, false),
            position -> listener.onPlexItemClicked(items.get(position), position));
      case TRACK:
        return new TrackViewHolder(inflater.inflate(R.layout.item_track, parent, false),
            position -> listener.onPlexItemClicked(items.get(position), position));
      case PLAYLIST:
        return new PlaylistViewHolder(inflater.inflate(R.layout.item_playlist, parent, false),
            position -> listener.onPlexItemClicked(items.get(position), position));
      case MEDIA_TYPE:
        return new MediaTypeViewHolder(inflater.inflate(R.layout.item_media_type, parent, false),
            position -> listener.onPlexItemClicked(items.get(position), position));
      case HEADER:
        return new HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false));
      default:
        return null;
    }
  }

  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    Object item = items.get(position);
    switch (getItemViewType(position)) {
      case ARTIST:
        ((ArtistViewHolder) holder).bindModel((Artist) item);
        break;
      case ALBUM:
        ((AlbumViewHolder) holder).bindModel((Album) item);
        break;
      case TRACK:
        ((TrackViewHolder) holder).bindModel((Track) item);
        break;
      case PLAYLIST:
        ((PlaylistViewHolder) holder).bindModel((Playlist) item);
        break;
      case MEDIA_TYPE:
        ((MediaTypeViewHolder) holder).bindModel((MediaType) item);
        break;
      case HEADER:
        ((HeaderViewHolder) holder).bindModel((Header) item);
        break;
      default:
    }
  }

  @Override public int getItemCount() {
    return items.size();
  }

  @Override public int getItemViewType(int position) {
    PlexItem item = items.get(position);
    if (item instanceof Artist) {
      return ARTIST;
    } else if (item instanceof Album) {
      return ALBUM;
    } else if (item instanceof Track) {
      return TRACK;
    } else if (item instanceof Playlist) {
      return PLAYLIST;
    } else if (item instanceof MediaType) {
      return MEDIA_TYPE;
    } else {
      return HEADER;
    }
  }

  public void add(PlexItem item) {
    this.items.add(item);
    notifyDataSetChanged();
  }

  public void addAll(List<PlexItem> items) {
    this.items.addAll(items);
    notifyDataSetChanged();
  }

  public void set(List<PlexItem> items) {
    this.items = items;
    notifyDataSetChanged();
  }

  public List<PlexItem> getItems() {
    return items;
  }

  public interface OnPlexItemClickListener {
    void onPlexItemClicked(PlexItem plexItem, int position);
  }
}
