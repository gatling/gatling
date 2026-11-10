/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.body.multipart.impl;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * Encodes the {@code name} and {@code filename} parameters of a multipart/form-data part's {@code
 * Content-Disposition} header like browsers do, as per <a
 * href="https://www.rfc-editor.org/rfc/rfc7578">RFC 7578</a> and the <a
 * href="https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#multipart-form-data">WHATWG
 * multipart/form-data encoding algorithm</a>:
 *
 * <ul>
 *   <li>characters that the form's charset can't encode are replaced with HTML numeric character
 *       references, e.g. {@code &#128514;}
 *   <li>{@code "}, CR and LF are escaped as {@code %22}, {@code %0D} and {@code %0A}
 *   <li>the result is written as raw bytes in the form's charset (no {@code filename*}, no full
 *       percent-encoding)
 * </ul>
 */
final class DispositionParamEncoder {

  private DispositionParamEncoder() {}

  static byte[] encode(String value, Charset charset) {
    return escape(value, charset.newEncoder()).getBytes(charset);
  }

  private static String escape(String value, CharsetEncoder encoder) {
    boolean encodable = encoder.canEncode(value);
    if (encodable
        && value.indexOf('"') == -1
        && value.indexOf('\r') == -1
        && value.indexOf('\n') == -1) {
      return value;
    }

    StringBuilder sb = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); ) {
      int codePoint = value.codePointAt(i);
      int charCount = Character.charCount(codePoint);
      if (codePoint == '"') {
        sb.append("%22");
      } else if (codePoint == '\r') {
        sb.append("%0D");
      } else if (codePoint == '\n') {
        sb.append("%0A");
      } else if (encodable || encoder.canEncode(value.substring(i, i + charCount))) {
        sb.appendCodePoint(codePoint);
      } else {
        sb.append("&#").append(codePoint).append(';');
      }
      i += charCount;
    }
    return sb.toString();
  }
}
