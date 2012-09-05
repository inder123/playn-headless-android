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

import java.util.Map;

import playn.core.PlayN;
import playn.core.util.Callback;
import playn.net.ext.InlineConverter;
import cli.System.AsyncCallback;
import cli.System.IAsyncResult;
import cli.System.IO.StreamReader;
import cli.System.IO.StreamWriter;
import cli.System.Net.WebHeaderCollection;
import cli.System.Net.WebRequest;
import cli.System.Net.WebResponse;

public class IOSExtNet extends IOSNet {

  public IOSExtNet(IOSExtPlatform platform) {
    super(platform);
  }

  @Override
  public void get(String url, Callback<String> callback) {
    try {
      final WebRequest req = WebRequest.Create(url);
      req.BeginGetResponse(gotResponse(req, callback), null);
    } catch (Throwable t) {
      callback.onFailure(t);
    }
  }

  @Override
  public void post(String urlIn, final String dataIn, final Callback<String> callback) {
    try {
      // Convert inlined request to regular request.
      String method = "POST";
      InlineConverter inlined = new InlineConverter(method, urlIn, dataIn, PlayN.json());
      String url = inlined.getUrl();
      final String data = inlined.getBody();
      method = inlined.getHttpMethod();

      final WebRequest req = WebRequest.Create(url);
      req.set_Method(method);
      WebHeaderCollection headers = new WebHeaderCollection();
      for (Map.Entry<String, String> header : inlined.getHeaders()) {
        headers.Add(header.getKey(), header.getValue());
      }
      req.set_Headers(headers);
      req.BeginGetRequestStream(new AsyncCallback(new AsyncCallback.Method() {
        @Override
        public void Invoke(IAsyncResult result) {
          try {
            StreamWriter out = new StreamWriter(req.GetRequestStream());
            out.Write(data);
            out.Close();
            req.BeginGetResponse(gotResponse(req, callback), null);
          } catch (Throwable t) {
            notifyFailure(callback, t);
          }
        }
      }), null);
    } catch (Throwable t) {
      callback.onFailure(t);
    }
  }

  protected AsyncCallback gotResponse (final WebRequest req, final Callback<String> callback) {
    return new AsyncCallback(new AsyncCallback.Method() {
      @Override
      public void Invoke(IAsyncResult result) {
        StreamReader reader = null;
        try {
          WebResponse rsp = req.EndGetResponse(result);
          extractAuthToken(rsp);
          reader = new StreamReader(rsp.GetResponseStream());
          notifySuccess(callback, reader.ReadToEnd());
        } catch (final Throwable t) {
          notifyFailure(callback, t);
        } finally {
          if (reader != null)
            reader.Close();
        }
      }
    });
  }

  private void extractAuthToken(WebResponse rsp) {
    WebHeaderCollection headers = rsp.get_Headers();
    String authToken = headers.Get("X-Trymph-AuthN");
    if (authToken != null && !authToken.isEmpty()) {
      PlayN.storage().setItem("X-Trymph-AuthN", authToken);
    }
  }
}
