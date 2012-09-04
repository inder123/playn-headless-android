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
package playn.net.ext;

import java.util.Map.Entry;

import junit.framework.TestCase;
import playn.core.Json;
import playn.core.json.JsonImpl;

/**
 * Unit test for {@link InlineConverter}.
 *
 * @author Inderjeet Singh
 */
public class InlineConverterTest extends TestCase {

  private final Json parser = new JsonImpl();

  public void testRemoveGreazeInline() {
    assertEquals("http://localhost:8080/context/pathinfo/?foo2=bar2",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathinfo/?foo2=bar2"));
    assertEquals("http://localhost:8080/context/pathinfo/?foo1=bar1&foo2=bar2",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathinfo/?foo1=bar1&X-Greaze-Inline=true&foo2=bar2"));
    assertEquals("http://localhost:8080/context/pathinfo/?foo2=bar2",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathinfo/?X-Greaze-Inline=false&foo2=bar2"));
    assertEquals("http://localhost:8080/context/pathinfo/",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathinfo/?X-Greaze-Inline=false"));
    assertEquals("http://localhost:8080/context/pathinfo?a=b",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathinfo?a=b&X-Greaze-Inline=false"));
    assertEquals("http://localhost:8080/context/pathinfo?a=b",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathinfo?X-Greaze-Inline=false&a=b"));
    assertEquals("http://localhost:8080/context/pathInfo?a=b",
        InlineConverter.removeGreazeInlineParam("http://localhost:8080/context/pathInfo?X-Greaze-Inline=true&a=b"));
  }

  public void testParamValuesUrlEscaped() {
    String url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true";
    String data = "{\"urlParams\":{\"msg\":{\"a\":\"b\"}}}";
    InlineConverter inline = new InlineConverter("GET", url, data, parser);
    assertEquals("http://localhost:8080/context/pathinfo?msg=%7B%22a%22%3A%22b%22%7D", inline.getUrl());
  }

  public void testInlineParamValuesSkipped() {
    String url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true";
    String data = "{\"urlParams\":{\"X-Greaze-Inline\":true}}";
    InlineConverter inline = new InlineConverter("GET", url, data, parser);
    assertEquals("http://localhost:8080/context/pathinfo", inline.getUrl());
  }

  public void testHttpMethod() {
    String url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true";
    String data = "{\"method\":\"GET\",\"headers\":{},\"urlParams\":{}}";
    InlineConverter inline = new InlineConverter("POST", url, data, parser);
    assertEquals("GET", inline.getHttpMethod());
    data = "{\"headers\":{},\"urlParams\":{}}";
    inline = new InlineConverter("POST", url, data, parser);
    assertEquals("POST", inline.getHttpMethod());
  }

  public void testBody() {
    String url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true";
    String data = "{\"headers\":{\"h1\":\"v1\"},\"body\":{\"1\":\"2\"},\"urlParams\":{\"a\":\"b\"}}";
    InlineConverter inline = new InlineConverter("POST", url, data, parser);
    assertEquals("{\"1\":\"2\"}", inline.getBody());
  }

  public void testHeaders() {
    String url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true";
    String data = "{\"headers\":{\"h1\":\"v1\"},\"body\":{\"1\":\"2\"},\"urlParams\":{\"a\":\"b\"}}";
    InlineConverter inline = new InlineConverter("POST", url, data, parser);
    Entry<String, String> header = inline.getHeaders().iterator().next();
    assertEquals("h1", header.getKey());
    assertEquals("v1", header.getValue());
  }

  public void testUrlParams() {
    String url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true";
    String data = "{\"headers\":{\"h1\":\"v1\"},\"body\":{\"1\":\"2\"},\"urlParams\":{\"a\":\"b\"}}";
    InlineConverter inline = new InlineConverter("POST", url, data, parser);
    assertTrue(inline.getUrl().contains("a=b"));
    url = "http://localhost:8080/context/pathinfo?X-Greaze-Inline=true&a=b&c=d";
    inline = new InlineConverter("POST", url, data, parser);
    assertEquals(1, getCount(inline.getUrl(), "a=b"));
  }

  private int getCount(String str, String pattern) {
    int count = 0;
    int index;
    while ((index = str.indexOf(pattern)) >= 0) {
      ++count;
      str = str.substring(0, index) + str.substring(index + pattern.length());
    }
    return count;
  }
}
