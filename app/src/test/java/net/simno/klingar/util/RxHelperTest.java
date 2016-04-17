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
package net.simno.klingar.util;

import net.simno.klingar.data.model.Directory;
import net.simno.klingar.data.model.MediaContainer;
import net.simno.klingar.data.model.Playlist;
import net.simno.klingar.data.model.Track;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subscriptions.CompositeSubscription;

import static net.simno.klingar.util.RxHelper.FLATMAP_DIRS;
import static net.simno.klingar.util.RxHelper.FLATMAP_PLAYLISTS;
import static net.simno.klingar.util.RxHelper.FLATMAP_TRACKS;
import static net.simno.klingar.util.RxHelper.getSubscriptions;
import static net.simno.klingar.util.RxHelper.unsubscribe;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class RxHelperTest {

  @Test
  public void testUnsubscribe() {
    CompositeSubscription cs = new CompositeSubscription();
    assertThat(cs.isUnsubscribed(), is(false));

    unsubscribe(cs);
    assertThat(cs.isUnsubscribed(), is(true));
  }

  @Test
  public void testGetSubscriptions() {
    CompositeSubscription first = getSubscriptions(null);
    assertThat(first, notNullValue());
    assertThat(first.isUnsubscribed(), is(false));

    CompositeSubscription second = getSubscriptions(first);
    assertThat(second, is(sameInstance(first)));

    second.unsubscribe();
    assertThat(second.isUnsubscribed(), is(true));

    CompositeSubscription third = getSubscriptions(second);
    assertThat(third, notNullValue());
    assertThat(third.isUnsubscribed(), is(false));
    assertThat(third, not(sameInstance(second)));
  }

  @Test
  public void testFlatmapDirs() {
    MediaContainer mc = new MediaContainer();
    List<Directory> dirs = new ArrayList<>();
    dirs.add(new Directory());
    dirs.add(new Directory());
    dirs.add(new Directory());
    mc.directories = dirs;

    TestSubscriber<Directory> testSubscriber = new TestSubscriber<>();
    Observable.just(mc).flatMap(FLATMAP_DIRS).subscribe(testSubscriber);

    testSubscriber.assertNoErrors();
    testSubscriber.assertReceivedOnNext(dirs);
  }

  @Test
  public void testFlatmapTracks() {
    MediaContainer mc = new MediaContainer();
    List<Track> tracks = new ArrayList<>();
    tracks.add(new Track());
    tracks.add(new Track());
    tracks.add(new Track());
    mc.tracks = tracks;

    TestSubscriber<Track> testSubscriber = new TestSubscriber<>();
    Observable.just(mc).flatMap(FLATMAP_TRACKS).subscribe(testSubscriber);

    testSubscriber.assertNoErrors();
    testSubscriber.assertReceivedOnNext(tracks);
  }

  @Test
  public void testFlatmapPlaylists() {
    MediaContainer mc = new MediaContainer();
    List<Playlist> playlists = new ArrayList<>();
    playlists.add(new Playlist());
    playlists.add(new Playlist());
    playlists.add(new Playlist());
    mc.playlists = playlists;

    TestSubscriber<Playlist> testSubscriber = new TestSubscriber<>();
    Observable.just(mc).flatMap(FLATMAP_PLAYLISTS).subscribe(testSubscriber);

    testSubscriber.assertNoErrors();
    testSubscriber.assertReceivedOnNext(playlists);
  }
}
