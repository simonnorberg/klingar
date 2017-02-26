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
package net.simno.klingar.util;

import io.reactivex.FlowableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public final class RxHelper {

  private static final SingleTransformer SINGLE_SCHEDULERS = single -> single
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread());

  private static final FlowableTransformer FLOWABLE_SCHEDULERS = flowable -> flowable
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread());

  private RxHelper() {
    // no instances
  }

  public static void dispose(Disposable disposable) {
    if (disposable != null && !disposable.isDisposed()) {
      disposable.dispose();
    }
  }

  public static <T> FlowableTransformer<T, T> flowableSchedulers() {
    //noinspection unchecked
    return (FlowableTransformer<T, T>) RxHelper.FLOWABLE_SCHEDULERS;
  }

  public static <T> SingleTransformer<T, T> singleSchedulers() {
    //noinspection unchecked
    return (SingleTransformer<T, T>) RxHelper.SINGLE_SCHEDULERS;
  }
}
