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

import org.reactivestreams.Publisher;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public final class Rx {

  private final Scheduler io;
  private final Scheduler ui;
  private final Scheduler newThread;
  private final SingleTransformer singleSchedulers = new SingleTransformer() {
    @Override public SingleSource apply(Single upstream) {
      return upstream.subscribeOn(io).observeOn(ui);
    }
  };
  private final FlowableTransformer flowableSchedulers = new FlowableTransformer() {
    @Override public Publisher apply(Flowable upstream) {
      return upstream.subscribeOn(io).observeOn(ui);
    }
  };

  private Rx(Scheduler io, Scheduler ui, Scheduler newThread) {
    this.io = io;
    this.ui = ui;
    this.newThread = newThread;
  }

  public static void dispose(Disposable disposable) {
    if (disposable != null && !disposable.isDisposed()) {
      disposable.dispose();
    }
  }

  public static void onError(Throwable throwable) {
    Timber.e(throwable, "onError");
  }

  private static Rx production() {
    return new Rx(Schedulers.io(), AndroidSchedulers.mainThread(), Schedulers.newThread());
  }

  public static Rx test() {
    return new Rx(Schedulers.trampoline(), Schedulers.trampoline(), Schedulers.trampoline());
  }

  public <T> FlowableTransformer<T, T> flowableSchedulers() {
    //noinspection unchecked
    return (FlowableTransformer<T, T>) flowableSchedulers;
  }

  public <T> SingleTransformer<T, T> singleSchedulers() {
    //noinspection unchecked
    return (SingleTransformer<T, T>) singleSchedulers;
  }

  public Scheduler io() {
    return io;
  }

  public Scheduler newThread() {
    return newThread;
  }

  @Module
  public static class RxModule {
    @Provides @Singleton Rx provideRx() {
      return Rx.production();
    }
  }
}
