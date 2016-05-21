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
package net.simno.klingar.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.R;
import net.simno.klingar.data.LoginManager;
import net.simno.klingar.data.ServerManager;
import net.simno.klingar.data.Type;
import net.simno.klingar.service.MusicService;
import net.simno.klingar.service.MyPlexServerFinder;
import net.simno.klingar.ui.ToolbarOwner;
import net.simno.klingar.ui.callback.MediaControllerListener;
import net.simno.klingar.ui.callback.MediaItemListener;
import net.simno.klingar.ui.callback.SettingsListener;
import net.simno.klingar.ui.fragment.BrowserFragment;
import net.simno.klingar.ui.fragment.DetailFragment;
import net.simno.klingar.ui.fragment.LicensesFragment;
import net.simno.klingar.ui.fragment.MiniPlayerFragment;
import net.simno.klingar.ui.fragment.SettingsFragment;
import net.simno.klingar.util.Network;
import net.simno.klingar.util.RxHelper;
import net.simno.klingar.util.SimpleSubscriber;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static net.simno.klingar.util.MediaItemHelper.getViewType;

public class BrowserActivity extends CastBaseActivity implements ToolbarOwner.Activity,
    MediaItemListener, NavigationView.OnNavigationItemSelectedListener, SettingsListener {

  public static final String EXTRA_START_FULLSCREEN = "net.simno.klingar.EXTRA_START_FULLSCREEN";
  public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "net.simno.klingar.EXTRA_CURRENT_MEDIA_DESCRIPTION";

  private static final String STATE_CURRENT_DRAWER_ITEM = "state_current_drawer_item";
  private static final String STATE_LIBS = "state_libs";

  @BindView(R.id.container) FrameLayout container;
  @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
  @BindView(R.id.navigation) NavigationView navigation;
  @Nullable @BindView(R.id.mini_player_container) View miniPlayerContainer;

  @Inject ToolbarOwner toolbarOwner;
  @Inject ServerManager serverManager;
  @Inject Network network;
  @Inject LoginManager loginManager;

  private MediaBrowserCompat mediaBrowser;
  private MiniPlayerFragment miniPlayer;
  private ArrayList<MediaItem> libs;
  private int currentDrawerItem;

  private final MediaControllerCompat.Callback mediaControllerCallback =
      new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
          if (shouldShowMiniPlayer()) {
            showMiniPlayer();
          } else {
            hideMiniPlayer();
          }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
          if (shouldShowMiniPlayer()) {
            showMiniPlayer();
          } else {
            hideMiniPlayer();
          }
        }
      };

  private final MediaBrowserCompat.ConnectionCallback connectionCallback =
      new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
          try {
            connectToSession(mediaBrowser.getSessionToken());
          } catch (RemoteException e) {
            Timber.e(e, "Could not connect media controller");
          }
        }
      };

  public static void newIntent(Context context) {
    context.startActivity(new Intent(context, BrowserActivity.class));
  }

  @Override
  void injectDependencies() {
    KlingarApp.get(this).component().inject(this);
  }

  @Override
  int getLayoutResource() {
    return R.layout.activity_browser;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    startPlayerActivityIfNeeded(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    toolbarOwner.setActivity(this);

    mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class),
        connectionCallback, null);

    if (savedInstanceState == null) {
      MyPlexServerFinder.start(this);
      startPlayerActivityIfNeeded(getIntent());
    } else {
      currentDrawerItem = savedInstanceState.getInt(STATE_CURRENT_DRAWER_ITEM);
      libs = savedInstanceState.getParcelableArrayList(STATE_LIBS);
    }

    navigation.setNavigationItemSelectedListener(this);
  }

  private void startPlayerActivityIfNeeded(Intent intent) {
    if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
      MediaDescriptionCompat mediaDescription = intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION);
      Intent fullScreenIntent = new Intent(this, PlayerActivity.class)
          .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
          .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION, mediaDescription);
      startActivity(fullScreenIntent);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    miniPlayer = (MiniPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_mini_player);
    hideMiniPlayer();

    mediaBrowser.connect();
  }

  @Override
  protected void onResume() {
    super.onResume();
    observeLibs();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_CURRENT_DRAWER_ITEM, currentDrawerItem);
    outState.putParcelableArrayList(STATE_LIBS, libs);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (getSupportMediaController() != null) {
      getSupportMediaController().unregisterCallback(mediaControllerCallback);
    }
    mediaBrowser.disconnect();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    toolbarOwner.setActivity(null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      if (isDrawerOpen()) {
        closeDrawer();
      } else {
        openDrawer();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    if (isDrawerOpen()) {
      closeDrawer();
      return;
    }
    if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
      getSupportFragmentManager().popBackStack();
      return;
    }
    super.onBackPressed();
  }

  @Override
  public void setToolbarTitle(CharSequence title) {
    setTitle(title);
  }

  @Override
  public void setToolbarTitle(int resId) {
    setTitle(resId);
  }

  @Override
  public void setToolbarTitleColor(int color) {
    toolbar.setTitleTextColor(color);
  }

  @Override
  public void setToolbarBackgroundColor(int color) {
    toolbar.setBackgroundColor(color);
  }

  @Override
  public MediaBrowserCompat getMediaBrowser() {
    return mediaBrowser;
  }

  @Override
  public void onMediaItemSelected(MediaItem mediaItem) {
    switch (getViewType(mediaItem)) {
      case Type.MEDIA_TYPE:
        replaceFragment(BrowserFragment.newInstance(mediaItem), false);
        break;
      case Type.PLAYLIST:
      case Type.ARTIST:
      case Type.ALBUM:
        replaceFragment(DetailFragment.newInstance(mediaItem), false);
        break;
      case Type.TRACK:
        getSupportMediaController().getTransportControls().playFromMediaId(mediaItem.getMediaId(), null);
        break;
    }
  }

  private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
    MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
    setSupportMediaController(mediaController);
    mediaController.registerCallback(mediaControllerCallback);

    if (shouldShowMiniPlayer()) {
      showMiniPlayer();
    } else {
      hideMiniPlayer();
    }

    if (miniPlayer != null) {
      miniPlayer.onConnected();
    }

    onMediaControllerConnected();
  }

  private void onMediaControllerConnected() {
    Fragment currentFragment = getCurrentFragment();
    if (currentFragment instanceof MediaControllerListener) {
      ((MediaControllerListener) currentFragment).onConnected();
    }
  }

  private void showMiniPlayer() {
    if (network.isConnected() && miniPlayerContainer != null) {
      miniPlayerContainer.setVisibility(View.VISIBLE);
    }
  }

  private void hideMiniPlayer() {
    if (miniPlayerContainer != null) {
      miniPlayerContainer.setVisibility(View.INVISIBLE);
    }
  }

  private boolean shouldShowMiniPlayer() {
    MediaControllerCompat mediaController = getSupportMediaController();
    if (mediaController == null ||
        mediaController.getMetadata() == null ||
        mediaController.getPlaybackState() == null) {
      return false;
    }
    int state = mediaController.getPlaybackState().getState();
    switch (state) {
      case PlaybackStateCompat.STATE_ERROR:
      case PlaybackStateCompat.STATE_NONE:
      case PlaybackStateCompat.STATE_STOPPED:
        return false;
      default:
        return true;
    }
  }

  private boolean isDrawerOpen() {
    return drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START);
  }

  private void openDrawer() {
    if (drawerLayout != null) {
      drawerLayout.openDrawer(GravityCompat.START);
    }
  }

  private void closeDrawer() {
    if (drawerLayout != null) {
      drawerLayout.closeDrawers();
    }
  }

  private void replaceFragment(Fragment fragment, boolean isTopLevel) {
    if (isTopLevel) {
      getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.container, fragment, null);
    if (!isTopLevel) {
      transaction.addToBackStack(null);
    }
    transaction.commit();
  }

  private Fragment getCurrentFragment() {
    return getSupportFragmentManager().findFragmentById(R.id.container);
  }

  private void observeLibs() {
    subscriptions.add(serverManager.libs()
        .compose(bindToLifecycle())
        .compose(RxHelper.applySchedulers())
        .subscribe(new SimpleSubscriber<ArrayList<MediaItem>>() {
          @Override
          public void onNext(ArrayList<MediaItem> libs) {
            BrowserActivity.this.libs = libs;
            updateMenu();
            if (getCurrentFragment() != null) {
              setDrawerItemChecked(currentDrawerItem);
            } else {
              if (!libs.isEmpty()) {
                setDrawerItemChecked(0);
                browseLibrary(0);
              }
            }
          }
        }));
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId != currentDrawerItem) {
      setDrawerItemChecked(itemId);
      browseLibrary(itemId);
    }
    closeDrawer();
    return true;
  }

  private void updateMenu() {
    Menu menu = navigation.getMenu();
    menu.clear();
    for (int i = 0; i < libs.size(); ++i) {
      menu.add(Menu.NONE, i, i, libs.get(i).getDescription().getTitle());
    }
  }

  private void setDrawerItemChecked(int checkId) {
    currentDrawerItem = checkId;
    Menu menu = navigation.getMenu();
    for (int i = 0; i < libs.size(); ++i) {
      MenuItem foundItem = menu.findItem(i);
      if (foundItem != null) {
        foundItem.setChecked(i == checkId);
      }
    }
  }

  private void browseLibrary(int position) {
    MediaItem mediaItem = libs.get(position);
    if (mediaItem != null) {
      replaceFragment(BrowserFragment.newInstance(mediaItem), true);
    }
  }

  @OnClick(R.id.navigation_settings)
  void onClickSettings() {
    if (!(getCurrentFragment() instanceof SettingsFragment)) {
      setDrawerItemChecked(-1);
      replaceFragment(SettingsFragment.newInstance(), false);
    }
    closeDrawer();
  }

  @Override
  public void signOut() {
    getSupportMediaController().getTransportControls().stop();
    loginManager.logout();
    LoginActivity.newIntent(this);
    finish();
  }

  @Override
  public void showLicenses() {
    replaceFragment(LicensesFragment.newInstance(), false);
  }

  @Override
  public void showRepo() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse("https://github.com/simonnorberg/klingar"));
    if (isIntentAvailable(intent)) {
      startActivity(intent);
    }
  }
}
