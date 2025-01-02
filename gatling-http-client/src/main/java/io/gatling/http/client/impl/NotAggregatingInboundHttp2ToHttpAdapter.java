/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.impl;

import static io.netty.handler.codec.http2.Http2Error.PROTOCOL_ERROR;
import static io.netty.handler.codec.http2.Http2Exception.connectionError;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.util.concurrent.Promise;

/**
 * Standard {@link InboundHttp2ToHttpAdapter} generates {@link FullHttpResponse}. This is not what
 * we want.
 */
public final class NotAggregatingInboundHttp2ToHttpAdapter extends Http2EventAdapter {

  private final Http2Connection connection;
  private final Promise<Void> whenAlpn;

  NotAggregatingInboundHttp2ToHttpAdapter(Http2Connection connection, Promise<Void> whenAlpn) {

    checkNotNull(connection, "connection");
    this.connection = connection;
    this.whenAlpn = whenAlpn;
  }

  @Override
  public int onDataRead(
      ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
      throws Http2Exception {
    Http2Stream stream = connection.stream(streamId);

    if (stream == null) {
      throw connectionError(
          PROTOCOL_ERROR, "Data Frame received for unknown stream id " + streamId);
    }

    final int processedBytes = data.readableBytes();

    ctx.fireChannelRead(new Http2Content(new DefaultHttpContent(data), streamId, endOfStream));
    return processedBytes + padding;
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx,
      int streamId,
      Http2Headers headers,
      int padding,
      boolean endOfStream)
      throws Http2Exception {
    HttpResponse response = HttpConversionUtil.toHttpResponse(streamId, headers, false);
    ctx.fireChannelRead(response);
    if (endOfStream) {
      ctx.fireChannelRead(new Http2Content(LastHttpContent.EMPTY_LAST_CONTENT, streamId, true));
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
      boolean endOfStream)
      throws Http2Exception {
    onHeadersRead(ctx, streamId, headers, padding, endOfStream);
  }

  @Override
  public void onGoAwayRead(
      ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
    ctx.fireUserEventTriggered(new Http2AppHandler.GoAwayFrame(lastStreamId, errorCode));
  }

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
    if (!whenAlpn.isDone()) {
      whenAlpn.setSuccess(null);
    }
  }
}
