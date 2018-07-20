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

package io.gatling.http.client.body.bytearray;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

public class ByteArrayRequestBody extends RequestBody<byte[]> {

  public ByteArrayRequestBody(byte[] content, String contentType, Charset charset) {
    super(content, contentType, charset);
  }

  @Override
  public WritableContent build(boolean zeroCopy, ByteBufAllocator alloc) {
    return new WritableContent(Unpooled.wrappedBuffer(content), content.length);
  }

  @Override
  public RequestBodyBuilder<byte[]> newBuilder() {
    return new ByteArrayRequestBodyBuilder(content);
  }
}
