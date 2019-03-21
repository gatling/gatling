/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

//
// Copyright (c) 2018 AsyncHttpClient Project. All rights reserved.
//
// This program is licensed to you under the Apache License Version 2.0,
// and you may not use this file except in compliance with the Apache License Version 2.0.
// You may obtain a copy of the Apache License Version 2.0 at
//     http://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the Apache License Version 2.0 is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
//

package io.gatling.http.client.util;

import io.gatling.http.client.uri.Uri;
import io.gatling.netty.util.ahc.StringBuilderPool;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;

import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class HttpUtils {

  private static final String CONTENT_TYPE_CHARSET_ATTRIBUTE = "charset=";
  private static final String CONTENT_TYPE_BOUNDARY_ATTRIBUTE = "boundary=";
  private static final String BROTLY_ACCEPT_ENCODING_SUFFIX = ", br";

  private HttpUtils() {
  }

  public static String hostHeader(Uri uri) {
    String host = uri.getHost();
    int port = uri.getPort();
    return port == -1 || port == uri.getSchemeDefaultPort() ? host : host + ":" + port;
  }

  public static String originHeader(Uri uri) {
    StringBuilder sb = StringBuilderPool.DEFAULT.get();
    sb.append(uri.isSecured() ? "https://" : "http://").append(uri.getHost());
    if (uri.getExplicitPort() != uri.getSchemeDefaultPort()) {
      sb.append(':').append(uri.getPort());
    }
    return sb.toString();
  }

  public static Charset extractContentTypeCharsetAttribute(String contentType) {
    String charsetName = extractContentTypeAttribute(contentType, CONTENT_TYPE_CHARSET_ATTRIBUTE);
    return charsetName != null ? Charset.forName(charsetName) : null;
  }

  public static String extractContentTypeBoundaryAttribute(String contentType) {
    return extractContentTypeAttribute(contentType, CONTENT_TYPE_BOUNDARY_ATTRIBUTE);
  }

  private static String extractContentTypeAttribute(String contentType, String attribute) {
    if (contentType == null) {
      return null;
    }

    for (int i = 0; i < contentType.length(); i++) {
      if (contentType.regionMatches(true, i, attribute, 0,
              attribute.length())) {
        int start = i + attribute.length();

        // trim left
        while (start < contentType.length()) {
          char c = contentType.charAt(start);
          if (c == ' ' || c == '\'' || c == '"') {
            start++;
          } else {
            break;
          }
        }
        if (start == contentType.length()) {
          break;
        }

        // trim right
        int end = start + 1;
        while (end < contentType.length()) {
          char c = contentType.charAt(end);
          if (c == ' ' || c == '\'' || c == '"' || c == ';') {
            break;
          } else {
            end++;
          }
        }

        return contentType.substring(start, end);
      }
    }

    return null;
  }

  // The pool of ASCII chars to be used for generating a multipart boundary.
  private static byte[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(US_ASCII);

  // a fixed size of 35
  public static byte[] computeMultipartBoundary() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    byte[] bytes = new byte[35];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = MULTIPART_CHARS[random.nextInt(MULTIPART_CHARS.length)];
    }
    return bytes;
  }

  public static String patchContentTypeWithBoundaryAttribute(CharSequence base, byte[] boundary) {
    StringBuilder sb = StringBuilderPool.DEFAULT.get().append(base);
    if (base.length() != 0 && base.charAt(base.length() - 1) != ';') {
      sb.append(';');
    }
    return sb.append(' ').append(CONTENT_TYPE_BOUNDARY_ATTRIBUTE).append(new String(boundary, US_ASCII)).toString();
  }

  public static CharSequence filterOutBrotliFromAcceptEncoding(String acceptEncoding) {
    // we don't support Brotly ATM
    if (acceptEncoding.endsWith(BROTLY_ACCEPT_ENCODING_SUFFIX)) {
      return acceptEncoding.subSequence(0, acceptEncoding.length() - BROTLY_ACCEPT_ENCODING_SUFFIX.length());
    }
    return acceptEncoding;
  }

  public static boolean isConnectionClose(HttpHeaders headers) {
    return headers.contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true);
  }
}
