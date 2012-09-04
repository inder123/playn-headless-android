// Copyright (C) 2012 Trymph Inc.
package playn.java;

import java.io.IOException;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import playn.core.Analytics;
import playn.core.Game;
import playn.core.Json;
import playn.core.Keyboard;
import playn.core.Mouse;
import playn.core.Net;
import playn.core.PlayN;
import playn.core.Pointer;
import playn.core.RegularExpression;
import playn.core.Storage;
import playn.core.Touch;
import playn.core.TouchStub;
import playn.core.json.JsonImpl;

/**
 * This class overrides JavaPlatform to provide an alternate implementation of net. It overrides
 * all the methods that use net.
 *
 * @author Inderjeet Singh
 */
public class JavaExtPlatform extends JavaPlatform {
  private final JavaExtNet net = new JavaExtNet(this);

  // Maximum delta time to consider between update() calls (in milliseconds). If the delta between
  // two update()s is greater than MAX_DELTA, we clamp to MAX_DELTA.
  private static final float MAX_DELTA = 100;

  // Minimum time between any two paint() calls (in milliseconds). We will paint every
  // FRAME_TIME ms, which is equivalent to (1000 * 1 / FRAME_TIME) frames per second.
  // TODO(pdr): this is set ridiculously low because we're using Java's software renderer which
  // causes the paint loop to be quite slow. Setting this to 10 prevents hitching that occurs when
  // we try to squeeze a paint() near max bound of FRAME_TIME.
  private static final float FRAME_TIME = 10;

  private static JavaPlatform testInstance;

  public static JavaExtPlatform register() {
    float scaleFactor = 1;
    String sfprop = System.getProperty("playn.scaleFactor", String.valueOf(scaleFactor));
    try {
      return register(Float.parseFloat(sfprop));
    } catch (Exception e) {
      System.err.println("Invalid scaleFactor supplied '" + sfprop + "': " + e);
    }
    return register(scaleFactor);
  }

  public static JavaExtPlatform register(float scaleFactor) {
    JavaExtPlatform instance = new JavaExtPlatform(scaleFactor, false);
    PlayN.setPlatform(instance);
    return instance;
  }

  public static JavaPlatform registerHeadless() {
    // Guard against multiple-registration. This can happen when running tests in maven.
    if (testInstance != null) {
      return testInstance;
    }
    testInstance = new JavaPlatform(1, true);
    PlayN.setPlatform(testInstance);
    return testInstance;
  }

  private final JavaAnalytics analytics = new JavaAnalytics();
  private final JavaAudio audio = new JavaAudio();
  private final JavaRegularExpression regex = new JavaRegularExpression();
  private final JavaStorage storage = new JavaStorage();
  private final JsonImpl json = new JsonImpl();
  private final JavaKeyboard keyboard = new JavaKeyboard();
  private final JavaPointer pointer = new JavaPointer();
  private final JavaGraphics graphics;
  private final JavaMouse mouse = new JavaMouse(this);
  private final JavaAssets assets = new JavaAssets(this);

  private int updateRate = 0;
  private float accum = updateRate;
  private double lastUpdateTime;
  private double lastPaintTime;

  public JavaExtPlatform(float scaleFactor, boolean headless) {
    super(scaleFactor, headless);
    graphics = new JavaGraphics(this, scaleFactor, headless);
  }

  /**
   * Sets the title of the window.
   *
   * @param title the window title
   */
  public void setTitle(String title) {
    Display.setTitle(title);
  }

  @Override
  public Type type() {
    return Type.JAVA;
  }

  @Override
  public JavaAudio audio() {
    return audio;
  }

  @Override
  public JavaGraphics graphics() {
    return graphics;
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
  public Pointer pointer() {
    return pointer;
  }

  @Override
  public Mouse mouse() {
    return mouse;
  }

  @Override
  public Touch touch() {
    return new TouchStub();
  }

  @Override
  public Storage storage() {
    return storage;
  }

  @Override
  public Analytics analytics() {
    return analytics;
  }

  @Override
  public JavaAssets assets() {
    return assets;
  }

  @Override
  public RegularExpression regularExpression() {
    return regex;
  }

  @Override
  public float random() {
    return (float) Math.random();
  }

  @Override
  public double time() {
    return System.currentTimeMillis();
  }

  @Override
  public void openURL(String url) {
    System.out.println("Opening url: " + url);
    String browser = "chrome ";
    if (System.getProperty("os.name", "-").contains("indows"))
      browser = "rundll32 url.dll,FileProtocolHandler ";
    try {
      Runtime.getRuntime().exec(browser + url);
    } catch (IOException e) {
    }
  }

  @Override
  public void run(final Game game) {
    this.updateRate = game.updateRate();

    storage.init();
    try {
      // initialize LWJGL (and show the display) now that the game has been initialized
      graphics.init();
      // now that the display is initialized we can init our mouse and keyboard
      mouse.init();
      keyboard.init();
    } catch (LWJGLException e) {
      throw new RuntimeException("Unrecoverable initialization error", e);
    }

    game.init();

    boolean wasActive = Display.isActive();
    while (!Display.isCloseRequested()) {
      // Event handling.
      mouse.update();
      keyboard.update();
      pointer.update();
      net.update();

      // Notify the app if lose or regain focus (treat said as pause/resume).
      if (wasActive != Display.isActive()) {
        if (wasActive)
          onPause();
        else
          onResume();
        wasActive = Display.isActive();
      }

      // Execute any pending runnables.
      runQueue.execute();

      // Game loop.
      double now = time();
      float updateDelta = (float) (now - lastUpdateTime);
      if (updateDelta > 1) {
        updateDelta = updateDelta > MAX_DELTA ? MAX_DELTA : updateDelta;
        lastUpdateTime = now;

        if (updateRate == 0) {
          game.update(updateDelta);
          accum = 0;
        } else {
          accum += updateDelta;
          while (accum > updateRate) {
            game.update(updateRate);
            accum -= updateRate;
          }
        }
      }

      float paintDelta = (float) (now - lastPaintTime);
      if (paintDelta > FRAME_TIME) {
        graphics.paint(game, updateRate == 0 ? 0 : accum / updateRate);
        lastPaintTime = now;
      }

      Display.sync(60);
      Display.update();
    }

    onExit();
    System.exit(0);
  }
}
