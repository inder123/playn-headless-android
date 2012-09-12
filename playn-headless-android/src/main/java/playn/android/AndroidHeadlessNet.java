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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import playn.core.NetImpl;
import playn.core.PlayN;
import playn.core.util.Callback;
import playn.net.ext.InlineConverter;

/**
 * A headless version of {@link NetImpl} that is largely a copy of AndroidNet but avoids
 * the dependency on AndroidPlatform. In addition, it supports HTTP PUT, and converts
 * Greaze inlined requests into regular requests.
 *
 * @author Inderjeet Singh
 */
final class AndroidHeadlessNet extends NetImpl {
  private static final boolean LOG = true;

  @Override
  public void get(String url, Callback<String> callback) {
    doHttp(false, url, null, callback);
  }

  @Override
  public void post(String url, String data, Callback<String> callback) {
    doHttp(true, url, data, callback);
  }

  AndroidHeadlessNet(AndroidHeadlessPlatform platform) {
    super(platform);
  }

  private void doHttp(final boolean isPost, final String urlIn, final String dataIn,
                      final Callback<String> callback) {
    new Thread("AndroidNet.doHttp") {
      public void run() {
        HttpClient httpclient = new DefaultHttpClient();
        HttpRequestBase req = null;
        // Convert inlined request to regular request.
        String method = isPost ? "POST" : "GET";
        InlineConverter inlined = new InlineConverter(method, urlIn, dataIn, PlayN.json());
        String url = inlined.getUrl();
        String data = inlined.getBody();
        method = inlined.getHttpMethod();
        if (LOG) PlayN.log().info(method + " " + url + "\n" + inlined.getHeaders() + "\n" + data);
        if (method.equalsIgnoreCase("GET")) {
          req = new HttpGet(url);
        } else {
          HttpEntityEnclosingRequestBase op = method.equalsIgnoreCase("PUT")
              ? new HttpPut(url) : new HttpPost(url);
          if (data != null) {
            try {
              op.setEntity(new StringEntity(data));
            } catch (UnsupportedEncodingException e) {
              platform.notifyFailure(callback, e);
            }
          }
          req = op;
        }
        for (Map.Entry<String, String> header : inlined.getHeaders()) {
          req.setHeader(header.getKey(), header.getValue());
        }
        try {
          HttpResponse response = httpclient.execute(req);
          allowCallbackToProcessFullResponse(callback, response);
          platform.notifySuccess(callback, EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
          platform.notifyFailure(callback, e);
        }
      }

      @SuppressWarnings({ "rawtypes", "unchecked" })
      private void allowCallbackToProcessFullResponse(Callback<String> callback, HttpResponse response) {
        try {
          Class<? extends Callback> clazz = callback.getClass();
          Method processResponseStatus = clazz.getMethod("processResponseStatus", int.class, String.class);
          if (processResponseStatus == null) return;
          StatusLine statusLine = response.getStatusLine();
          int statusCode = statusLine.getStatusCode();
          String reason = statusLine.getReasonPhrase();
          processResponseStatus.invoke(callback, statusCode, reason);

          Method getResponseHeadersOfInterest = clazz.getMethod("getResponseHeadersOfInterest");
          List<String> headers = (List<String>) getResponseHeadersOfInterest.invoke(callback);
          Method processHeader = clazz.getMethod("processResponseHeader", String.class, String.class);
          for (String headerName : headers) {
            Header header = response.getFirstHeader(headerName);
            if (header != null) processHeader.invoke(callback, headerName, header.getValue());
          }
        } catch (Exception e) {
          PlayN.log().warn("Bad callback", e);
          // ignore
        }
      }
    }.start();
  }
}
