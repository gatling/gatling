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

package io.gatling.http.client.body.multipart.impl;

import io.gatling.http.client.body.multipart.FileLikePart;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class FileLikePartImpl<T extends FileLikePart<?>> extends PartImpl {

  /**
   * Attachment's file name as a byte array
   */
  private static final byte[] FILE_NAME_BYTES = "; filename=".getBytes(US_ASCII);

  FileLikePartImpl(T part, byte[] boundary) {
    super(part, boundary);
  }

  protected void visitContentDispositionHeader(PartVisitor visitor) {
    super.visitContentDispositionHeader(visitor);
    String fileName = ((FileLikePart<?>) part).getFileName();
    if (fileName != null) {
      visitor.withBytes(FILE_NAME_BYTES);
      visitor.withByte(QUOTE_BYTE);
      visitor.withBytes(fileName.getBytes(part.getCharset() != null ? part.getCharset() : UTF_8));
      visitor.withByte(QUOTE_BYTE);
    }
  }
}
