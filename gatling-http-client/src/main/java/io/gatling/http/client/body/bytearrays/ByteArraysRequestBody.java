/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.client.body.bytearrays;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

public class ByteArraysRequestBody extends RequestBody<byte[][]> {

  private final long contentLength;

  public ByteArraysRequestBody(byte[][] content, String contentType, Charset charset) {
    super(content, contentType, charset);
    long contentLength = 0;
    for (byte[] bytes : content) {
      contentLength += bytes.length;
    }
    this.contentLength = contentLength;
  }

  @Override
  public WritableContent build(boolean zeroCopy, ByteBufAllocator alloc) {
    return new WritableContent(Unpooled.wrappedBuffer(content), contentLength);
  }

  @Override
  public RequestBodyBuilder<byte[][]> newBuilder() {
    return new ByteArraysRequestBodyBuilder(content);
  }

  @Override
  public byte[] getBytes() {
    byte[] bytes = new byte[(int) contentLength];
    int offset = 0;
    for (byte[] chunk: content) {
      System.arraycopy(chunk, 0, bytes, offset, chunk.length);
      offset += chunk.length;
    }
    return bytes;
  }

  @Override
  public String toString() {
    return "ByteArraysRequestBody{" +
      "content=<" + contentLength + " bytes>" +
      ", contentType=" + contentType +
      ", charset=" + charset +
      '}';
  }
}
