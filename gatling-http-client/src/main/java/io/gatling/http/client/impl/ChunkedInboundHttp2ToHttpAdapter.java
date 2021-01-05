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

package io.gatling.http.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.util.concurrent.Promise;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http2.Http2Error.PROTOCOL_ERROR;
import static io.netty.handler.codec.http2.Http2Exception.connectionError;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class ChunkedInboundHttp2ToHttpAdapter extends Http2EventAdapter {

  private final boolean propagateSettings;
  private final Http2Connection connection;
  private final boolean validateHttpHeaders;
  private final Promise<Channel> whenHttp2Handshake;

  ChunkedInboundHttp2ToHttpAdapter(Http2Connection connection,
                                          boolean validateHttpHeaders,
                                          boolean propagateSettings,
                                          Promise<Channel> whenHttp2Handshake) {

    checkNotNull(connection, "connection");
    this.connection = connection;
    this.validateHttpHeaders = validateHttpHeaders;
    this.propagateSettings = propagateSettings;
    this.whenHttp2Handshake = whenHttp2Handshake;
  }

  @Override
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
    throws Http2Exception {
    Http2Stream stream = connection.stream(streamId);

    if (stream == null) {
      throw connectionError(PROTOCOL_ERROR, "Data Frame received for unknown stream id " + streamId);
    }

    final int processedBytes = data.readableBytes();

    HttpContent content = endOfStream ? new DefaultLastHttpContent(data) : new DefaultHttpContent(data);
    ctx.fireChannelRead(new Http2Content(content, streamId));
    return processedBytes + padding;
  }

  private void convertAndFire(ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean endOfStream) throws Http2Exception {
    HttpResponse response = HttpConversionUtil.toHttpResponse(streamId, headers, validateHttpHeaders);
    ctx.fireChannelRead(response);
    if (endOfStream) {
      ctx.fireChannelRead(new Http2Content(LastHttpContent.EMPTY_LAST_CONTENT, streamId));
    }
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
                            boolean endOfStream) throws Http2Exception {
    convertAndFire(ctx, streamId, headers, endOfStream);
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                            short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
    onHeadersRead(ctx, streamId, headers, padding, endOfStream);
  }

  @Override
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
    ctx.fireExceptionCaught(Http2Exception.streamError(streamId, Http2Error.valueOf(errorCode),
      "HTTP/2 to HTTP layer caught stream reset"));
  }

  @Override
  public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                                Http2Headers headers, int padding) throws Http2Exception {
    if (connection.stream(promisedStreamId) != null)
      throw connectionError(PROTOCOL_ERROR, "Push Promise Frame received for pre-existing stream id %d",
        promisedStreamId);

    if (headers.status() == null)
      headers.status(OK.codeAsText());

    convertAndFire(ctx, streamId, headers, true);
  }

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {

    if (!whenHttp2Handshake.isDone()) {
      whenHttp2Handshake.setSuccess(ctx.channel());
    }

    if (propagateSettings) {
      ctx.fireChannelRead(settings);
    }
  }
}
