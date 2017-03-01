package net.simno.klingar.playback;

import net.simno.klingar.util.Rx;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Flowable;

@Module
public class PlaybackModule {

  @Provides Flowable<Long> provideSeconds() {
    return Flowable.interval(1, TimeUnit.SECONDS);
  }

  @Provides Random provideRandom() {
    return new Random();
  }

  @Provides @Singleton MusicController provideMusicController(Flowable<Long> seconds, Rx rx) {
    return new MusicController(seconds, rx);
  }

  @Provides @Singleton QueueManager provideQueueManager(Random random) {
    return new QueueManager(random);
  }
}
