/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.client.ahc.util;

import io.gatling.http.client.ahc.uri.Uri;
import org.junit.jupiter.api.Test;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpUtilsTest {

  @Test
  void testExtractCharsetWithoutQuotes() {
    assertEquals(HttpUtils.extractContentTypeCharsetAttribute("text/html; charset=iso-8859-1"), ISO_8859_1);
  }

  @Test
  void testExtractCharsetWithSingleQuotes() {
    assertEquals(HttpUtils.extractContentTypeCharsetAttribute("text/html; charset='iso-8859-1'"), ISO_8859_1);
  }

  @Test
  void testExtractCharsetWithDoubleQuotes() {
    assertEquals(HttpUtils.extractContentTypeCharsetAttribute("text/html; charset=\"iso-8859-1\""), ISO_8859_1);
  }

  @Test
  void testExtractCharsetWithDoubleQuotesAndSpaces() {
    assertEquals(HttpUtils.extractContentTypeCharsetAttribute("text/html; charset= \"iso-8859-1\" "), ISO_8859_1);
  }

  @Test
  void testExtractCharsetWithExtraAttribute() {
    assertEquals(HttpUtils.extractContentTypeCharsetAttribute("text/xml; charset=utf-8; action=\"someaction\""), UTF_8);
  }

  @Test
  void testExtractCharsetFallsBackToUtf8() {
    assertNull(HttpUtils.extractContentTypeCharsetAttribute(APPLICATION_JSON.toString()));
  }

  @Test
  void testGetHostHeader() {
    Uri uri = Uri.create("http://stackoverflow.com/questions/1057564/pretty-git-branch-graphs");
    assertEquals("stackoverflow.com", HttpUtils.hostHeader(uri), "Incorrect hostHeader returned");
  }

  @Test
  void computeOriginForPlainUriWithImplicitPort() {
    assertEquals("http://foo.com", HttpUtils.originHeader(Uri.create("ws://foo.com/bar")));
  }

  @Test
  void computeOriginForPlainUriWithDefaultPort() {
    assertEquals("http://foo.com", HttpUtils.originHeader(Uri.create("ws://foo.com:80/bar")));
  }

  @Test
  void computeOriginForPlainUriWithNonDefaultPort() {
    assertEquals("http://foo.com:81", HttpUtils.originHeader(Uri.create("ws://foo.com:81/bar")));
  }

  @Test
  void computeOriginForSecuredUriWithImplicitPort() {
    assertEquals("https://foo.com", HttpUtils.originHeader(Uri.create("wss://foo.com/bar")));
  }

  @Test
  void computeOriginForSecuredUriWithDefaultPort() {
    assertEquals("https://foo.com", HttpUtils.originHeader(Uri.create("wss://foo.com:443/bar")));
  }

  @Test
  void computeOriginForSecuredUriWithNonDefaultPort() {
    assertEquals("https://foo.com:444", HttpUtils.originHeader(Uri.create("wss://foo.com:444/bar")));
  }
}
