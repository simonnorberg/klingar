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
package net.simno.klingar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import net.simno.klingar.data.model.Track;
import net.simno.klingar.playback.MusicController;
import net.simno.klingar.playback.MusicService;
import net.simno.klingar.playback.QueueManager;
import net.simno.klingar.ui.KlingarActivity;
import net.simno.klingar.util.Pair;
import net.simno.klingar.util.Rx;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

public class MediaNotificationManager extends BroadcastReceiver {

  private static final String ACTION_PLAY = "net.simno.klingar.ACTION_PLAY";
  private static final String ACTION_PAUSE = "net.simno.klingar.ACTION_PAUSE";
  private static final String ACTION_NEXT = "net.simno.klingar.ACTION_NEXT";
  private static final String ACTION_PREVIOUS = "net.simno.klingar.ACTION_PREVIOUS";
  private static final String ACTION_STOP_CAST = "net.simno.klingar.ACTION_STOP_CAST";
  private static final String CHANNEL_ID = "net.simno.klingar.CHANNEL_ID";
  private static final int NOTIFICATION_ID = 412;
  private static final int REQUEST_CODE = 100;

  private final MusicService service;
  private final MusicController musicController;
  private final QueueManager queueManager;
  private final Rx rx;
  private final NotificationManager notificationManager;
  private final PendingIntent playIntent;
  private final PendingIntent pauseIntent;
  private final PendingIntent nextIntent;
  private final PendingIntent previousIntent;
  private final PendingIntent stopCastIntent;
  private final int notificationColor;
  private final int iconWidth;
  private final int iconHeight;
  @State private int state;
  private Track currentTrack;
  private Disposable disposable;
  private boolean started;

  public MediaNotificationManager(MusicService service, MusicController musicController,
                                  QueueManager queueManager, Rx rx) {
    this.service = service;
    this.musicController = musicController;
    this.queueManager = queueManager;
    this.rx = rx;

    notificationColor = ContextCompat.getColor(service, R.color.primary);
    notificationManager = (NotificationManager) service.getSystemService(
        Context.NOTIFICATION_SERVICE);

    String p = service.getPackageName();
    playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_PLAY).setPackage(p), FLAG_CANCEL_CURRENT);
    pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_PAUSE).setPackage(p), FLAG_CANCEL_CURRENT);
    nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_NEXT).setPackage(p), FLAG_CANCEL_CURRENT);
    previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_PREVIOUS).setPackage(p), FLAG_CANCEL_CURRENT);
    stopCastIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
        new Intent(ACTION_STOP_CAST).setPackage(p), FLAG_CANCEL_CURRENT);

    iconWidth = service.getResources().getDimensionPixelSize(
        android.R.dimen.notification_large_icon_width);
    iconHeight = service.getResources().getDimensionPixelSize(
        android.R.dimen.notification_large_icon_height);

    // Cancel all notifications to handle the case where the Service was killed and
    // restarted by the system.
    if (notificationManager != null) {
      notificationManager.cancelAll();
    }
  }

  /**
   * Posts the notification and starts tracking the session to keep it
   * updated. The notification will automatically be removed if the session is
   * destroyed before {@link #stopNotification} is called.
   */
  public void startNotification() {
    Timber.d("startNotification started %s", started);
    if (!started) {
      currentTrack = queueManager.currentTrack();

      // The notification must be updated after setting started to true
      Notification notification = createNotification();
      if (notification != null) {
        observeSession();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_STOP_CAST);
        service.registerReceiver(this, filter);
        service.startForeground(NOTIFICATION_ID, notification);
        started = true;
      }
    }
  }

  private void observeSession() {
    Rx.dispose(disposable);
    disposable = Flowable.combineLatest(queueManager.queue(), musicController.state(),
        (pair, state) -> {
          boolean stopNotification = false; // higher priority if both are true
          boolean showNotification = false;

          Track track = pair.first.get(pair.second);
          if (!track.equals(currentTrack)) {
            currentTrack = track;
            showNotification = true;
          }

          boolean stateChanged = MediaNotificationManager.this.state != state;
          MediaNotificationManager.this.state = state;
          if (state == STATE_STOPPED || state == STATE_NONE) {
            stopNotification = true;
          } else if (stateChanged) {
            showNotification = true;
          }

          return new Pair<>(stopNotification, showNotification);
        })
        .compose(rx.flowableSchedulers())
        .subscribe(pair -> {
          if (pair.first) {
            stopNotification();
          } else if (pair.second) {
            Notification notification = createNotification();
            if (notification != null) {
              notificationManager.notify(NOTIFICATION_ID, notification);
            }
          }
        }, Rx::onError);
  }

  /**
   * Removes the notification and stops tracking the session. If the session
   * was destroyed this has no effect.
   */
  public void stopNotification() {
    if (started) {
      started = false;
      Rx.dispose(disposable);
      try {
        notificationManager.cancel(NOTIFICATION_ID);
        service.unregisterReceiver(this);
      } catch (IllegalArgumentException ignored) {
        // ignore if the receiver is not registered.
      }
      service.stopForeground(true);
    }
  }

  @Override public void onReceive(Context context, Intent intent) {
    if (intent.getAction() == null) {
      return;
    }
    switch (intent.getAction()) {
      case ACTION_PAUSE:
      case ACTION_PLAY:
        musicController.playPause();
        break;
      case ACTION_NEXT:
        musicController.next();
        break;
      case ACTION_PREVIOUS:
        musicController.previous();
        break;
      case ACTION_STOP_CAST:
        Intent stopCastIntent = new Intent(context, MusicService.class);
        stopCastIntent.setAction(MusicService.ACTION_STOP_CASTING);
        ContextCompat.startForegroundService(service, stopCastIntent);
        break;
      default:
    }
  }

  private Notification createNotification() {
    Timber.d("createNotification currentTrack %s", currentTrack);
    if (currentTrack == null) {
      return null;
    }

    // Notification channels are only supported on Android O+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel();
    }

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(service,
        CHANNEL_ID);

    notificationBuilder.addAction(R.drawable.ic_notification_previous,
        service.getString(R.string.label_previous), previousIntent);

    addPlayPauseAction(notificationBuilder);

    notificationBuilder.addAction(R.drawable.ic_notification_next,
        service.getString(R.string.label_next), nextIntent);

    notificationBuilder
        .setStyle(new MediaStyle()
            .setShowActionsInCompactView(1) // show only play/pause in compact view
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopCastIntent)
            .setMediaSession(musicController.getSessionToken()))
        .setDeleteIntent(stopCastIntent)
        .setColor(notificationColor)
        .setSmallIcon(R.drawable.ic_notification)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .setContentIntent(createContentIntent())
        .setContentTitle(currentTrack.title())
        .setContentText(currentTrack.artistTitle());

    String castName = musicController.getCastName();
    if (castName != null) {
      String castInfo = service.getResources().getString(R.string.casting_to_device, castName);
      notificationBuilder.setSubText(castInfo);
      notificationBuilder.addAction(R.drawable.ic_notification_close,
          service.getString(R.string.stop_casting), stopCastIntent);
    }

    setNotificationPlaybackState(notificationBuilder);

    if (currentTrack.thumb() != null) {
      loadImage(currentTrack.thumb(), notificationBuilder);
    }

    return notificationBuilder.build();
  }

  private void addPlayPauseAction(NotificationCompat.Builder builder) {
    if (state == PlaybackStateCompat.STATE_PLAYING) {
      builder.addAction(new NotificationCompat.Action(R.drawable.ic_notification_pause,
          service.getString(R.string.label_pause), pauseIntent));
    } else {
      builder.addAction(new NotificationCompat.Action(R.drawable.ic_notification_play,
          service.getString(R.string.label_play), playIntent));
    }
  }

  private PendingIntent createContentIntent() {
    Intent intent = new Intent(service, KlingarActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return PendingIntent.getActivity(service, REQUEST_CODE, intent, FLAG_CANCEL_CURRENT);
  }

  private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
    PlaybackStateCompat playbackState = musicController.getPlaybackState();
    if (playbackState == null || !started) {
      service.stopForeground(true);
      return;
    }
    if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING
        && playbackState.getPosition() >= 0) {
      builder.setWhen(System.currentTimeMillis() - playbackState.getPosition())
          .setShowWhen(true)
          .setUsesChronometer(true);
    } else {
      builder.setWhen(0)
          .setShowWhen(false)
          .setUsesChronometer(false);
    }
    // Make sure that the notification can be dismissed by the user when we are not playing:
    builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
  }

  private void loadImage(final String url, final NotificationCompat.Builder builder) {
    Glide.with(service)
        .asBitmap()
        .load(url)
        .apply(RequestOptions.overrideOf(iconWidth, iconHeight))
        .into(new CustomTarget<Bitmap>() {
          @Override
          public void onResourceReady(
              @NonNull Bitmap resource,
              Transition<? super Bitmap> transition
          ) {
            if (TextUtils.equals(currentTrack.thumb(), url)) {
              builder.setLargeIcon(resource);
              notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
          }

          @Override public void onLoadCleared(@Nullable Drawable placeholder) {
          }
        });
  }

  /**
   * Creates Notification Channel. This is required in Android O+ to display notifications.
   */
  @RequiresApi(Build.VERSION_CODES.O)
  private void createNotificationChannel() {
    if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
          service.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
      channel.setDescription(service.getString(R.string.notification_channel_description));
      notificationManager.createNotificationChannel(channel);
    }
  }
}
