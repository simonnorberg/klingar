/*
 * Copyright (C) 2014 The Android Open Source Project
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
package net.simno.klingar.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;

import com.bumptech.glide.RequestManager;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.MediaNotificationManager;
import net.simno.klingar.R;
import net.simno.klingar.data.MusicProvider;
import net.simno.klingar.playback.CastPlayback;
import net.simno.klingar.playback.LocalPlayback;
import net.simno.klingar.playback.Playback;
import net.simno.klingar.playback.PlaybackManager;
import net.simno.klingar.playback.QueueManager;
import net.simno.klingar.ui.activity.BrowserActivity;
import net.simno.klingar.util.MediaIdHelper;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class MusicService extends MediaBrowserServiceCompat implements PlaybackManager.PlaybackServiceCallback, QueueManager.MetadataUpdateListener {

  // Extra on MediaSession that contains the Cast device name currently connected to
  public static final String EXTRA_CONNECTED_CAST = "net.simno.klingar.CAST_NAME";

  // The action of the incoming Intent indicating that it contains a command to be executed
  // (see {@link #onStartCommand})
  public static final String ACTION_CMD = "net.simno.klingar.ACTION_CMD";

  // The key in the extras of the incoming Intent indicating the command that should be executed
  // (see {@link #onStartCommand})
  public static final String CMD_NAME = "CMD_NAME";

  // A value of a CMD_NAME key in the extras of the incoming Intent that indicates that the
  // music playback should be paused (see {@link #onStartCommand})
  public static final String CMD_PAUSE = "CMD_PAUSE";

  // A value of a CMD_NAME key that indicates that the music playback should switch
  // to local playback from cast playback.
  public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";

  // Delay stopSelf by using a handler.
  private static final int STOP_DELAY = 30000;

  @Inject RequestManager glide;
  @Inject QueueManager queueManager;
  @Inject MusicProvider musicProvider;

  private CompositeSubscription subscriptions;
  private PlaybackManager playbackManager;
  private MediaSessionCompat session;
  private MediaNotificationManager mediaNotificationManager;
  private Bundle sessionExtras;
  private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
  private MediaRouter mediaRouter;

  /**
   * Consumer responsible for switching the Playback instances depending on whether
   * it is connected to a remote player.
   */
  private final VideoCastConsumerImpl castConsumer = new VideoCastConsumerImpl() {
    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                       boolean wasLaunched) {
      // In case we are casting, send the device name as an extra on MediaSession metadata.
      sessionExtras.putString(EXTRA_CONNECTED_CAST, VideoCastManager.getInstance().getDeviceName());
      session.setExtras(sessionExtras);
      // Now we can switch to CastPlayback
      Playback playback = new CastPlayback(musicProvider);
      mediaRouter.setMediaSessionCompat(session);
      playbackManager.switchToPlayback(playback, true);
    }

    @Override
    public void onDisconnectionReason(int reason) {
      // Last chance to update the underlying stream position.
      playbackManager.getPlayback().updateLastKnownStreamPosition();
    }

    @Override
    public void onDisconnected() {
      sessionExtras.remove(EXTRA_CONNECTED_CAST);
      session.setExtras(sessionExtras);
      Playback playback = new LocalPlayback(MusicService.this);
      mediaRouter.setMediaSessionCompat(null);
      playbackManager.switchToPlayback(playback, false);
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();

    KlingarApp.get(this).component().inject(this);
    subscriptions = RxHelper.getSubscriptions(subscriptions);

    queueManager.setMetadataUpdateListener(this);

    LocalPlayback playback = new LocalPlayback(this);
    playbackManager = new PlaybackManager(this, queueManager, playback);

    // Start a new MediaSession
    session = new MediaSessionCompat(this, "MusicService");
    setSessionToken(session.getSessionToken());
    session.setCallback(playbackManager.getMediaSessionCallback());
    session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

    Context context = getApplicationContext();
    Intent intent = new Intent(context, BrowserActivity.class);
    PendingIntent pi = PendingIntent.getActivity(context, 1339, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    session.setSessionActivity(pi);

    sessionExtras = new Bundle();
    session.setExtras(sessionExtras);

    playbackManager.updatePlaybackState(null);

    try {
      mediaNotificationManager = new MediaNotificationManager(this);
    } catch (RemoteException e) {
      throw new IllegalStateException("Could not create a MediaNotificationManager", e);
    }
    VideoCastManager.getInstance().addVideoCastConsumer(castConsumer);
    mediaRouter = MediaRouter.getInstance(getApplicationContext());
  }

  @Override
  public int onStartCommand(Intent startIntent, int flags, int startId) {
    if (startIntent != null) {
      String action = startIntent.getAction();
      String command = startIntent.getStringExtra(CMD_NAME);
      if (ACTION_CMD.equals(action)) {
        if (CMD_PAUSE.equals(command)) {
          playbackManager.handlePauseRequest();
        } else if (CMD_STOP_CASTING.equals(command)) {
          VideoCastManager.getInstance().disconnect();
        }
      } else {
        // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
        MediaButtonReceiver.handleIntent(session, startIntent);
      }
    }
    // Reset the delay handler to enqueue a message to stop the service if nothing is playing.
    delayedStopHandler.removeCallbacksAndMessages(null);
    delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    RxHelper.unsubscribe(subscriptions);

    // Service is being killed, so make sure we release our resources
    playbackManager.handleStopRequest(null);
    mediaNotificationManager.stopNotification();
    VideoCastManager.getInstance().removeVideoCastConsumer(castConsumer);
    delayedStopHandler.removeCallbacksAndMessages(null);
    session.release();
  }

  @Nullable @Override
  public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
    if (android.os.Process.SYSTEM_UID == clientUid || android.os.Process.myUid() == clientUid) {
      // Always allow this app!
      return new BrowserRoot(MediaIdHelper.MEDIA_ID_ROOT, null);
    }
    return null;
  }

  @Override
  public void onLoadChildren(@NonNull final String parentId,
                             @NonNull final Result<List<MediaItem>> result) {
    result.detach(); // Allow the sendResult call to happen later
    subscriptions.add(musicProvider.media(parentId)
        .subscribeOn(Schedulers.io())
        .subscribe(new SimpleSubscriber<List<MediaItem>>() {
          @Override
          public void onNext(List<MediaItem> mediaItems) {
            queueManager.setMediaItems(mediaItems);
            result.sendResult(mediaItems);
          }
        }));
  }

  /**
   * Callback method called from PlaybackManager whenever the music is about to play.
   */
  @Override
  public void onPlaybackStart() {
    if (!session.isActive()) {
      session.setActive(true);
    }

    delayedStopHandler.removeCallbacksAndMessages(null);

    // The service needs to continue running even after the bound client (usually a
    // MediaController) disconnects, otherwise the music playback will stop.
    // Calling startService(Intent) will keep the service running until it is explicitly killed.
    startService(new Intent(getApplicationContext(), MusicService.class));
  }

  /**
   * Callback method called from PlaybackManager whenever the music stops playing.
   */
  @Override
  public void onPlaybackStop() {
    // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
    // potentially stopping the service.
    delayedStopHandler.removeCallbacksAndMessages(null);
    delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    stopForeground(true);
  }

  @Override
  public void onNotificationRequired() {
    mediaNotificationManager.startNotification();
  }

  @Override
  public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
    session.setPlaybackState(newState);
  }

  @Override
  public void onMetadataChanged(MediaMetadataCompat metadata) {
    session.setMetadata(metadata);
  }

  @Override
  public void onMetadataRetrieveError() {
    playbackManager.updatePlaybackState(getString(R.string.error_no_metadata));
  }

  @Override
  public void onCurrentQueueIndexUpdated(int queueIndex) {
    playbackManager.handlePlayRequest();
  }

  @Override
  public void onQueueUpdated(CharSequence title, List<MediaSessionCompat.QueueItem> newQueue) {
    session.setQueue(newQueue);
    session.setQueueTitle(title);
  }

  /**
   * A simple handler that stops the service if playback is not active (playing)
   */
  private static class DelayedStopHandler extends Handler {
    private final WeakReference<MusicService> weakReference;

    private DelayedStopHandler(MusicService service) {
      weakReference = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
      MusicService service = weakReference.get();
      if (service != null && service.playbackManager.getPlayback() != null) {
        if (service.playbackManager.getPlayback().isPlaying()) {
          Timber.d("Ignoring delayed stop since the media player is in use.");
          return;
        }
        Timber.d("Stopping service with delay handler.");
        service.stopSelf();
      }
    }
  }
}
