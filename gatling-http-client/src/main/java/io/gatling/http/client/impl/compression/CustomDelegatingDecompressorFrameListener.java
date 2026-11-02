/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.impl.compression;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.function.IntPredicate;

/**
 * Extend {@link DelegatingDecompressorFrameListener} in order to:
 *
 * <ul>
 *   <li>retain the content-encoding header
 *   <li>skip decompression for streams whose response body is not used
 * </ul>
 */
public final class CustomDelegatingDecompressorFrameListener
    extends DelegatingDecompressorFrameListener {

  private IntPredicate decompressionRequired = streamId -> true;

  // set only for the duration of super.onHeadersRead, on the channel's event loop,
  // as newContentDecompressor doesn't get passed the streamId
  private boolean skipDecompression;

  public CustomDelegatingDecompressorFrameListener(
      Http2Connection connection, Http2FrameListener listener) {
    super(connection, listener, 0);
  }

  public void setDecompressionRequired(IntPredicate decompressionRequired) {
    this.decompressionRequired = decompressionRequired;
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream)
      throws Http2Exception {
    skipDecompression = !decompressionRequired.test(streamId);
    try {
      super.onHeadersRead(ctx, streamId, headers, padding, endStream);
    } finally {
      skipDecompression = false;
    }
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx,
      int streamId,
      Http2Headers headers,
      int streamDependency,
      short weight,
      boolean exclusive,
      int padding,
      boolean endStream)
      throws Http2Exception {
    skipDecompression = !decompressionRequired.test(streamId);
    try {
      super.onHeadersRead(
          ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
    } finally {
      skipDecompression = false;
    }
  }

  @Override
  protected EmbeddedChannel newContentDecompressor(
      ChannelHandlerContext ctx, CharSequence contentEncoding) throws Http2Exception {
    // returning null makes DelegatingDecompressorFrameListener pass the DATA frames through
    // untouched
    return skipDecompression ? null : super.newContentDecompressor(ctx, contentEncoding);
  }

  @Override
  protected CharSequence getTargetContentEncoding(CharSequence contentEncoding) {
    return contentEncoding;
  }
}
