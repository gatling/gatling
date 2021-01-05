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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public final class MimeTypes {

  public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  static final Map<String, String> MIME_TYPES_FILE_TYPE_MAP = new HashMap<>();

  static {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("gatling-mime.types")))) {

      String line;
      while ((line = reader.readLine()) != null) {
        // comment
        if (line.charAt(0) == '#')
          continue;

        StringTokenizer tokenizer = new StringTokenizer(line);
        int tokenCount = tokenizer.countTokens();

        // empty line
        if (tokenCount == 0)
          continue;

        String mimeType = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
          String fileExtension = tokenizer.nextToken();
          MIME_TYPES_FILE_TYPE_MAP.put(fileExtension, mimeType);
        }
      }

    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private MimeTypes() {
  }

  public static String getMimeType(String fileName) {
    int dotIdx = fileName.lastIndexOf('.');
    if (dotIdx != -1 || dotIdx > fileName.length() - 2) {
      String extension = fileName.substring(dotIdx + 1);
      String mimeType = MIME_TYPES_FILE_TYPE_MAP.get(extension);
      if (mimeType != null) {
        return mimeType;
      }
    }

    return DEFAULT_MIME_TYPE;
  }
}
