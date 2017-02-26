/*
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
package net.simno.klingar.util;

import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ViewsTest {

  @Mock View mockView;

  @Test public void setViewVisibilityVisible() {
    Views.visible(mockView);
    verify(mockView, times(1)).setVisibility(View.VISIBLE);
  }

  @Test public void setViewVisibilityInvisible() {
    Views.invisible(mockView);
    verify(mockView, times(1)).setVisibility(View.INVISIBLE);
  }

  @Test public void setViewVisibilityGone() {
    Views.gone(mockView);
    verify(mockView, times(1)).setVisibility(View.GONE);
  }
}
