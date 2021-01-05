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

package io.gatling.http.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MimeTypesTest {

  @Test
  void testRegisteredFileExtensionsCount() {
    assertEquals(982, MimeTypes.MIME_TYPES_FILE_TYPE_MAP.size());
  }

  @Test
  void testExpectedEntry() {
    assertEquals("application/json", MimeTypes.getMimeType("/foo/bar.json"));
  }

  @Test
  void testDefaultMimeType() {
    assertEquals(MimeTypes.DEFAULT_MIME_TYPE, MimeTypes.getMimeType("/foo/bar.unknown"));
  }
}
