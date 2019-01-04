/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import static io.gatling.http.client.ahc.util.MiscUtils.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultipartChunkedInput implements ChunkedInput<ByteBuf> {

  private enum ChunkedInputState {

    /**
     * There's something to read
     */
    CONTINUE,

    /**
     * There's nothing to read and input has to suspend
     */
    SUSPEND,

    /**
     * There's nothing to read and input has to stop
     */
    STOP
  }

  static final int DEFAULT_CHUNK_SIZE = 8 * 1024;

  private final List<PartImpl> parts;
  private final long contentLength;
  private final int chunkSize;
  private final AtomicBoolean closed = new AtomicBoolean();
  private boolean endOfInput;
  private long progress = 0L;
  private int currentPartIndex;
  private boolean done = false;

  public MultipartChunkedInput(List<PartImpl> parts, long contentLength) {
    this.parts = parts;
    this.contentLength = contentLength;
    this.chunkSize = contentLength > 0 ? (int) Math.min(contentLength, (long) DEFAULT_CHUNK_SIZE) : DEFAULT_CHUNK_SIZE;
  }

  @Override
  @Deprecated
  public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
    return readChunk(ctx.alloc());
  }

  @Override
  public ByteBuf readChunk(ByteBufAllocator alloc) throws Exception {

    if (endOfInput) {
      return null;
    }

    ByteBuf buffer = alloc.buffer(chunkSize);
    ChunkedInputState state = copyInto(buffer);
    progress += buffer.writerIndex();
    switch (state) {
      case STOP:
        endOfInput = true;
        return buffer;
      case SUSPEND:
        // this will suspend the stream in ChunkedWriteHandler
        buffer.release();
        return null;
      case CONTINUE:
        return buffer;
      default:
        throw new IllegalStateException("Unknown state: " + state);
    }
  }

  private ChunkedInputState copyInto(ByteBuf target) throws IOException {

    if (done) {
      return ChunkedInputState.STOP;
    }

    while (target.isWritable() && !done) {
      PartImpl currentPart = parts.get(currentPartIndex);
      currentPart.copyInto(target);

      if (currentPart.getState() == PartImplState.DONE) {
        currentPartIndex++;
        if (currentPartIndex == parts.size()) {
          done = true;
        }
      }
    }

    return ChunkedInputState.CONTINUE;
  }

  @Override
  public boolean isEndOfInput() {
    return endOfInput;
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      for (PartImpl part : parts) {
        closeSilently(part);
      }
    }
  }

  @Override
  public long length() {
    return contentLength;
  }

  @Override
  public long progress() {
    return progress;
  }
}
