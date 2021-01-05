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

package io.gatling.http.client.body.bytearray;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.Base64;

public final class ByteArrayRequestBody extends RequestBody.Base<byte[]> {

  private final String fileName;
  private final Charset charset;

  public ByteArrayRequestBody(byte[] content, String contentType, String fileName, Charset charset) {
    super(content, contentType);
    this.fileName = fileName;
    this.charset = charset;
  }

  @Override
  public WritableContent build(ByteBufAllocator alloc) {
    return new WritableContent(Unpooled.wrappedBuffer(content), content.length);
  }

  @Override
  public RequestBodyBuilder newBuilder() {
    return new ByteArrayRequestBodyBuilder(content, fileName);
  }

  @Override
  public byte[] getBytes() {
    return content;
  }

  public String getFileName() {
    return fileName;
  }

  public Charset getCharset() {
    return charset;
  }

  @Override
  public String toString() {
    return "ByteArrayRequestBody{" +
      "contentType='" + contentType + '\'' +
      ", content (Base64)=" + Base64.getEncoder().encodeToString(content) +
      '}';
  }
}
