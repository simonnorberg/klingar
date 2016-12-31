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

import com.bluelinelabs.conductor.Controller;

import butterknife.ButterKnife;
import rx.subscriptions.CompositeSubscription;

import static net.simno.klingar.util.RxHelper.unsubscribe;

abstract class BaseController extends Controller {

  CompositeSubscription subscriptions;

  public BaseController(Bundle args) {
    super(args);
  }

  protected abstract int getLayoutResource();
  protected abstract void injectDependencies();

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    injectDependencies();
    View view = inflater.inflate(getLayoutResource(), container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    subscriptions = new CompositeSubscription();
  }

  @Override protected void onDetach(@NonNull View view) {
    super.onDetach(view);
    unsubscribe(subscriptions);
  }

  void showToast(int resId) {
    if (getActivity() != null) {
      Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }
  }
}
