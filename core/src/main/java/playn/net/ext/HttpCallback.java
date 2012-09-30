// Copyright (C) 2012 Trymph Inc.
package playn.net.ext;

import java.util.List;

import playn.core.util.Callback;

/**
 * A special callback that also provides a mechanism to process response headers.
 *
 * @author Inderjeet Singh
 */
public interface HttpCallback<T> extends Callback<T> {
  public List<String> getResponseHeadersOfInterest();
  public void processResponseHeader(String header, String value);
  public void processResponseStatus(int responseStatusCode, String reasonPhrase, String responseBody);
}
