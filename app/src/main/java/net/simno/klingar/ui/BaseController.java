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
package net.simno.klingar.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.ControllerChangeType;
import com.bluelinelabs.conductor.rxlifecycle2.RxController;

import net.simno.klingar.KlingarApp;
import net.simno.klingar.util.Rx;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;

abstract class BaseController extends RxController {

  CompositeDisposable disposables;
  private Unbinder unbinder;
  private boolean hasExited;

  public BaseController(Bundle args) {
    super(args);
  }

  protected abstract int getLayoutResource();
  protected abstract void injectDependencies();

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    injectDependencies();
    View view = inflater.inflate(getLayoutResource(), container, false);
    unbinder = ButterKnife.bind(this, view);
    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    disposables = new CompositeDisposable();
  }

  @Override protected void onDetach(@NonNull View view) {
    super.onDetach(view);
    Rx.dispose(disposables);
  }

  @Override protected void onDestroyView(@NonNull View view) {
    super.onDestroyView(view);
    unbinder.unbind();
    unbinder = null;
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (hasExited) {
      KlingarApp.get(getActivity()).refWatcher().watch(this);
    }
  }

  @Override
  protected void onChangeEnded(@NonNull ControllerChangeHandler changeHandler,
                               @NonNull ControllerChangeType changeType) {
    super.onChangeEnded(changeHandler, changeType);
    hasExited = !changeType.isEnter;
    if (isDestroyed()) {
      KlingarApp.get(getActivity()).refWatcher().watch(this);
    }
  }

  void showToast(int resId) {
    if (getActivity() != null) {
      Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }
  }
}
