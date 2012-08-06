// Copyright (C) 2012 Trymph Inc.
package playn.android;

import android.app.Activity;
import android.view.Gravity;
import android.widget.LinearLayout;

public final class AndroidRootView extends LinearLayout {

  public AndroidRootView(Activity activity) {
    super(activity);
    setBackgroundColor(0xFF000000);
    setGravity(Gravity.CENTER);
  }

  @Override
  public void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
  }
}
