// Copyright (C) 2012 Trymph Inc.
package playn.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import playn.core.PlayN;
import playn.core.util.Callback;
import playn.net.ext.InlineConverter;

public class JavaExtNet extends JavaNet {

  public JavaExtNet(JavaExtPlatform platform) {
    super(platform);
  }

  private static final int BUF_SIZE = 4096;
  private List<JavaWebSocket> sockets = new ArrayList<JavaWebSocket>();

  @Override
  public WebSocket createWebSocket(String url, WebSocket.Listener listener) {
    JavaWebSocket socket = new JavaWebSocket(url, listener);
    sockets.add(socket);
    return socket;
  }

  @Override
  public void get(final String urlStr, final Callback<String> callback) {
    new Thread("JavaNet.get(" + urlStr + ")") {
      @Override
      public void run() {
        try {
          URL url = new URL(canonicalizeUrl(urlStr));
          InputStream stream = url.openStream();
          InputStreamReader reader = new InputStreamReader(stream);
          notifySuccess(callback, readFully(reader));

        } catch (MalformedURLException e) {
          notifyFailure(callback, e);
        } catch (IOException e) {
          notifyFailure(callback, e);
        }
      }
    }.start();
  }

  @Override
  public void post(final String urlIn, final String dataIn, final Callback<String> callback) {
    new Thread("JavaNet.post(" + urlIn + ")") {
      public void run() {
        try {
          // Convert inlined request to regular request.
          String method = "POST";
          InlineConverter inlined = new InlineConverter(method, urlIn, dataIn, PlayN.json());
          String urlStr = inlined.getUrl();
          String data = inlined.getBody();
          method = inlined.getHttpMethod();

          URL url = new URL(canonicalizeUrl(urlStr));
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();

          conn.setRequestMethod(method);
          conn.setRequestProperty("Content-type", "application/json; charset=UTF-8");
          for (Map.Entry<String, String> header : inlined.getHeaders()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
          }

          if (method.equals("POST")) {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setAllowUserInteraction(false);
          }

          conn.connect();
          if (method.equals("POST")) {
            conn.getOutputStream().write(data.getBytes("UTF-8"));
            conn.getOutputStream().close();
          }
          String result = readFully(new InputStreamReader(conn.getInputStream()));
          conn.disconnect();
          allowCallbackToProcessFullResponse(callback, conn);
          notifySuccess(callback, result);

        } catch (MalformedURLException e) {
          notifyFailure(callback, e);
        } catch (IOException e) {
          notifyFailure(callback, e);
        }
      }
    }.start();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void allowCallbackToProcessFullResponse(Callback<String> callback, HttpURLConnection conn) {
    try {
      Class<? extends Callback> clazz = callback.getClass();
      Method processResponseStatus = clazz.getMethod("processResponseStatus", int.class, String.class);
      if (processResponseStatus == null) return;
      processResponseStatus.setAccessible(true);
      int statusCode = conn.getResponseCode();
      String reason = conn.getResponseMessage();
      processResponseStatus.invoke(callback, statusCode, reason);

      Method getResponseHeadersOfInterest = clazz.getMethod("getResponseHeadersOfInterest");
      getResponseHeadersOfInterest.setAccessible(true);
      List<String> headers = (List<String>) getResponseHeadersOfInterest.invoke(callback);
      Method processHeader = clazz.getMethod("processResponseHeader", String.class, String.class);
      processHeader.setAccessible(true);
      for (String headerName : headers) {
        String header = conn.getHeaderField(headerName);
        if (header != null) processHeader.invoke(callback, headerName, header);
      }
    } catch (Exception e) {
      PlayN.log().warn("Bad callback", e);
      // ignore
    }
  }

  void update() {
    for (Iterator<JavaWebSocket> it = sockets.iterator(); it.hasNext(); ) {
      JavaWebSocket s = it.next();
      if (!s.update()) {
        it.remove();
      }
    }
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
}
