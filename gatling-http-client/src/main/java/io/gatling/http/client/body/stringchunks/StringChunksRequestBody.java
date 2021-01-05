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

package io.gatling.http.client.body.stringchunks;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.gatling.netty.util.StringWithCachedBytes;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.Charset;
import java.util.List;

public final class StringChunksRequestBody extends RequestBody.Base<List<StringWithCachedBytes>> {

  private final Charset charset;
  private final long contentLength;

  public StringChunksRequestBody(List<StringWithCachedBytes> content, String contentType, Charset charset) {
    super(content, contentType);
    long contentLength = 0;
    for (StringWithCachedBytes stringWithCachedBytes : content) {
      contentLength += stringWithCachedBytes.bytes.length;
    }
    this.contentLength = contentLength;
    this.charset = charset;
  }

  @Override
  public WritableContent build(ByteBufAllocator alloc) {
    return new WritableContent(StringWithCachedBytes.toByteBuf(content), contentLength);
  }

  @Override
  public RequestBodyBuilder newBuilder() {
    return new StringChunksRequestBodyBuilder(content);
  }

  @Override
  public byte[] getBytes() {
    byte[] bytes = new byte[(int) contentLength];
    int offset = 0;
    for (StringWithCachedBytes chunk: content) {
      System.arraycopy(chunk.bytes, 0, bytes, offset, chunk.bytes.length);
      offset += chunk.bytes.length;
    }
    return bytes;
  }

  @Override
  public String toString() {
    return "StringChunksRequestBody{" +
      "contentType='" + contentType + '\'' +
      ", charset=" + charset +
      ", content=" + StringWithCachedBytes.toString(content) +
      '}';
  }
}
