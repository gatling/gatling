/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.impl.chunk;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Several optimizations on ChunkedWriteHandler:
 *
 * <ul>
 *   <li>lazily allocate queue
 *   <li>write doesn't push in the queue unless the queue in not empty or the message is a
 *       ChunkedInput
 *   <li>save extra chunks cast
 *   <li>simplified logging
 *   <li>simplified boolean logic
 *   <li>Gatling doesn't use ChannelProgressivePromise
 * </ul>
 */
public class ForkedChunkedWriteHandler extends ChannelDuplexHandler {

  private static final InternalLogger logger =
      InternalLoggerFactory.getInstance(ChunkedWriteHandler.class);

  private Queue<ForkedChunkedWriteHandler.PendingWrite> queue;
  private volatile ChannelHandlerContext ctx;

  private Queue<ForkedChunkedWriteHandler.PendingWrite> getAllocatedQueue() {
    if (queue == null) {
      queue = new ArrayDeque<>();
    }
    return queue;
  }

  private boolean isQueueNonEmpty() {
    return queue != null && !queue.isEmpty();
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  /** Continues to fetch the chunks from the input. */
  public void resumeTransfer() {
    final ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      return;
    }
    if (ctx.executor().inEventLoop()) {
      resumeTransfer0(ctx);
    } else {
      // let the transfer resume on the next event loop round
      ctx.executor().execute(() -> resumeTransfer0(ctx));
    }
  }

  private void resumeTransfer0(ChannelHandlerContext ctx) {
    try {
      doFlush(ctx);
    } catch (Exception e) {
      logger.warn("Unexpected exception while sending chunks.", e);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (isQueueNonEmpty() || msg instanceof ChunkedInput) {
      getAllocatedQueue().add(new ForkedChunkedWriteHandler.PendingWrite(msg, promise));
    } else {
      ctx.write(msg);
    }
  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    if (isQueueNonEmpty()) {
      doFlush(ctx);
    } else {
      ctx.flush();
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (isQueueNonEmpty()) {
      doFlush(ctx);
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) {
    if (isQueueNonEmpty() && ctx.channel().isWritable()) {
      // channel is writable again try to continue flushing
      doFlush(ctx);
    }
    ctx.fireChannelWritabilityChanged();
  }

  private void discard(Throwable cause) {
    if (queue != null && !queue.isEmpty()) {
      for (; ; ) {
        ForkedChunkedWriteHandler.PendingWrite currentWrite = queue.poll();

        if (currentWrite == null) {
          break;
        }
        Object message = currentWrite.msg;
        if (message instanceof ChunkedInput) {
          ChunkedInput<?> in = (ChunkedInput<?>) message;
          boolean endOfInput;
          try {
            endOfInput = in.isEndOfInput();
            closeInput(in);
          } catch (Exception e) {
            closeInput(in);
            currentWrite.fail(e);
            logger.warn("ChunkedInput failed", e);
            continue;
          }

          if (!endOfInput) {
            if (cause == null) {
              cause = new ClosedChannelException();
            }
            currentWrite.fail(cause);
          } else {
            currentWrite.success();
          }
        } else {
          if (cause == null) {
            cause = new ClosedChannelException();
          }
          currentWrite.fail(cause);
        }
      }
    }
  }

  private void doFlush(final ChannelHandlerContext ctx) {
    final Channel channel = ctx.channel();
    if (!channel.isActive()) {
      discard(null);
      return;
    }

    boolean requiresFlush = true;
    ByteBufAllocator allocator = ctx.alloc();
    while (channel.isWritable()) {
      final ForkedChunkedWriteHandler.PendingWrite currentWrite = queue.peek();

      if (currentWrite == null) {
        break;
      }

      if (currentWrite.promise.isDone()) {
        // This might happen e.g. in the case when a write operation
        // failed, but there are still unconsumed chunks left.
        // Most chunked input sources would stop generating chunks
        // and report end of input, but this doesn't work with any
        // source wrapped in HttpChunkedInput.
        // Note, that we're not trying to release the message/chunks
        // as this had to be done already by someone who resolved the
        // promise (using ChunkedInput.close method).
        // See https://github.com/netty/netty/issues/8700.
        queue.remove();
        continue;
      }

      final Object pendingMessage = currentWrite.msg;

      if (pendingMessage instanceof ChunkedInput) {
        final ChunkedInput<?> chunks = (ChunkedInput<?>) pendingMessage;
        boolean endOfInput;
        boolean suspend;
        Object message = null;
        try {
          message = chunks.readChunk(allocator);
          endOfInput = chunks.isEndOfInput();
          // No need to suspend when reached at the end.
          suspend = message == null && !endOfInput;

        } catch (final Throwable t) {
          queue.remove();

          if (message != null) {
            ReferenceCountUtil.release(message);
          }

          closeInput(chunks);
          currentWrite.fail(t);
          break;
        }

        if (suspend) {
          // ChunkedInput.nextChunk() returned null and it has
          // not reached at the end of input. Let's wait until
          // more chunks arrive. Nothing to write or notify.
          break;
        }

        if (message == null) {
          // If message is null write an empty ByteBuf.
          // See https://github.com/netty/netty/issues/1671
          message = Unpooled.EMPTY_BUFFER;
        }

        if (endOfInput) {
          // We need to remove the element from the queue before we call writeAndFlush() as this
          // operation
          // may cause an action that also touches the queue.
          queue.remove();
        }
        // Flush each chunk to conserve memory
        ChannelFuture f = ctx.writeAndFlush(message);
        if (endOfInput) {
          if (f.isDone()) {
            handleEndOfInputFuture(f, chunks, currentWrite);
          } else {
            // Register a listener which will close the input once the write is complete.
            // This is needed because the Chunk may have some resource bound that can not
            // be closed before it's not written.
            //
            // See https://github.com/netty/netty/issues/303
            f.addListener(
                (ChannelFutureListener)
                    future -> handleEndOfInputFuture(future, chunks, currentWrite));
          }
        } else {
          final boolean resume = !channel.isWritable();
          if (f.isDone()) {
            handleFuture(f, chunks, currentWrite, resume);
          } else {
            f.addListener(
                (ChannelFutureListener)
                    future -> handleFuture(future, chunks, currentWrite, resume));
          }
        }
        requiresFlush = false;
      } else {
        queue.remove();
        ctx.write(pendingMessage, currentWrite.promise);
        requiresFlush = true;
      }

      if (!channel.isActive()) {
        discard(new ClosedChannelException());
        break;
      }
    }

    if (requiresFlush) {
      ctx.flush();
    }
  }

  private static void handleEndOfInputFuture(
      ChannelFuture future,
      ChunkedInput<?> input,
      ForkedChunkedWriteHandler.PendingWrite currentWrite) {
    if (!future.isSuccess()) {
      closeInput(input);
      currentWrite.fail(future.cause());
    } else {
      // read state of the input in local variables before closing it
      closeInput(input);
      currentWrite.success();
    }
  }

  private void handleFuture(
      ChannelFuture future,
      ChunkedInput<?> input,
      ForkedChunkedWriteHandler.PendingWrite currentWrite,
      boolean resume) {
    if (!future.isSuccess()) {
      closeInput(input);
      currentWrite.fail(future.cause());
    } else {
      if (resume && future.channel().isWritable()) {
        resumeTransfer();
      }
    }
  }

  private static void closeInput(ChunkedInput<?> chunks) {
    try {
      chunks.close();
    } catch (Throwable t) {
      logger.warn("Failed to close a ChunkedInput.", t);
    }
  }

  private static final class PendingWrite {
    final Object msg;
    final ChannelPromise promise;

    PendingWrite(Object msg, ChannelPromise promise) {
      this.msg = msg;
      this.promise = promise;
    }

    void fail(Throwable cause) {
      ReferenceCountUtil.release(msg);
      promise.tryFailure(cause);
    }

    void success() {
      if (promise.isDone()) {
        // No need to notify the progress or fulfill the promise because it's done already.
        return;
      }
      promise.trySuccess();
    }
  }
}
