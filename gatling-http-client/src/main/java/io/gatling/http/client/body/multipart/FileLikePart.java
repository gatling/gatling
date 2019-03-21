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

package io.gatling.http.client.body.multipart;

import io.gatling.http.client.Param;
import io.gatling.http.client.util.MiscUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public abstract class FileLikePart<T> extends Part<T> {

  private static final MimetypesFileTypeMap MIME_TYPES_FILE_TYPE_MAP;

  static {
    try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("gatling-mime.types")) {
      MIME_TYPES_FILE_TYPE_MAP = new MimetypesFileTypeMap(is);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final String fileName;

  FileLikePart(String name, T content, Charset charset, String transferEncoding, String contentId, String dispositionType, String contentType, List<Param> customHeaders, String fileName) {
    super(name,
            content,
            charset,
            transferEncoding,
            contentId,
            dispositionType,
            computeContentType(contentType, fileName),
            customHeaders
            );
    this.fileName = fileName;
  }

  private static String computeContentType(String contentType, String fileName) {
    return contentType != null ? contentType : MIME_TYPES_FILE_TYPE_MAP.getContentType(MiscUtils.withDefault(fileName, ""));
  }

  public String getFileName() {
    return fileName;
  }
}
