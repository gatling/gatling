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

package io.gatling.http.client.body;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

public class ByteArrayRequestBody extends RequestBody<byte[]> {
  public ByteArrayRequestBody(byte[] content) {
    super(content);
  }

  @Override
  public WritableContent build(String contentTypeHeader, Charset charset, boolean zeroCopy, ByteBufAllocator alloc) {
    return new WritableContent(Unpooled.wrappedBuffer(content), content.length, null);
  }
}
