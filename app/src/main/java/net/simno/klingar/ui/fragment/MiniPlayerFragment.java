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
package net.simno.klingar.ui.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.ImageButton;
import android.widget.TextView;

import net.simno.klingar.R;
import net.simno.klingar.ui.activity.BrowserActivity;
import net.simno.klingar.ui.activity.PlayerActivity;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.OnClick;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

public class MiniPlayerFragment extends BaseFragment {

  @BindView(R.id.track) TextView track;
  @BindView(R.id.artist) TextView artist;
  @BindView(R.id.play_pause_button) ImageButton playPause;
  @BindDrawable(R.drawable.ic_play_circle_outline_white_36dp) Drawable playDrawable;
  @BindDrawable(R.drawable.ic_pause_circle_outline_white_36dp) Drawable pauseDrawable;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
    @Override
    public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
      MiniPlayerFragment.this.onPlaybackStateChanged(state);
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
      if (metadata == null) {
        return;
      }
      MiniPlayerFragment.this.onMetadataChanged(metadata);
    }
  };

  @Override
  int getLayoutResource() {
    return R.layout.fragment_mini_player;
  }

  @Override
  public void onStart() {
    super.onStart();
    if (getActivity().getSupportMediaController() != null) {
      onConnected();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (getActivity().getSupportMediaController() != null) {
      getActivity().getSupportMediaController().unregisterCallback(callback);
    }
  }

  public void onConnected() {
    MediaControllerCompat controller = getActivity().getSupportMediaController();
    if (controller != null) {
      onMetadataChanged(controller.getMetadata());
      onPlaybackStateChanged(controller.getPlaybackState());
      controller.registerCallback(callback);
    }
  }

  @OnClick(R.id.mini_player)
  public void onMiniPlayerClicked() {
    Intent intent = new Intent(getActivity(), PlayerActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    MediaMetadataCompat metadata = getActivity().getSupportMediaController().getMetadata();
    if (metadata != null) {
      intent.putExtra(BrowserActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, metadata.getDescription());
    }
    startActivity(intent);
  }

  @OnClick(R.id.play_pause_button)
  public void onPlayPauseClicked() {
    PlaybackStateCompat stateObject = getActivity().getSupportMediaController().getPlaybackState();
    int state = stateObject == null ? STATE_NONE : stateObject.getState();

    if (state == STATE_PAUSED || state == STATE_STOPPED || state == STATE_NONE) {
      playMedia();
    } else if (state == STATE_PLAYING || state == STATE_BUFFERING || state == STATE_CONNECTING) {
      pauseMedia();
    }
  }

  private void onPlaybackStateChanged(PlaybackStateCompat state) {
    if (getActivity() == null || state == null) { // TODO null check?
      return;
    }

    boolean enablePlay = false;
    if (state.getState() == STATE_PAUSED || state.getState() == STATE_STOPPED) {
      enablePlay = true;
    }
    playPause.setImageDrawable(enablePlay ? playDrawable : pauseDrawable);
  }

  private void onMetadataChanged(MediaMetadataCompat metadata) {
    if (getActivity() == null || metadata == null) { // TODO null check?
      return;
    }
    MediaDescriptionCompat description = metadata.getDescription();
    track.setText(description.getTitle());
    artist.setText(description.getSubtitle());
  }

  private void playMedia() {
    MediaControllerCompat controller = getActivity().getSupportMediaController();
    if (controller != null) {
      controller.getTransportControls().play();
    }
  }

  private void pauseMedia() {
    MediaControllerCompat controller = getActivity().getSupportMediaController();
    if (controller != null) {
      controller.getTransportControls().pause();
    }
  }
}
