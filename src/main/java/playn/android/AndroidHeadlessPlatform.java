/**
 * Copyright 2011 The PlayN Authors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/
package playn.android;

import playn.core.AbstractPlatform;
import playn.core.Assets;
import playn.core.Audio;
import playn.core.Game;
import playn.core.Graphics;
import playn.core.Json;
import playn.core.Keyboard;
import playn.core.Mouse;
import playn.core.MouseStub;
import playn.core.Net;
import playn.core.PlayN;
import playn.core.Pointer;
import playn.core.Touch;
import playn.core.json.JsonImpl;
import playn.http.Http;
import playn.http.HttpAndroid;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

/**
 * A headless version of Android platform that eliminates all dependencies on open-gl
 * and mouse capture events. It also uses a {@link Handler#post(Runnable)} to implement
 * {@link #invokeLater(Runnable)} method instead of run queue.
 *
 * @author Inderjeet Singh
 */
public class AndroidHeadlessPlatform extends AbstractPlatform {

  public static final boolean DEBUG_LOGS = false;

  private static boolean init = false;

  public static synchronized void registerApp(Context appContext) {
    if (!init) {
      init = true;
      AndroidHeadlessPlatform platform = new AndroidHeadlessPlatform(appContext);
      PlayN.setPlatform(platform);
      Http.register(new HttpAndroid(platform));
    }
  }

  public static synchronized void setActivity(Activity currentActivity) {
    if (!init) {
      registerApp(currentActivity.getApplicationContext());
    }
    ((AndroidHeadlessPlatform)PlayN.platform()).setCurrentActivityForApp(currentActivity);
  }

  Game game;
  private final Context appContext;
  private Activity currentActivity;
  private final Handler handler = new Handler();

  private final AndroidAnalytics analytics = new AndroidAnalytics();
  private final AndroidHeadlessKeyboard keyboard;
  private final AndroidHeadlessNet net;
  private final AndroidHeadlessStorage storage;
  private final Json json = new JsonImpl();

  protected AndroidHeadlessPlatform(Context appContext) {
    super(new AndroidLog());
    this.appContext = appContext;

    keyboard = new AndroidHeadlessKeyboard(this);
    net = new AndroidHeadlessNet(this);
    storage = new AndroidHeadlessStorage(appContext);
  }

  private void setCurrentActivityForApp(Activity activity) {
    this.currentActivity = activity;
  }

  public Context getCurrentContext() {
    return currentActivity != null ? currentActivity : appContext;
  }

  public void runOnUiThread(Runnable action) {
    if (currentActivity == null) {
      new Handler().post(action);
    } else {
      currentActivity.runOnUiThread(action);
    }
  }

  @Override
  public Assets assets() {
    throw new AssertionError();
  }

  @Override
  public AndroidAnalytics analytics() {
    return analytics;
  }

  @Override
  public Audio audio() {
    throw new AssertionError();
  }

  @Override
  public Graphics graphics() {
    throw new AssertionError();
  }

  @Override
  public Json json() {
    return json;
  }

  @Override
  public Keyboard keyboard() {
    return keyboard;
  }

  @Override
  public Net net() {
    return net;
  }

  @Override
  public void openURL(String url) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    appContext.startActivity(browserIntent);
  }

  @Override
  public void invokeLater(Runnable runnable) {
    handler.post(runnable);
  }

  @Override
  public void invokeAsync(final Runnable action) {
    Runnable runnable = new Runnable() {
      public void run () {
        new AsyncTask<Void,Void,Void>() {
          @Override public Void doInBackground(Void... params) {
            try {
              action.run();
            } catch (Exception e) {
              log.warn("Async task failure [task=" + action + "]", e);
            }
            return null;
          }
        }.execute();
      }
    };
    if (currentActivity == null) {
      new Handler().post(runnable);
    } else {
      currentActivity.runOnUiThread(runnable);
    }
  }

  @Override
  public Mouse mouse() {
    return new MouseStub();
  }

  @Override
  public Touch touch() {
    throw new AssertionError();
  }

  @Override
  public Pointer pointer() {
    throw new AssertionError();
  }

  @Override
  public float random() {
    return (float) Math.random();
  }

  @Override
  public AndroidRegularExpression regularExpression() {
    return new AndroidRegularExpression();
  }

  @Override
  public void run(Game game) {
    this.game = game;
    game.init();
  }

  @Override
  public AndroidHeadlessStorage storage() {
    return storage;
  }

  @Override
  public double time() {
    return System.currentTimeMillis();
  }

  @Override
  public Type type() {
    return Type.ANDROID;
  }

  @Override
  public void setPropagateEvents(boolean propagate) {
    // No touch and pointer
  }
}
