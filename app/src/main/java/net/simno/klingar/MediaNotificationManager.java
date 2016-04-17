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
package net.simno.klingar;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import net.simno.klingar.service.MusicService;
import net.simno.klingar.ui.activity.BrowserActivity;

import timber.log.Timber;

public class MediaNotificationManager extends BroadcastReceiver {

  private static final String ACTION_NEXT = "net.simno.klingar.next";
  private static final String ACTION_PAUSE = "net.simno.klingar.pause";
  private static final String ACTION_PLAY = "net.simno.klingar.play";
  private static final String ACTION_PREV = "net.simno.klingar.prev";
  private static final String ACTION_STOP_CASTING = "net.simno.klingar.stop_cast";

  private static final int NOTIFICATION_ID = 1337;
  private static final int REQUEST_CODE = 1338;

  private final MusicService service;
  private MediaSessionCompat.Token sessionToken;
  private MediaControllerCompat controller;
  private MediaControllerCompat.TransportControls transportControls;

  private PlaybackStateCompat playbackState;
  private MediaMetadataCompat metadata;

  private final NotificationManagerCompat notificationManager;

  private final PendingIntent pauseIntent;
  private final PendingIntent playIntent;
  private final PendingIntent previousIntent;
  private final PendingIntent nextIntent;
  private final PendingIntent stopCastIntent;

  private final int notificationColor;
  private boolean started = false;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
    @Override
    public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
      playbackState = state;
      if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
          state.getState() == PlaybackStateCompat.STATE_NONE) {
        stopNotification();
      } else {
        Notification notification = createNotification();
        if (notification != null) {
          notificationManager.notify(NOTIFICATION_ID, notification);
        }
      }
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
      MediaNotificationManager.this.metadata = metadata;
      Notification notification = createNotification();
      if (notification != null) {
        notificationManager.notify(NOTIFICATION_ID, notification);
      }
    }

    @Override
    public void onSessionDestroyed() {
      super.onSessionDestroyed();
      try {
        updateSessionToken();
      } catch (RemoteException e) {
        Timber.e(e, "Could not connect media controller");
      }
    }
  };

  public MediaNotificationManager(MusicService service) throws RemoteException {
    KlingarApp.get(service).component().inject(this);

    this.service = service;
    updateSessionToken();

    notificationColor = ContextCompat.getColor(service, R.color.primary);

    notificationManager = NotificationManagerCompat.from(service);

    String pkg = service.getPackageName();
    pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    stopCastIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_STOP_CASTING).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

    // Cancel all notifications to handle the case where the Service was killed and
    // restarted by the system.
    notificationManager.cancelAll();
  }

  /**
   * Update the state based on a change on the session token. Called either when
   * we are running for the first time or when the media session owner has destroyed the session
   * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
   */
  private void updateSessionToken() throws RemoteException {
    MediaSessionCompat.Token freshToken = service.getSessionToken();
    if (sessionToken == null && freshToken != null ||
        sessionToken != null && !sessionToken.equals(freshToken)) {
      if (controller != null) {
        controller.unregisterCallback(callback);
      }
      sessionToken = freshToken;
      if (sessionToken != null) {
        controller = new MediaControllerCompat(service, sessionToken);
        transportControls = controller.getTransportControls();
        if (started) {
          controller.registerCallback(callback);
        }
      }
    }
  }

  private Notification createNotification() {
    if (metadata == null || playbackState == null) {
      return null;
    }

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(service);
    int playPauseButtonPosition = 0;

    // If skip to previous action is enabled
    if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
      notificationBuilder.addAction(R.drawable.ic_skip_previous_white_24dp,
          service.getString(R.string.label_previous), previousIntent);

      // If there is a "skip to previous" button, the play/pause button will
      // be the second one. We need to keep track of it, because the MediaStyle notification
      // requires to specify the index of the buttons (actions) that should be visible
      // when in compact view.
      playPauseButtonPosition = 1;
    }

    addPlayPauseAction(notificationBuilder);

    // If skip to next action is enabled
    if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
      notificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
          service.getString(R.string.label_next), nextIntent);
    }

    MediaDescriptionCompat description = metadata.getDescription();

    NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle()
        .setShowActionsInCompactView(new int[]{playPauseButtonPosition})
        .setMediaSession(sessionToken);

    notificationBuilder
        .setStyle(style)
        .setColor(notificationColor)
        .setSmallIcon(R.drawable.ic_notification_24dp)
        .setLargeIcon(description.getIconBitmap())
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setUsesChronometer(true)
        .setContentIntent(createContentIntent(description))
        .setContentTitle(description.getTitle())
        .setContentText(description.getSubtitle());

    if (controller != null && controller.getExtras() != null) {
      String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
      if (castName != null) {
        String castInfo = service.getResources().getString(R.string.casting_to_device, castName);
        notificationBuilder.setSubText(castInfo);
        notificationBuilder.addAction(R.drawable.ic_close_white_24dp,
            service.getString(R.string.stop_casting), stopCastIntent);
      }
    }

    setNotificationPlaybackState(notificationBuilder);

    return notificationBuilder.build();
  }

  private void addPlayPauseAction(NotificationCompat.Builder builder) {
    String label;
    int icon;
    PendingIntent intent;
    if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
      label = service.getString(R.string.label_pause);
      icon = R.drawable.ic_pause_white_24dp;
      intent = pauseIntent;
    } else {
      label = service.getString(R.string.label_play);
      icon = R.drawable.ic_play_arrow_white_24dp;
      intent = playIntent;
    }
    builder.addAction(new NotificationCompat.Action(icon, label, intent));
  }

  private PendingIntent createContentIntent(MediaDescriptionCompat description) {
    Intent openUI = new Intent(service, BrowserActivity.class);
    openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    openUI.putExtra(BrowserActivity.EXTRA_START_FULLSCREEN, true);
    if (description != null) {
      openUI.putExtra(BrowserActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
    }
    return PendingIntent.getActivity(service, REQUEST_CODE, openUI,
        PendingIntent.FLAG_CANCEL_CURRENT);
  }

  private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
    if (playbackState == null || !started) {
      service.stopForeground(true);
      return;
    }
    if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING &&
        playbackState.getPosition() >= 0) {
      builder
          .setWhen(System.currentTimeMillis() - playbackState.getPosition())
          .setShowWhen(true)
          .setUsesChronometer(true);
    } else {
      builder
          .setWhen(0)
          .setShowWhen(false)
          .setUsesChronometer(false);
    }

    // Make sure that the notification can be dismissed by the user when we are not playing:
    builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
  }

  /**
   * Posts the notification and starts tracking the session to keep it updated.
   * The notification will automatically be removed if the session is destroyed
   * before {@link #stopNotification} is called.
   */
  public void startNotification() {
    if (!started) {
      metadata = controller.getMetadata();
      playbackState = controller.getPlaybackState();

      // The notification must be updated after setting started to true
      Notification notification = createNotification();
      if (notification != null) {
        controller.registerCallback(callback);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_STOP_CASTING);
        service.registerReceiver(this, filter);

        service.startForeground(NOTIFICATION_ID, notification);
        started = true;
      }
    }
  }

  /**
   * Removes the notification and stops tracking the session. If the session
   * was destroyed this has no effect.
   */
  public void stopNotification() {
    if (started) {
      started = false;
      controller.unregisterCallback(callback);
      try {
        notificationManager.cancel(NOTIFICATION_ID);
        service.unregisterReceiver(this);
      } catch (IllegalArgumentException ignored) {
        // ignore if the receiver is not registered.
      }
      service.stopForeground(true);
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    switch (intent.getAction()) {
      case ACTION_PAUSE:
        transportControls.pause();
        break;
      case ACTION_PLAY:
        transportControls.play();
        break;
      case ACTION_NEXT:
        transportControls.skipToNext();
        break;
      case ACTION_PREV:
        transportControls.skipToPrevious();
        break;
      case ACTION_STOP_CASTING:
        Intent i = new Intent(context, MusicService.class);
        i.setAction(MusicService.ACTION_CMD);
        i.putExtra(MusicService.CMD_NAME, MusicService.CMD_STOP_CASTING);
        service.startService(i);
        break;
    }
  }
}
