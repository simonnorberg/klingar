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

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class TestEditor implements SharedPreferences.Editor {

  private final Map<String, Object> preferences = new HashMap<>();

  @Override public SharedPreferences.Editor putString(String key, @Nullable String value) {
    preferences.put(key, value);
    return this;
  }

  @Override
  public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
    throw new UnsupportedOperationException();
  }

  @Override public SharedPreferences.Editor putInt(String key, int value) {
    throw new UnsupportedOperationException();
  }

  @Override public SharedPreferences.Editor putLong(String key, long value) {
    throw new UnsupportedOperationException();
  }

  @Override public SharedPreferences.Editor putFloat(String key, float value) {
    throw new UnsupportedOperationException();
  }

  @Override public SharedPreferences.Editor putBoolean(String key, boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override public SharedPreferences.Editor remove(String key) {
    preferences.remove(key);
    return this;
  }

  @Override public SharedPreferences.Editor clear() {
    throw new UnsupportedOperationException();
  }

  @Override public boolean commit() {
    return true;
  }

  @Override public void apply() {
  }

  Map<String, Object> getPreferences() {
    return preferences;
  }
}
