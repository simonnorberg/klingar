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
package net.simno.klingar.data;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressLint("CommitPrefEdits")
@RunWith(MockitoJUnitRunner.class)
public class SharedPrefsTest {

  private final TestEditor testEditor = new TestEditor();
  @Mock SharedPreferences mockSharedPreferences;
  private SharedPrefs prefs;

  @Before public void setup() {
    when(mockSharedPreferences.getString(anyString(), anyString())).thenAnswer(mock ->
        testEditor.getPreferences().getOrDefault(mock.getArgument(0), mock.getArgument(1)));
    when(mockSharedPreferences.edit()).thenReturn(testEditor);
    prefs = new SharedPrefs(mockSharedPreferences);
  }

  @Test public void putAndGetString() {
    prefs.putString("testKey", "testValue");
    String actual = prefs.getString("testKey", "defaultValue");
    assertThat(actual, is("testValue"));
  }

  @Test public void removeAndGetDefaultString() {
    prefs.putString("testKey", "testValue");
    prefs.remove("testKey");
    String actual = prefs.getString("testKey", "defaultValue");
    assertThat(actual, is("defaultValue"));
  }
}
