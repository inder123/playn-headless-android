/**
 * Copyright 2012 The PlayN Authors
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
package playn.ios;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import playn.core.Game;
import playn.core.Json;
import playn.core.Mouse;
import playn.core.MouseStub;
import playn.core.PlayN;
import playn.core.RegularExpression;
import playn.core.json.JsonImpl;
import playn.http.Http;
import playn.http.HttpIOS;
import cli.MonoTouch.Foundation.NSUrl;
import cli.MonoTouch.UIKit.UIApplication;
import cli.MonoTouch.UIKit.UIDeviceOrientation;
import cli.MonoTouch.UIKit.UIInterfaceOrientation;
import cli.MonoTouch.UIKit.UIScreen;
import cli.MonoTouch.UIKit.UIWindow;
import cli.System.Drawing.RectangleF;

public class IOSExtPlatform extends IOSPlatform {

  /**
   * Registers your application. Defaults to supporting {@link SupportedOrients#PORTRAITS} and
   * native iPad resolution.
   */
  public static IOSExtPlatform register(UIApplication app) {
    return register(app, SupportedOrients.PORTRAITS);
  }

  /**
   * Registers your application with the specified supported orientations and native iPad
   * resolution.
   */
  public static IOSExtPlatform register(UIApplication app, SupportedOrients orients) {
    return register(app, orients, false);
  }

  /**
   * Registers your application with the specified supported orientations.
   *
   * @param iPadLikePhone if true, an iPad will be treated like a 2x Retina device with resolution
   * 384x512 and which will use @2x images. A Retina iPad will also have resolution 384x512 and
   * will use @4x images if they exist, then fall back to @2x (and default (1x) if necessary). If
   * false, iPad will be treated as a non-Retina device with resolution 768x1024 and will use
   * default (1x) images, and a Retina iPad will be treated as a Retina device with resolution
   * 768x1024 and will use @2x images.
   */
  public static IOSExtPlatform register(UIApplication app, SupportedOrients orients,
                                     boolean iPadLikePhone) {
    IOSExtPlatform platform = new IOSExtPlatform(app, orients, iPadLikePhone);
    PlayN.setPlatform(platform);
    Http.register(new HttpIOS(platform));
    return platform;
  }

  static {
    // disable output to System.out/err as that will result in a crash due to iOS disallowing
    // writes to stdout/stderr
    OutputStream noop = new OutputStream() {
      @Override
      public void write(int b) throws IOException {} // noop!
      @Override
      public void write(byte b[], int off, int len) throws IOException {} // noop!
    };
    System.setOut(new PrintStream(noop));
    System.setErr(new PrintStream(noop));
  }

  private final IOSAudio audio;
  private final IOSGraphics graphics;
  private final Json json;
  private final IOSKeyboard keyboard;
  private final IOSExtNet net;
  private final IOSPointer pointer;
  private final IOSStorage storage;
  private final IOSTouch touch;
  private final IOSAssets assets;
  private final IOSAnalytics analytics;

  private Game game;
  private float accum, alpha;

  private final SupportedOrients orients;
  private final UIApplication app;
  private final UIWindow mainWindow;
  private final IOSGameView gameView;

  protected IOSExtPlatform(UIApplication app, SupportedOrients orients, boolean iPadLikePhone) {
    super(app, orients, iPadLikePhone);
    this.app = app;
    this.orients = orients;

    float deviceScale = UIScreen.get_MainScreen().get_Scale();
    RectangleF bounds = UIScreen.get_MainScreen().get_Bounds();
    int screenWidth = (int)bounds.get_Width(), screenHeight = (int)bounds.get_Height();
    boolean useHalfSize = (screenWidth >= 768) && iPadLikePhone;
    float viewScale = (useHalfSize ? 2 : 1) * deviceScale;
    if (useHalfSize) {
      screenWidth /= 2;
      screenHeight /= 2;
    }

    audio = new IOSAudio(this);
    graphics = new IOSGraphics(this, screenWidth, screenHeight, viewScale, deviceScale);
    json = new JsonImpl();
    keyboard = new IOSKeyboard();
    net = new IOSExtNet(this);
    pointer = new IOSPointer(graphics);
    touch = new IOSTouch(graphics);
    assets = new IOSAssets(this);
    analytics = new IOSAnalytics();
    storage = new IOSStorage();

    mainWindow = new UIWindow(bounds);
    mainWindow.Add(gameView = new IOSGameView(this, bounds, deviceScale));

    // if the game supplied a proper delegate, configure it (for lifecycle notifications)
    if (app.get_Delegate() instanceof IOSApplicationDelegate)
      ((IOSApplicationDelegate) app.get_Delegate()).setPlatform(this);

    // configure our orientation to a supported default, a notification will come in later that
    // will adjust us to the device's current orientation
    onOrientationChange(orients.defaultOrient);
  }

  @Override
  public Type type() {
    return Type.IOS;
  }

  @Override
  public IOSAssets assets() {
    return assets;
  }

  @Override
  public IOSAnalytics analytics() {
    return analytics;
  }

  @Override
  public IOSAudio audio() {
    return audio;
  }

  @Override
  public IOSGraphics graphics() {
    return graphics;
  }

  @Override
  public Json json() {
    return json;
  }

  @Override
  public IOSKeyboard keyboard() {
    return keyboard;
  }

  @Override
  public IOSExtNet net() {
    return net;
  }

  @Override
  public Mouse mouse() {
    return new MouseStub();
  }

  @Override
  public IOSTouch touch() {
    return touch;
  }

  @Override
  public IOSPointer pointer() {
    return pointer;
  }

  @Override
  public float random() {
    return (float) Math.random();
  }

  @Override
  public RegularExpression regularExpression() {
    return null; // new IOSRegularExpression();
  }

  @Override
  public IOSStorage storage() {
    return storage;
  }

  @Override
  public double time() {
    return System.currentTimeMillis();
  }

  @Override
  public void openURL(String url) {
    if (!app.OpenUrl(new NSUrl(url))) {
      log().warn("Failed to open URL: " + url);
    }
  }

  @Override
  public void run(Game game) {
    this.game = game;
    // initialize the game and start things off
    game.init();
    // start the main game loop (TODO: support 0 update rate)
    gameView.Run(1000d / game.updateRate());
    // make our main window visible
    mainWindow.MakeKeyAndVisible();
  }

  // make these accessible to IOSApplicationDelegate
  protected void onPause() {
    super.onPause();
  }
  protected void onResume() {
    super.onResume();
  }
  protected void onExit() {
    super.onExit();
  }

  void viewDidInit(int defaultFrameBuffer) {
    graphics.ctx.viewDidInit(defaultFrameBuffer);
  }

  void onOrientationChange(UIDeviceOrientation orientation) {
    // Needed to handle the case when this method gets called through base-class constructor
    if (orients == null) return;
    if (!orients.isSupported(orientation))
      return; // ignore unsupported (or Unknown) orientations
    graphics.setOrientation(orientation);
    UIInterfaceOrientation sorient = ORIENT_MAP.get(orientation);
    if (!sorient.equals(app.get_StatusBarOrientation())) {
      app.SetStatusBarOrientation(sorient, !app.get_StatusBarHidden());
    }
    // TODO: notify the game of the orientation change
  }

  void update(float delta) {
    // log.debug("Update " + delta);

    // process pending actions
    runQueue.execute();

    // perform the game updates
    float updateRate = game.updateRate();
    if (updateRate == 0) {
      game.update(delta);
      accum = 0;
    } else {
      accum += delta;
      while (accum >= updateRate) {
        game.update(updateRate);
        accum -= updateRate;
      }
    }

    // save the alpha, we'll get a call to paint later
    alpha = (updateRate == 0) ? 0 : accum / updateRate;
  }

  void paint() {
    // log.debug("Paint " + alpha);
    graphics.paint(game, alpha);
  }

  protected static final Map<UIDeviceOrientation,UIInterfaceOrientation> ORIENT_MAP =
    new HashMap<UIDeviceOrientation,UIInterfaceOrientation>();
  static {
    ORIENT_MAP.put(UIDeviceOrientation.wrap(UIDeviceOrientation.Portrait),
                   UIInterfaceOrientation.wrap(UIInterfaceOrientation.Portrait));
    ORIENT_MAP.put(UIDeviceOrientation.wrap(UIDeviceOrientation.PortraitUpsideDown),
                   UIInterfaceOrientation.wrap(UIInterfaceOrientation.PortraitUpsideDown));
    // nb: these are swapped, because of some cracksmoking at Apple
    ORIENT_MAP.put(UIDeviceOrientation.wrap(UIDeviceOrientation.LandscapeLeft),
                   UIInterfaceOrientation.wrap(UIInterfaceOrientation.LandscapeRight));
    ORIENT_MAP.put(UIDeviceOrientation.wrap(UIDeviceOrientation.LandscapeRight),
                   UIInterfaceOrientation.wrap(UIInterfaceOrientation.LandscapeLeft));
  }
}
