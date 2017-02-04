/*
 * Copyright (C) 2014 The Android Open Source Project
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
package net.simno.klingar.playback;

import android.support.v4.media.session.PlaybackStateCompat.State;

import net.simno.klingar.data.model.Track;

/**
 * Interface representing either Local or Remote Playback. The {@link MusicService} works
 * directly with an instance of the Playback object to make the various calls such as
 * play, pause etc.
 */
interface Playback {
  /**
   * Start/setup the playback.
   * Resources/listeners would be allocated by implementations.
   */
  void start();

  /**
   * Stop the playback. All resources can be de-allocated by implementations here.
   *
   * @param notifyListeners if true and a callback has been set by setCallback,
   *                        callback.onPlaybackStatusChanged will be called after changing
   *                        the state.
   */
  void stop(boolean notifyListeners);

  /**
   * Get the current {@link android.media.session.PlaybackState#getState()}
   */
  @State int getState();

  /**
   * @return boolean that indicates that this is ready to be used.
   */
  boolean isConnected();

  /**
   * @return boolean indicating whether the player is playing or is supposed to be
   * playing when we gain audio focus.
   */
  boolean isPlaying();

  /**
   * @return position if currently playing an item
   */
  int getCurrentStreamPosition();

  /**
   * Set the current position. Typically used when switching players that are in paused state.
   *
   * @param position position in the stream
   */
  void setCurrentStreamPosition(int position);

  /**
   * Query the underlying stream and update the internal last known stream position.
   */
  void updateLastKnownStreamPosition();

  /**
   * @param track to play
   */
  void play(Track track);

  /**
   * Pause the current playing track
   */
  void pause();

  /**
   * Seek to the given position
   */
  void seekTo(int position);

  /**
   * @return the current track being processed in any state or null.
   */
  Track getCurrentTrack();

  /**
   * Set the current track. This is only used when switching from one playback to another.
   *
   * @param track to be set as the current.
   */
  void setCurrentTrack(Track track);

  /**
   * @param callback to be called
   */
  void setCallback(Callback callback);

  interface Callback {
    /**
     * On current music completed.
     */
    void onCompletion();

    /**
     * on Playback status changed
     * Implementations can use this callback to update
     * playback state on the media sessions.
     */
    void onPlaybackStatusChanged();

    /**
     * @param remoteTrack being currently played
     */
    void setTrackFromRemote(Track remoteTrack);
  }
}
