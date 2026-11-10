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

package io.gatling.http.client.body;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gatling.http.client.body.multipart.ByteArrayPart;
import io.gatling.http.client.body.multipart.MultipartFormDataRequestBodyBuilder;
import io.gatling.http.client.body.multipart.Part;
import io.gatling.http.client.body.multipart.StringPart;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultipartFormDataRequestBodyTest {

  private static final String CONTENT_TYPE = "multipart/form-data; boundary=BOUNDARY";

  private static String encode(Part<?> part, String contentType, Charset defaultCharset) {
    byte[] bytes =
        new MultipartFormDataRequestBodyBuilder(List.of(part))
            .build(contentType, null, defaultCharset)
            .getBytes();
    // decode with the charset under test so that assertions compare the exact bytes
    return new String(bytes, defaultCharset);
  }

  private static StringPart stringPart(String name) {
    return new StringPart(name, "value", UTF_8, null, null, null, null, null);
  }

  private static ByteArrayPart fileLikePart(String name, String fileName, Charset contentCharset) {
    return new ByteArrayPart(
        name,
        "content".getBytes(UTF_8),
        contentCharset,
        null,
        null,
        null,
        "text/plain",
        null,
        fileName);
  }

  @Test
  void nonAsciiNameIsEncodedInFormCharset() {
    String body = encode(stringPart("prénom-中文"), CONTENT_TYPE, UTF_8);
    assertTrue(body.contains("Content-Disposition: form-data; name=\"prénom-中文\"\r\n"), body);
  }

  @Test
  void nameAndFileNameUseFormCharsetNotPartContentCharset() {
    String body = encode(fileLikePart("prénom", "café.txt", UTF_8), CONTENT_TYPE, ISO_8859_1);
    assertTrue(
        body.contains("Content-Disposition: form-data; name=\"prénom\"; filename=\"café.txt\""),
        body);
  }

  @Test
  void contentTypeCharsetTakesPrecedenceOverDefaultCharset() {
    byte[] bytes =
        new MultipartFormDataRequestBodyBuilder(List.of(stringPart("é")))
            .build(CONTENT_TYPE, ISO_8859_1, UTF_8)
            .getBytes();
    assertTrue(new String(bytes, ISO_8859_1).contains("name=\"é\""));
  }

  @Test
  void quotesCrAndLfAreEscaped() {
    String body = encode(fileLikePart("a\"b\r\nc", "x\"y\r\nz.txt", UTF_8), CONTENT_TYPE, UTF_8);
    assertTrue(body.contains("name=\"a%22b%0D%0Ac\"; filename=\"x%22y%0D%0Az.txt\"\r\n"), body);
  }

  @Test
  void unencodableCharactersAreReplacedWithHtmlCharacterReferences() {
    String body = encode(fileLikePart("中", "😂.txt", UTF_8), CONTENT_TYPE, ISO_8859_1);
    assertTrue(body.contains("name=\"&#20013;\"; filename=\"&#128514;.txt\""), body);
  }
}
