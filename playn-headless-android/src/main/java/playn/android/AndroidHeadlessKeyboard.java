/**
 * Copyright 2010 The PlayN Authors
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

import playn.core.Keyboard;
import playn.core.util.Callback;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

public class AndroidHeadlessKeyboard implements Keyboard {

  private final AndroidHeadlessPlatform platform;
  private Listener listener;

  public AndroidHeadlessKeyboard (AndroidHeadlessPlatform platform) {
    this.platform = platform;
  }

  @Override
  public synchronized void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public boolean hasHardwareKeyboard() {
    return false; // TODO: return true for devices that have a hardware keyboard
  }

  @Override
  public void getText(final TextType textType, final String label, final String initVal,
      final Callback<String> callback) {
    platform.activity.runOnUiThread(new Runnable() {
      public void run () {
        final AlertDialog.Builder alert = new AlertDialog.Builder(platform.activity);

        alert.setMessage(label);

        // Set an EditText view to get user input
        final EditText input = new EditText(platform.activity);
        final int inputType;
        switch (textType) {
        case NUMBER:
            inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
            break;
        case EMAIL:
            inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
            break;
        case URL:
            inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
            break;
        case DEFAULT:
        default:
            inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
            break;
        }
        input.setInputType(inputType);
        input.setText(initVal);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            final String value = input.getText().toString();
            platform.invokeLater(new Runnable() {
              public void run() {
                callback.onSuccess(value);
              }
            });
          }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            platform.invokeLater(new Runnable() {
              public void run() {
                callback.onSuccess(null);
              }
            });
          }
        });
        alert.show();
      }
    });
  }

  /*
   * The methods below are called from the GL render thread
   */
  boolean onKeyDown(Event event) {
    if (listener != null) {
      listener.onKeyDown(event);
      return event.getPreventDefault();
    }
    return false;
  }

  boolean onKeyTyped(TypedEvent event) {
    if (listener != null) {
      listener.onKeyTyped(event);
      return event.getPreventDefault();
    }
    return false;
  }

  boolean onKeyUp(Event event) {
    if (listener != null) {
      listener.onKeyUp(event);
      return event.getPreventDefault();
    }
    return false;
  }
}
