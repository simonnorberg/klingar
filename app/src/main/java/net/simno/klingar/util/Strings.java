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

public final class Strings {

  private Strings() {
    // no instances
  }

  // android.text.TextUtils.equals
  public static boolean equals(CharSequence a, CharSequence b) {
    if (a == b) return true;
    int length;
    if (a != null && b != null && (length = a.length()) == b.length()) {
      if (a instanceof String && b instanceof String) {
        return a.equals(b);
      } else {
        for (int i = 0; i < length; i++) {
          if (a.charAt(i) != b.charAt(i)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  // https://github.com/JakeWharton/u2020
  public static boolean isBlank(CharSequence string) {
    return string == null || string.toString().trim().length() == 0;
  }
}
