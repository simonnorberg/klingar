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

import org.junit.Test;

import io.reactivex.disposables.CompositeDisposable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RxHelperTest {

  @Test public void dispose() {
    CompositeDisposable disposable = new CompositeDisposable();
    assertFalse(disposable.isDisposed());
    RxHelper.dispose(disposable);
    assertTrue(disposable.isDisposed());
  }

  @Test public void disposeNull() {
    RxHelper.dispose(null);
  }

  @Test public void disposeAlreadyDisposed() {
    CompositeDisposable disposable = new CompositeDisposable();
    disposable.dispose();
    assertTrue(disposable.isDisposed());
    RxHelper.dispose(disposable);
    assertTrue(disposable.isDisposed());
  }
}
