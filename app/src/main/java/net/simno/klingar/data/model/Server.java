/*
 * Copyright (C) 2016 Simon Norberg
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
package net.simno.klingar.data.model;

import com.google.auto.value.AutoValue;

import okhttp3.HttpUrl;

@AutoValue
public abstract class Server {
  public static Builder builder() {
    return new AutoValue_Server.Builder();
  }

  public abstract HttpUrl uri();

  @AutoValue.Builder public abstract static class Builder {
    @SuppressWarnings("UnusedReturnValue")
    public abstract Builder uri(HttpUrl uri);
    public abstract Server build();
  }
}
