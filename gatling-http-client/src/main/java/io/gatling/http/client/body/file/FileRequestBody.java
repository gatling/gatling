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

package io.gatling.http.client.body.file;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.stream.ChunkedFile;

import java.io.*;
import java.nio.charset.Charset;

public final class FileRequestBody extends RequestBody.Base<File> {

  private final Charset charset;

  public FileRequestBody(File content, String contentType, Charset charset) {
    super(content, contentType);
    this.charset = charset;
  }

  @Override
  public WritableContent build(ByteBufAllocator alloc) throws IOException {

    long contentLength = content.length();

    Object file = new ChunkedFile(content);

    return new WritableContent(file, contentLength);
  }

  @Override
  public RequestBodyBuilder newBuilder() {
    return new FileRequestBodyBuilder(content);
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public byte[] getBytes() {
    byte[] bytes = new byte[(int) content.length()];
    try (InputStream is = new FileInputStream(content)) {
      is.read(bytes);
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't read file", e);
    }

    return bytes;
  }

  public Charset getCharset() {
    return charset;
  }

  @Override
  public String toString() {
    return "FileRequestBody{" +
      "contentType='" + contentType + '\'' +
      ", charset=" + charset +
      ", content=" + content +
      '}';
  }
}
