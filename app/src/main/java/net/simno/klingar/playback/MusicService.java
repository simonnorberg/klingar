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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;

import net.simno.klingar.AndroidClock;
import net.simno.klingar.KlingarApp;
import net.simno.klingar.MediaNotificationManager;
import net.simno.klingar.data.api.MediaService;
import net.simno.klingar.ui.KlingarActivity;
import net.simno.klingar.util.Rx;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.OkHttpClient;
import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class MusicService extends Service implements PlaybackManager.PlaybackServiceCallback {

  public static final String ACTION_STOP_CASTING = "net.simno.klingar.ACTION_STOP_CASTING";

  private static final int STOP_DELAY = 30000;
  private final IBinder binder = new LocalBinder();
  private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
  @Inject QueueManager queueManager;
  @Inject MusicController musicController;
  @Inject AudioManager audioManager;
  @Inject WifiManager wifiManager;
  @Inject MediaService media;
  @Inject Rx rx;
  @Inject @Named("default") OkHttpClient client;
  private PlaybackManager playbackManager;
  private MediaSessionCompat session;
  private MediaNotificationManager mediaNotificationManager;
  private MediaRouter mediaRouter;
  private SessionManager castSessionManager;
  private SessionManagerListener<CastSession> castSessionManagerListener;
  private TimelineManager timelineManager;

  @Nullable @Override public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override public void onCreate() {
    super.onCreate();
    Timber.d("onCreate");
    KlingarApp.get(this).component().inject(this);

    Playback playback = new LocalPlayback(getApplicationContext(), musicController, audioManager,
        wifiManager, client);
    playbackManager = new PlaybackManager(queueManager, this, AndroidClock.DEFAULT, playback);

    session = new MediaSessionCompat(this, "MusicService");

    try {
      MediaControllerCompat mediaController =
          new MediaControllerCompat(this.getApplicationContext(), session.getSessionToken());
      musicController.setMediaController(mediaController);
    } catch (RemoteException e) {
      Timber.e(e, "Could not create MediaController");
      throw new IllegalStateException();
    }

    session.setCallback(playbackManager.getMediaSessionCallback());

    Context context = getApplicationContext();
    Intent intent = new Intent(context, KlingarActivity.class);
    session.setSessionActivity(PendingIntent.getActivity(context, 99, intent, FLAG_UPDATE_CURRENT));

    playbackManager.updatePlaybackState();

    mediaNotificationManager = new MediaNotificationManager(this, musicController,
        queueManager, rx);

    castSessionManager = CastContext.getSharedInstance(this).getSessionManager();
    castSessionManagerListener = new CastSessionManagerListener();
    castSessionManager.addSessionManagerListener(castSessionManagerListener, CastSession.class);

    mediaRouter = MediaRouter.getInstance(getApplicationContext());

    timelineManager = new TimelineManager(musicController, queueManager, media, rx);
    timelineManager.start();
  }

  @Override public int onStartCommand(Intent startIntent, int flags, int startId) {
    if (startIntent != null) {
      if (ACTION_STOP_CASTING.equals(startIntent.getAction())) {
        CastContext.getSharedInstance(this).getSessionManager().endCurrentSession(true);
      } else {
        // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
        MediaButtonReceiver.handleIntent(session, startIntent);
      }
    }
    // Reset the delay handler to enqueue a message to stop the service if nothing is playing
    delayedStopHandler.removeCallbacksAndMessages(null);
    delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    return START_STICKY;
  }

  @Override public void onDestroy() {
    Timber.d("onDestroy");
    // Service is being killed, so make sure we release our resources
    playbackManager.handleStopRequest();
    mediaNotificationManager.stopNotification();

    if (castSessionManager != null) {
      castSessionManager.removeSessionManagerListener(castSessionManagerListener,
          CastSession.class);
    }

    timelineManager.stop();

    delayedStopHandler.removeCallbacksAndMessages(null);
    session.release();
  }

  @Override public void onPlaybackStart() {
    session.setActive(true);

    delayedStopHandler.removeCallbacksAndMessages(null);

    // The service needs to continue running even after the bound client (usually a
    // MediaController) disconnects, otherwise the music playback will stop.
    // Calling startService(Intent) will keep the service running until it is explicitly killed.
    ContextCompat.startForegroundService(getApplicationContext(),
        new Intent(getApplicationContext(), MusicService.class));
  }

  @Override public void onPlaybackStop() {
    session.setActive(false);
    // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
    // potentially stopping the service.
    delayedStopHandler.removeCallbacksAndMessages(null);
    delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    stopForeground(true);
  }

  @Override public void onNotificationRequired() {
    mediaNotificationManager.startNotification();
  }

  @Override public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
    session.setPlaybackState(newState);
  }

  /**
   * A simple handler that stops the service if playback is not active (playing)
   */
  private static class DelayedStopHandler extends Handler {
    private final WeakReference<MusicService> weakReference;

    private DelayedStopHandler(MusicService service) {
      weakReference = new WeakReference<>(service);
    }

    @Override public void handleMessage(@NonNull Message msg) {
      MusicService service = weakReference.get();
      if (service != null && service.playbackManager.getPlayback() != null) {
        if (!service.playbackManager.getPlayback().isPlaying()) {
          service.stopSelf();
        }
      }
    }
  }

  private static class LocalBinder extends Binder {
  }

  /**
   * Session Manager Listener responsible for switching the Playback instances
   * depending on whether it is connected to a remote player.
   */
  private class CastSessionManagerListener implements SessionManagerListener<CastSession> {
    @Override public void onSessionEnded(CastSession castSession, int error) {
      Timber.d("onSessionEnded");
      musicController.setCastName(null);
      Playback playback = new LocalPlayback(getApplicationContext(), musicController, audioManager,
          wifiManager, client);
      mediaRouter.setMediaSessionCompat(null);
      playbackManager.switchToPlayback(playback, false);
    }

    @Override public void onSessionResumed(CastSession session, boolean wasSuspended) {
    }

    @Override public void onSessionStarted(CastSession castSession, String sessionId) {
      Timber.d("onSessionStarted %s", sessionId);
      musicController.setCastName(castSession.getCastDevice().getFriendlyName());
      Playback playback = new CastPlayback(MusicService.this);
      mediaRouter.setMediaSessionCompat(session);
      playbackManager.switchToPlayback(playback, true);
    }

    @Override public void onSessionStarting(CastSession session) {
    }

    @Override public void onSessionStartFailed(CastSession session, int error) {
    }

    @Override public void onSessionEnding(CastSession session) {
      // This is our final chance to update the underlying stream position
      // In onSessionEnded(), the underlying CastPlayback#mRemoteMediaClient
      // is disconnected and hence we update our local value of stream position
      // to the latest position.
      playbackManager.getPlayback().updateLastKnownStreamPosition();
    }

    @Override public void onSessionResuming(CastSession session, String sessionId) {
    }

    @Override public void onSessionResumeFailed(CastSession session, int error) {
    }

    @Override public void onSessionSuspended(CastSession session, int reason) {
    }
  }
}
