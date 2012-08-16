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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import playn.core.NetImpl;
import playn.core.PlayN;
import playn.core.util.Callback;

/**
 * A headless version of {@link NetImpl} that is largely a copy of AndroidNet but avoids
 * the dependency on AndroidPlatform.
 *
 * @author Inderjeet Singh
 */
final class AndroidHeadlessNet extends NetImpl {

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

  private void doHttp(final boolean isPost, final String url, final String data,
                      final Callback<String> callback) {
    new Thread("AndroidNet.doHttp") {
      public void run() {
        HttpClient httpclient = new DefaultHttpClient();
        HttpRequestBase req = null;
        if (isPost) {
          HttpPost httppost = new HttpPost(url);
          if (data != null) {
            try {
              httppost.setEntity(new StringEntity(data));
            } catch (UnsupportedEncodingException e) {
              notifyFailure(callback, e);
            }
          }
          req = httppost;
        } else {
          req = new HttpGet(url);
        }
        try {
          HttpResponse response = httpclient.execute(req);
          examineHeaders(callback, response);
          if (response.getStatusLine().getStatusCode() >= 400) {
            throw new RuntimeException(response.getStatusLine().toString());
          }
          notifySuccess(callback, EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
          notifyFailure(callback, e);
        }
      }

      @SuppressWarnings({ "rawtypes", "unchecked" })
      private void examineHeaders(Callback<String> callback, HttpResponse response) {
        try {
          Class<? extends Callback> clazz = callback.getClass();
          Method getResponseHeadersOfInterest = clazz.getMethod("getResponseHeadersOfInterest");
          if (getResponseHeadersOfInterest == null) return;
          List<String> headers = (List<String>) getResponseHeadersOfInterest.invoke(callback);
          Method processHeader = clazz.getMethod("processResponseHeader", String.class, String.class);
          for (String headerName : headers) {
            Header header = response.getFirstHeader(headerName);
            if (header != null) processHeader.invoke(callback, headerName, header.getValue());
          }
        } catch (Exception e) {
          PlayN.log().warn("examineHeaders failed", e);
          // ignore
        }
      }
    }.start();
  }
}
