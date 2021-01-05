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

import io.gatling.http.client.body.multipart.StringPart;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class StringPartImpl extends PartImpl {

  private final ByteBuf contentBuffer;

  public StringPartImpl(StringPart part, byte[] boundary) {
    super(part, boundary);
    contentBuffer = Unpooled.wrappedBuffer(part.getContent().getBytes(part.getCharset()));
  }

  @Override
  protected long getContentLength() {
    return contentBuffer.capacity();
  }

  @Override
  protected void copyContentInto(ByteBuf target) {
    copyInto(contentBuffer, target, PartImplState.POST_CONTENT);
  }

  @Override
  public void close() {
    super.close();
    contentBuffer.release();
  }
}
