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

package io.gatling.http.client.body.part.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class MessageEndPartImpl extends PartImpl {

  // lazy
  private ByteBuf contentBuffer;

  public MessageEndPartImpl(byte[] boundary) {
    super(null, boundary);
    state = PartImplState.PRE_CONTENT;
  }

  @Override
  public long copyInto(ByteBuf target) {
    return copyInto(lazyLoadContentBuffer(target.alloc()), target, PartImplState.DONE);
  }

  @Override
  public long transferTo(WritableByteChannel target) throws IOException {
    slowTarget = false;
    return transferTo(lazyLoadContentBuffer(ByteBufAllocator.DEFAULT), target, PartImplState.DONE);
  }

  private ByteBuf lazyLoadContentBuffer(ByteBufAllocator alloc) {
    if (contentBuffer == null) {
      contentBuffer = alloc.buffer((int) getContentLength());
      contentBuffer.writeBytes(EXTRA_BYTES).writeBytes(boundary).writeBytes(EXTRA_BYTES).writeBytes(CRLF_BYTES);
    }
    return contentBuffer;
  }

  @Override
  protected int computePreContentLength() {
    return 0;
  }

  @Override
  protected ByteBuf computePreContentBytes(int preContentLength) {
    return Unpooled.EMPTY_BUFFER;
  }

  @Override
  protected int computePostContentLength() {
    return 0;
  }

  @Override
  protected ByteBuf computePostContentBytes(int postContentLength) {
    return Unpooled.EMPTY_BUFFER;
  }

  @Override
  protected long getContentLength() {
    return EXTRA_BYTES.length + boundary.length + EXTRA_BYTES.length + CRLF_BYTES.length;
  }

  @Override
  protected long copyContentInto(ByteBuf target) {
    throw new UnsupportedOperationException("Not supposed to be called");
  }

  @Override
  protected long transferContentTo(WritableByteChannel target) {
    throw new UnsupportedOperationException("Not supposed to be called");
  }

  @Override
  public void close() {
    super.close();
    if (contentBuffer != null)
      contentBuffer.release();
  }
}
