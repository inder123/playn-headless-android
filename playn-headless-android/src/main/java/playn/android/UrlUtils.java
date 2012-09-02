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

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitis related to URL parameter handling.
 *
 * @author Inderjeet Singh
 */
public final class UrlUtils {

  private static final Map<Character, String> encoded = buildMap();

  private static Map<Character, String> buildMap() {
    Map<Character, String> map = new HashMap<Character, String>();
    map.put(' ', "%20");
    map.put('!', "%21");
    map.put('"', "%22");
    map.put('#', "%23");
    map.put('$', "%24");
    map.put('%', "%25");
    map.put('&', "%26");
    map.put('\'', "%27");
    map.put('(', "%28");
    map.put(')', "%29");
    map.put('*', "%2A");
    map.put('+', "%2B");
    map.put(',', "%2C");
    map.put('-', "%2D");
    map.put('.', "%2E");
    map.put('/', "%2F");
    map.put(':', "%3A");
    map.put(';', "%3B");
    map.put('<', "%3C");
    map.put('>', "%3E");
    map.put('=', "%3D");
    map.put('?', "%3F");
    map.put('@', "%40");
    map.put('[', "%5B");
    map.put('\\', "%5C");
    map.put(']', "%5D");
    map.put('^', "%5E");
    // map.put('_', "%5F");
    map.put('`', "%60");
    map.put('{', "%7B");
    map.put('|', "%7C");
    map.put('}', "%7D");
    map.put('~', "%7E");
    return map;
  }

  public static String urlEncode(String str) {
    StringBuilder sb = new StringBuilder();
    for (char c : str.toCharArray()) {
      String escaped = encoded.get(c);
      if (escaped == null) {
        sb.append(c);
      } else {
        sb.append(escaped);
      }
    }
    return sb.toString();
  }
}
