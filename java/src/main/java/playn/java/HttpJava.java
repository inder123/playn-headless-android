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
package playn.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import playn.core.util.Callback;
import playn.http.Http;
import playn.http.HttpException;
import playn.http.HttpMethod;
import playn.http.HttpRequest;
import playn.http.HttpResponse;

/**
 * Pure Java implementation of {@link Http}.
 *
 * @author Inderjeet Singh
 */
public class HttpJava extends Http {

  private static final int BUF_SIZE = 4096;
  private final JavaPlatform platform;

  public HttpJava(JavaPlatform platform) {
    this.platform = platform;
  }

  @Override
  public void doSend(HttpRequest request, Callback<HttpResponse> callback) {
    HttpMethod method = request.getMethod();
    switch (method) {
    case GET:
      get(request, callback);
      break;
    case POST:
    case PUT:
      postOrPut(request, callback);
      break;
    default: throw new UnsupportedOperationException(method.toString());
    }
  }

  public void get(final HttpRequest request, final Callback<HttpResponse> callback) {
    final String urlStr = request.getUrl();
    new Thread("JavaNet.get(" + urlStr + ")") {
      @Override
      public void run() {
        try {
          URL url = new URL(canonicalizeUrl(urlStr));
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          InputStream stream = conn.getInputStream();
          InputStreamReader reader = new InputStreamReader(stream);
          String responseBody = readFully(reader);
          gotResponse(request, conn, responseBody, callback);
        } catch (MalformedURLException e) {
          platform.notifyFailure(callback, e);
        } catch (IOException e) {
          platform.notifyFailure(callback, e);
        }
      }
    }.start();
  }

  public void postOrPut(final HttpRequest request, final Callback<HttpResponse> callback) {
    final String urlStr = request.getUrl();
    new Thread("JavaNet.post(" + urlStr + ")") {
      public void run() {
        try {
          URL url = new URL(canonicalizeUrl(urlStr));
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          HttpMethod method = request.getMethod();
          conn.setRequestMethod(method.toString());
          for (Map.Entry<String, String> header : request.getHeaders()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
          }
          if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setAllowUserInteraction(false);
          }
          conn.connect();
          if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            conn.getOutputStream().write(request.getBody().getBytes("UTF-8"));
            conn.getOutputStream().close();
          }
          String result = readFully(new InputStreamReader(conn.getInputStream()));
          conn.disconnect();
          gotResponse(request, conn, result, callback);
        } catch (MalformedURLException e) {
          platform.notifyFailure(callback, e);
        } catch (IOException e) {
          platform.notifyFailure(callback, e);
        }
      }
    }.start();
  }

  // Super-simple url-cleanup: assumes it either starts with "http", or that
  // it's an absolute path on the current server.
  private String canonicalizeUrl(String url) {
    if (!url.startsWith("http")) {
      return "http://" + server() + url;
    }
    return url;
  }

  // TODO: Make this specifyable somewhere.
  private String server() {
    return "127.0.0.1:8080";
  }

  private String readFully(Reader reader) throws IOException {
    StringBuffer result = new StringBuffer();
    char[] buf = new char[BUF_SIZE];
    int len = 0;
    while (-1 != (len = reader.read(buf))) {
      result.append(buf, 0, len);
    }
    return result.toString();
  }

  private void gotResponse(final HttpRequest req, HttpURLConnection conn, String responseBody,
      final Callback<HttpResponse> callback) throws IOException {
    int statusCode = -1;
    String statusLineMessage = null;
    Map<String, String> responseHeaders = new HashMap<String, String>();
    try {
      statusCode = conn.getResponseCode();
      statusLineMessage = conn.getResponseMessage();
      for (String headerName : conn.getHeaderFields().keySet()) {
        String value = conn.getHeaderField(headerName);
        responseHeaders.put(headerName, value);
      }
      HttpResponse response = new HttpResponse(
          statusCode, statusLineMessage, responseHeaders, responseBody);
      platform.notifySuccess(callback, response);
    } catch (final Throwable t) {
      throw new HttpException(statusCode, statusLineMessage, responseBody, t);
    }
  }
}
