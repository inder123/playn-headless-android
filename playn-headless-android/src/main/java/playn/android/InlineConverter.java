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

import org.apache.http.client.methods.HttpRequestBase;

import playn.core.Json;
import playn.core.Json.TypedArray;

/**
 * This class checks if a request in inlined. If so, it converts it back into non-inlined request.
 *
 * @author Inderjeet Singh
 */
final class InlineConverter {
  private static final String INLINE_PARAM_TRUE = "X-Greaze-Inline=true";
  private static final String INLINE_PARAM = "X-Greaze-Inline";
  private static final String INLINE_PARAM_FALSE = "X-Greaze-Inline=false";

  private final boolean inlined;
  private final String url;
  private final String method;
  private final String body;
  private final Json.Object envelope;
  private final Map<String, String> headers;

  InlineConverter(String method, String url, String data, Json parser) {
    this.inlined = url.contains(INLINE_PARAM_TRUE);
    this.envelope = inlined ? parser.parse(data) : null;
    this.method = inlined ? extractMethod(method, envelope) : method;
    this.url = addUrlParams(removeGreazeInlineParam(url), envelope, parser);
    this.body = inlined ? extractBody(envelope, parser) : data;
    this.headers = extractHeaders(envelope, parser);
  }

  private String extractMethod(String suppliedMethod, Json.Object envelope) {
    String method = envelope.getString("method");
    if (method == null) method = suppliedMethod;
    return method;
  }

  public String getHttpMethod() {
    return method;
  }

  public String getUrl() {
    return url;
  }

  public String getBody() {
    return body;
  }

  public void applyHeaders(HttpRequestBase req) {
    for (Map.Entry<String, String> header : headers.entrySet()) {
      req.setHeader(header.getKey(), header.getValue());
    }
  }

  /** Visible for testing only */
  Map<String, String> getHeaders() {
    return headers;
  }

  /** Visible for testing only */
  static String removeGreazeInlineParam(String url) {
    int start = url.indexOf(INLINE_PARAM);
    if (start == -1) return url;
    start = url.indexOf(INLINE_PARAM_TRUE);
    int length = INLINE_PARAM_TRUE.length();
    if (start == -1) {
      start = url.indexOf(INLINE_PARAM_FALSE);
      if (start == -1) return url;
      length = INLINE_PARAM_FALSE.length();
    }
    if (url.charAt(start-1) == '&') { // consume the & before
      --start;
      ++length;
    } else if (url.length() > start+length+1 && url.charAt(start+length+1) == '&') { // consume the & after
      ++length;
    }
    int second = start + length;
    url = url.substring(0, start) + url.substring(second);
    if (url.contains("?&")) { // remove ?&
      int index = url.indexOf("?&");
      url = url.substring(0, index+1) + url.substring(index + 2);
    }
    if (url.endsWith("?")) url = url.substring(0, url.length() - 1); // remove trailing ?
    return url;
  }

  private String addUrlParams(String url, Json.Object envelope, Json parser) {
    Map<String, String> params = extractMap(envelope, "urlParams", parser);
    for (Map.Entry<String, String> param : params.entrySet()) {
      url = addUrlParam(url, param.getKey(), param.getValue());
    }
    return url;
  }

  private String addUrlParam(String url, String key, String value) {
    if (key.equals(INLINE_PARAM)) return url;
    String param = UrlUtils.urlEncode(key) + "=" + UrlUtils.urlEncode(value);
    if (url.contains(param)) return url; // Already present, no need to add again
    url += url.contains("?") ? "&" : "?";
    url += param;
    return url;
  }

  private Map<String, String> extractHeaders(Json.Object envelope, Json parser) {
    return extractMap(envelope, "headers", parser);
  }

  private Map<String, String> extractMap(Json.Object envelope, String objectKey, Json parser) {
    Map<String, String> map = new HashMap<String, String>();
    if (!inlined) return map;
    Json.Object object = envelope.getObject(objectKey);
    if (object == null) return map;
    TypedArray<String> keys = object.keys();
    for (int i = 0; i < keys.length(); ++i) {
      String key = keys.get(i);
      String value = null;
      if (object.isString(key)) {
        value = object.getString(key);
      } else if (object.isBoolean(key)) {
        value = String.valueOf(object.getBoolean(key));
      } else if (object.isNumber(key)) {
        value = String.valueOf(object.getInt(key));
      } else if (object.isArray(key)) {
        value = toJsonString(object.getArray(key), parser);
      } else if (object.isObject(key)) {
        value = toJsonString(object.getObject(key), parser);
      }
      if (value != null) map.put(key, value);
    }
    return map;
  }

  /** Visible for testing only */
  static String extractBody(Json.Object envelope, Json parser) {
    return toJsonString(envelope.getObject("body"), parser);
  }

  private static String toJsonString(Json.Array json, Json parser) {
    return parser.newWriter().value(json).write();
  }

  private static String toJsonString(Json.Object json, Json parser) {
    return parser.newWriter().value(json).write();
  }
}
