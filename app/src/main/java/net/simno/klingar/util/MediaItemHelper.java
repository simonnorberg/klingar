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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import net.simno.klingar.data.Extra;
import net.simno.klingar.data.Type;

public final class MediaItemHelper {

  private MediaItemHelper() {
    // no instances
  }

  public static MediaItem createMediaItem(@NonNull String id, @NonNull String title,
                                          @Nullable Bundle extras, int flag) {
    return new MediaItem(new MediaDescriptionCompat.Builder()
        .setMediaId(id)
        .setTitle(title)
        .setExtras(extras)
        .build(), flag);
  }

  public static MediaItem createHeaderMediaItem(@NonNull String title) {
    Bundle extras = new Bundle();
    extras.putInt(Extra.INT_TYPE, Type.HEADER);
    return createMediaItem(title, title, extras, 0);
  }

  public static int getViewType(@NonNull MediaItem mediaItem) {
    MediaDescriptionCompat description = mediaItem.getDescription();
    if (description.getExtras() != null) {
      return description.getExtras().getInt(Extra.INT_TYPE, 0);
    }
    return 0;
  }
}
