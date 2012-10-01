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