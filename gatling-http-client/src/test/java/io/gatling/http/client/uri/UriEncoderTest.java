/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.uri;

import io.gatling.http.client.Param;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UriEncoderTest {

  private static String encode(UriEncoder encoder, String uri, Param... params) {
    return encoder.encode(Uri.create(uri), Arrays.asList(params)).toString();
  }

  @Test
  public void testRawEncoder() {
    UriEncoder encoder = UriEncoder.RAW;
    assertEquals("https://gatling.io", encode(encoder, "https://gatling.io"));
    assertEquals("https://gatling.io/foo", encode(encoder, "https://gatling.io/foo"));
    assertEquals("https://gatling.io/foo bar", encode(encoder, "https://gatling.io/foo bar"));
    assertEquals("https://gatling.io?foo=bar&F OO=B AR", encode(encoder, "https://gatling.io", new Param("foo", "bar"), new Param("F OO", "B AR")));
    assertEquals("https://gatling.io/f oo?f oo=b ar&F OO=B AR", encode(encoder, "https://gatling.io/f oo?f oo=b ar", new Param("F OO", "B AR")));
  }

  @Test
  public void testFixingEncoder() {
    UriEncoder encoder = UriEncoder.FIXING;
    assertEquals("https://gatling.io", encode(encoder, "https://gatling.io"));
    assertEquals("https://gatling.io/foo", encode(encoder, "https://gatling.io/foo"));
    assertEquals("https://gatling.io/foo%20bar", encode(encoder,"https://gatling.io/foo bar"));
    assertEquals("https://gatling.io?foo=bar&F+OO=B+AR", encode(encoder, "https://gatling.io", new Param("foo", "bar"), new Param("F OO", "B AR")));
    assertEquals("https://gatling.io/f%20oo?f+oo=b+ar&F+OO=B+AR", encode(encoder, "https://gatling.io/f oo?f oo=b ar", new Param("F OO", "B AR")));
  }
}
