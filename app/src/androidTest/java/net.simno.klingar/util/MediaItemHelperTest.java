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

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import net.simno.klingar.data.Extra;
import net.simno.klingar.data.Type;

import org.junit.Test;
import org.junit.runner.RunWith;

import static net.simno.klingar.util.MediaItemHelper.createHeaderMediaItem;
import static net.simno.klingar.util.MediaItemHelper.createMediaItem;
import static net.simno.klingar.util.MediaItemHelper.getViewType;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
@RunWith(AndroidJUnit4.class)
public class MediaItemHelperTest {

  @Test
  public void testCreateMediaItem() {
    MediaItem mediaItem = createMediaItem("mediaId", "title", new Bundle(), MediaItem.FLAG_BROWSABLE);

    assertThat(mediaItem.getMediaId(), is((CharSequence) "mediaId"));
    assertThat(mediaItem.getDescription().getTitle(), is((CharSequence) "title"));
    assertThat(mediaItem.isBrowsable(), is(true));
    assertThat(mediaItem.isPlayable(), is(false));
  }

  @Test
  public void testCreateHeaderMediaItem() {
    MediaItem mediaItem = createHeaderMediaItem("title");
    MediaDescriptionCompat description = mediaItem.getDescription();
    Bundle extras = description.getExtras();

    assertThat(extras, is(notNullValue()));
    assertThat(description.getTitle(), is((CharSequence) "title"));
    assertThat(extras.getInt(Extra.INT_TYPE), is(Type.HEADER));
    assertThat(mediaItem.isBrowsable(), is(false));
    assertThat(mediaItem.isPlayable(), is(false));
  }

  @Test
  public void testGetViewType() {
    int viewType = Type.HEADER;
    Bundle extras = new Bundle();
    extras.putInt(Extra.INT_TYPE, viewType);
    MediaItem mediaItem = createMediaItem("mediaId", "title", extras, 0);
    int getViewType = getViewType(mediaItem);

    assertThat(getViewType, is(viewType));
  }
}
