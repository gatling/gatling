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

package io.gatling.http.client.body.string;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class StringRequestBody extends RequestBody.Base<String> {

  private final Charset charset;

  public StringRequestBody(String content, String contentType, Charset charset) {
    super(content, contentType);
    this.charset = charset;
  }

  @Override
  public WritableContent build(ByteBufAllocator alloc) {
    ByteBuf bb =
            charset.equals(StandardCharsets.UTF_8) ?
                    ByteBufUtil.writeUtf8(alloc, content) :
                    ByteBufUtil.encodeString(alloc, CharBuffer.wrap(content), charset);

    return new WritableContent(bb, bb.readableBytes());
  }

  @Override
  public RequestBodyBuilder newBuilder() {
    return new StringRequestBodyBuilder(content);
  }

  @Override
  public byte[] getBytes() {
    return content.getBytes(charset);
  }

  @Override
  public String toString() {
    return "StringRequestBody{" +
      "contentType='" + contentType + '\'' +
      ", charset=" + charset +
      ", content=" + content +
      '}';
  }
}
