/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.netty.handler.codec.http.websocketx;

import io.gatling.http.client.impl.DefaultHttpClient;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * <p>
 * Performs client side opening and closing handshakes for web socket specification version <a
 * href="http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17" >draft-ietf-hybi-thewebsocketprotocol-
 * 17</a>
 * </p>
 */
public class FixedWebSocketClientHandshaker13 {

  private static final InternalLogger logger = InternalLoggerFactory.getInstance(FixedWebSocketClientHandshaker13.class);

  public static final String MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private String expectedChallengeResponseString;

  private final boolean allowExtensions;
  private final boolean performMasking;
  private final boolean allowMaskMismatch;

  ////////////////////////////////////// W

  private static final String HTTP_SCHEME_PREFIX = HttpScheme.HTTP + "://";
  private static final String HTTPS_SCHEME_PREFIX = HttpScheme.HTTPS + "://";
  protected static final int DEFAULT_FORCE_CLOSE_TIMEOUT_MILLIS = 10000;

  private final URI uri;

  private final WebSocketVersion version;

  private volatile boolean handshakeComplete;

  private volatile long forceCloseTimeoutMillis = DEFAULT_FORCE_CLOSE_TIMEOUT_MILLIS;

  private volatile int forceCloseInit;

  private static final AtomicIntegerFieldUpdater<FixedWebSocketClientHandshaker13> FORCE_CLOSE_INIT_UPDATER =
    AtomicIntegerFieldUpdater.newUpdater(FixedWebSocketClientHandshaker13.class, "forceCloseInit");

  private volatile boolean forceCloseComplete;

  private final String expectedSubprotocol;

  private volatile String actualSubprotocol;

  protected final HttpHeaders customHeaders;

  private final int maxFramePayloadLength;

  private final boolean absoluteUpgradeUrl;

  /**
   * Returns the URI to the web socket. e.g. "ws://myhost.com/path"
   */
  public URI uri() {
    return uri;
  }

  /**
   * Version of the web socket specification that is being used
   */
  public WebSocketVersion version() {
    return version;
  }

  /**
   * Returns the max length for any frame's payload
   */
  public int maxFramePayloadLength() {
    return maxFramePayloadLength;
  }

  /**
   * Flag to indicate if the opening handshake is complete
   */
  public boolean isHandshakeComplete() {
    return handshakeComplete;
  }

  private void setHandshakeComplete() {
    handshakeComplete = true;
  }

  /**
   * Returns the CSV of requested subprotocol(s) sent to the server as specified in the constructor
   */
  public String expectedSubprotocol() {
    return expectedSubprotocol;
  }

  /**
   * Returns the subprotocol response sent by the server. Only available after end of handshake.
   * Null if no subprotocol was requested or confirmed by the server.
   */
  public String actualSubprotocol() {
    return actualSubprotocol;
  }

  private void setActualSubprotocol(String actualSubprotocol) {
    this.actualSubprotocol = actualSubprotocol;
  }

  public long forceCloseTimeoutMillis() {
    return forceCloseTimeoutMillis;
  }

  /**
   * Flag to indicate if the closing handshake was initiated because of timeout.
   * For testing only.
   */
  protected boolean isForceCloseComplete() {
    return forceCloseComplete;
  }

  /**
   * Sets timeout to close the connection if it was not closed by the server.
   *
   * @param forceCloseTimeoutMillis
   *            Close the connection if it was not closed by the server after timeout specified
   */
  public FixedWebSocketClientHandshaker13 setForceCloseTimeoutMillis(long forceCloseTimeoutMillis) {
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
    return this;
  }

  /**
   * Begins the opening handshake
   *
   * @param channel
   *            Channel
   */
  public ChannelFuture handshake(Channel channel) {
    ObjectUtil.checkNotNull(channel, "channel");
    return handshake(channel, channel.newPromise());
  }

  /**
   * Begins the opening handshake
   *
   * @param channel
   *            Channel
   * @param promise
   *            the {@link ChannelPromise} to be notified when the opening handshake is sent
   */
  public final ChannelFuture handshake(Channel channel, final ChannelPromise promise) {
    ChannelPipeline pipeline = channel.pipeline();
    HttpResponseDecoder decoder = pipeline.get(HttpResponseDecoder.class);
    if (decoder == null) {
      // FIXME GATLING FORK: target correct HttpClientCodec by name instead of first from head
      // HttpClientCodec codec = pipeline.get(HttpClientCodec.class);
      ChannelHandler codec = pipeline.get(DefaultHttpClient.HTTP_CLIENT_CODEC);
      if (codec == null) {
        promise.setFailure(new IllegalStateException("ChannelPipeline does not contain " +
          "an HttpResponseDecoder or HttpClientCodec"));
        return promise;
      }
    }

    FullHttpRequest request = newHandshakeRequest();

    channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          ChannelPipeline p = future.channel().pipeline();
          ChannelHandlerContext ctx = p.context(HttpRequestEncoder.class);
          if (ctx == null) {
            // FIXME GATLING FORK: target correct HttpClientCodec by name instead of first from head
            // ctx = p.context(HttpClientCodec.class);
            ctx = p.context(DefaultHttpClient.HTTP_CLIENT_CODEC);
          }
          if (ctx == null) {
            promise.setFailure(new IllegalStateException("ChannelPipeline does not contain " +
              "an HttpRequestEncoder or HttpClientCodec"));
            return;
          }
          p.addAfter(ctx.name(), "ws-encoder", newWebSocketEncoder());

          promise.setSuccess();
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
    return promise;
  }

  /**
   * Validates and finishes the opening handshake initiated by {@link #handshake}}.
   *
   * @param channel
   *            Channel
   * @param response
   *            HTTP response containing the closing handshake details
   */
  public final void finishHandshake(Channel channel, FullHttpResponse response) {
    verify(response);

    // Verify the subprotocol that we received from the server.
    // This must be one of our expected subprotocols - or null/empty if we didn't want to speak a subprotocol
    String receivedProtocol = response.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
    receivedProtocol = receivedProtocol != null ? receivedProtocol.trim() : null;
    String expectedProtocol = expectedSubprotocol != null ? expectedSubprotocol : "";
    boolean protocolValid = false;

    if (expectedProtocol.isEmpty() && receivedProtocol == null) {
      // No subprotocol required and none received
      protocolValid = true;
      setActualSubprotocol(expectedSubprotocol); // null or "" - we echo what the user requested
    } else if (!expectedProtocol.isEmpty() && receivedProtocol != null && !receivedProtocol.isEmpty()) {
      // We require a subprotocol and received one -> verify it
      for (String protocol : expectedProtocol.split(",")) {
        if (protocol.trim().equals(receivedProtocol)) {
          protocolValid = true;
          setActualSubprotocol(receivedProtocol);
          break;
        }
      }
    } // else mixed cases - which are all errors

    if (!protocolValid) {
      throw new WebSocketHandshakeException(String.format(
        "Invalid subprotocol. Actual: %s. Expected one of: %s",
        receivedProtocol, expectedSubprotocol));
    }

    setHandshakeComplete();

    final ChannelPipeline p = channel.pipeline();
    // Remove decompressor from pipeline if its in use
    HttpContentDecompressor decompressor = p.get(HttpContentDecompressor.class);
    if (decompressor != null) {
      p.remove(decompressor);
    }

    // Remove aggregator if present before
    HttpObjectAggregator aggregator = p.get(HttpObjectAggregator.class);
    if (aggregator != null) {
      p.remove(aggregator);
    }

    ChannelHandlerContext ctx = p.context(HttpResponseDecoder.class);
    if (ctx == null) {
      // FIXME GATLING FORK: target correct HttpClientCodec by name instead of first from head
      // ctx = p.context(HttpClientCodec.class);
      ctx = p.context(DefaultHttpClient.HTTP_CLIENT_CODEC);
      if (ctx == null) {
        throw new IllegalStateException("ChannelPipeline does not contain " +
          "an HttpRequestEncoder or HttpClientCodec");
      }
      final HttpClientCodec codec =  (HttpClientCodec) ctx.handler();
      // Remove the encoder part of the codec as the user may start writing frames after this method returns.
      codec.removeOutboundHandler();

      p.addAfter(ctx.name(), "ws-decoder", newWebsocketDecoder());

      // Delay the removal of the decoder so the user can setup the pipeline if needed to handle
      // WebSocketFrame messages.
      // See https://github.com/netty/netty/issues/4533
      channel.eventLoop().execute(new Runnable() {
        @Override
        public void run() {
          p.remove(codec);
        }
      });
    } else {
      if (p.get(HttpRequestEncoder.class) != null) {
        // Remove the encoder part of the codec as the user may start writing frames after this method returns.
        p.remove(HttpRequestEncoder.class);
      }
      final ChannelHandlerContext context = ctx;
      p.addAfter(context.name(), "ws-decoder", newWebsocketDecoder());

      // Delay the removal of the decoder so the user can setup the pipeline if needed to handle
      // WebSocketFrame messages.
      // See https://github.com/netty/netty/issues/4533
      channel.eventLoop().execute(new Runnable() {
        @Override
        public void run() {
          p.remove(context.handler());
        }
      });
    }
  }

  /**
   * Process the opening handshake initiated by {@link #handshake}}.
   *
   * @param channel
   *            Channel
   * @param response
   *            HTTP response containing the closing handshake details
   * @return future
   *            the {@link ChannelFuture} which is notified once the handshake completes.
   */
  public final ChannelFuture processHandshake(final Channel channel, HttpResponse response) {
    return processHandshake(channel, response, channel.newPromise());
  }

  /**
   * Process the opening handshake initiated by {@link #handshake}}.
   *
   * @param channel
   *            Channel
   * @param response
   *            HTTP response containing the closing handshake details
   * @param promise
   *            the {@link ChannelPromise} to notify once the handshake completes.
   * @return future
   *            the {@link ChannelFuture} which is notified once the handshake completes.
   */
  public final ChannelFuture processHandshake(final Channel channel, HttpResponse response,
                                              final ChannelPromise promise) {
    if (response instanceof FullHttpResponse) {
      try {
        finishHandshake(channel, (FullHttpResponse) response);
        promise.setSuccess();
      } catch (Throwable cause) {
        promise.setFailure(cause);
      }
    } else {
      ChannelPipeline p = channel.pipeline();
      ChannelHandlerContext ctx = p.context(HttpResponseDecoder.class);
      if (ctx == null) {
        // FIXME GATLING FORK: target correct HttpClientCodec by name instead of first from head
        // ctx = p.context(HttpClientCodec.class);
        ctx = p.context(DefaultHttpClient.HTTP_CLIENT_CODEC);
        if (ctx == null) {
          return promise.setFailure(new IllegalStateException("ChannelPipeline does not contain " +
            "an HttpResponseDecoder or HttpClientCodec"));
        }
      }
      // Add aggregator and ensure we feed the HttpResponse so it is aggregated. A limit of 8192 should be more
      // then enough for the websockets handshake payload.
      //
      // TODO: Make handshake work without HttpObjectAggregator at all.
      String aggregatorName = "httpAggregator";
      p.addAfter(ctx.name(), aggregatorName, new HttpObjectAggregator(8192));
      p.addAfter(aggregatorName, "handshaker", new SimpleChannelInboundHandler<FullHttpResponse>() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
          // Remove ourself and do the actual handshake
          ctx.pipeline().remove(this);
          try {
            finishHandshake(channel, msg);
            promise.setSuccess();
          } catch (Throwable cause) {
            promise.setFailure(cause);
          }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          // Remove ourself and fail the handshake promise.
          ctx.pipeline().remove(this);
          promise.setFailure(cause);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
          // Fail promise if Channel was closed
          if (!promise.isDone()) {
            promise.tryFailure(new ClosedChannelException());
          }
          ctx.fireChannelInactive();
        }
      });
      try {
        ctx.fireChannelRead(ReferenceCountUtil.retain(response));
      } catch (Throwable cause) {
        promise.setFailure(cause);
      }
    }
    return promise;
  }

  /**
   * Performs the closing handshake
   *
   * @param channel
   *            Channel
   * @param frame
   *            Closing Frame that was received
   */
  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame) {
    ObjectUtil.checkNotNull(channel, "channel");
    return close(channel, frame, channel.newPromise());
  }

  /**
   * Performs the closing handshake
   *
   * @param channel
   *            Channel
   * @param frame
   *            Closing Frame that was received
   * @param promise
   *            the {@link ChannelPromise} to be notified when the closing handshake is done
   */
  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame, ChannelPromise promise) {
    ObjectUtil.checkNotNull(channel, "channel");
    channel.writeAndFlush(frame, promise);
    applyForceCloseTimeout(channel, promise);
    return promise;
  }

  private void applyForceCloseTimeout(final Channel channel, ChannelFuture flushFuture) {
    final long forceCloseTimeoutMillis = this.forceCloseTimeoutMillis;
    final FixedWebSocketClientHandshaker13 handshaker = this;
    if (forceCloseTimeoutMillis <= 0 || !channel.isActive() || forceCloseInit != 0) {
      return;
    }

    flushFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        // If flush operation failed, there is no reason to expect
        // a server to receive CloseFrame. Thus this should be handled
        // by the application separately.
        // Also, close might be called twice from different threads.
        if (future.isSuccess() && channel.isActive() &&
          FORCE_CLOSE_INIT_UPDATER.compareAndSet(handshaker, 0, 1)) {
          final Future<?> forceCloseFuture = channel.eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
              if (channel.isActive()) {
                channel.close();
                forceCloseComplete = true;
              }
            }
          }, forceCloseTimeoutMillis, TimeUnit.MILLISECONDS);

          channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
              forceCloseFuture.cancel(false);
            }
          });
        }
      }
    });
  }

  /**
   * Return the constructed raw path for the give {@link URI}.
   */
  protected String upgradeUrl(URI wsURL) {
    if (absoluteUpgradeUrl) {
      return wsURL.toString();
    }

    String path = wsURL.getRawPath();
    path = path == null || path.isEmpty() ? "/" : path;
    String query = wsURL.getRawQuery();
    return query != null && !query.isEmpty() ? path + '?' + query : path;
  }

  static CharSequence websocketHostValue(URI wsURL) {
    int port = wsURL.getPort();
    if (port == -1) {
      return wsURL.getHost();
    }
    String host = wsURL.getHost();
    String scheme = wsURL.getScheme();
    if (port == HttpScheme.HTTP.port()) {
      return HttpScheme.HTTP.name().contentEquals(scheme)
        || WebSocketScheme.WS.name().contentEquals(scheme) ?
        host : NetUtil.toSocketAddressString(host, port);
    }
    if (port == HttpScheme.HTTPS.port()) {
      return HttpScheme.HTTPS.name().contentEquals(scheme)
        || WebSocketScheme.WSS.name().contentEquals(scheme) ?
        host : NetUtil.toSocketAddressString(host, port);
    }

    // if the port is not standard (80/443) its needed to add the port to the header.
    // See http://tools.ietf.org/html/rfc6454#section-6.2
    return NetUtil.toSocketAddressString(host, port);
  }

  static CharSequence websocketOriginValue(URI wsURL) {
    String scheme = wsURL.getScheme();
    final String schemePrefix;
    int port = wsURL.getPort();
    final int defaultPort;
    if (WebSocketScheme.WSS.name().contentEquals(scheme)
      || HttpScheme.HTTPS.name().contentEquals(scheme)
      || (scheme == null && port == WebSocketScheme.WSS.port())) {

      schemePrefix = HTTPS_SCHEME_PREFIX;
      defaultPort = WebSocketScheme.WSS.port();
    } else {
      schemePrefix = HTTP_SCHEME_PREFIX;
      defaultPort = WebSocketScheme.WS.port();
    }

    // Convert uri-host to lower case (by RFC 6454, chapter 4 "Origin of a URI")
    String host = wsURL.getHost().toLowerCase(Locale.US);

    if (port != defaultPort && port != -1) {
      // if the port is not standard (80/443) its needed to add the port to the header.
      // See http://tools.ietf.org/html/rfc6454#section-6.2
      return schemePrefix + NetUtil.toSocketAddressString(host, port);
    }
    return schemePrefix + host;
  }

  //////////////////////////////////////

  /**
   * Creates a new instance.
   *
   * @param webSocketURL
   *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
   *            sent to this URL.
   * @param version
   *            Version of web socket specification to use to connect to the server
   * @param subprotocol
   *            Sub protocol request sent to the server.
   * @param allowExtensions
   *            Allow extensions to be used in the reserved bits of the web socket frame
   * @param customHeaders
   *            Map of custom headers to add to the client request
   * @param maxFramePayloadLength
   *            Maximum length of a frame's payload
   * @param performMasking
   *            Whether to mask all written websocket frames. This must be set to true in order to be fully compatible
   *            with the websocket specifications. Client applications that communicate with a non-standard server
   *            which doesn't require masking might set this to false to achieve a higher performance.
   * @param allowMaskMismatch
   *            When set to true, frames which are not masked properly according to the standard will still be
   *            accepted
   * @param forceCloseTimeoutMillis
   *            Close the connection if it was not closed by the server after timeout specified.
   * @param  absoluteUpgradeUrl
   *            Use an absolute url for the Upgrade request, typically when connecting through an HTTP proxy over
   *            clear HTTP
   */
  public FixedWebSocketClientHandshaker13(URI webSocketURL, WebSocketVersion version, String subprotocol,
                              boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength,
                              boolean performMasking, boolean allowMaskMismatch,
                              long forceCloseTimeoutMillis, boolean absoluteUpgradeUrl) {
    this.uri = webSocketURL;
    this.version = version;
    expectedSubprotocol = subprotocol;
    this.customHeaders = customHeaders;
    this.maxFramePayloadLength = maxFramePayloadLength;
    this.forceCloseTimeoutMillis = forceCloseTimeoutMillis;
    this.absoluteUpgradeUrl = absoluteUpgradeUrl;
    this.allowExtensions = allowExtensions;
    this.performMasking = performMasking;
    this.allowMaskMismatch = allowMaskMismatch;
  }

  /**
   * /**
   * <p>
   * Sends the opening request to the server:
   * </p>
   *
   * <pre>
   * GET /chat HTTP/1.1
   * Host: server.example.com
   * Upgrade: websocket
   * Connection: Upgrade
   * Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
   * Origin: http://example.com
   * Sec-WebSocket-Protocol: chat, superchat
   * Sec-WebSocket-Version: 13
   * </pre>
   *
   */
  private FullHttpRequest newHandshakeRequest() {
    URI wsURL = uri();

    // Get 16 bit nonce and base 64 encode it
    byte[] nonce = WebSocketUtil.randomBytes(16);
    String key = WebSocketUtil.base64(nonce);

    String acceptSeed = key + MAGIC_GUID;
    byte[] sha1 = WebSocketUtil.sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
    expectedChallengeResponseString = WebSocketUtil.base64(sha1);

    if (logger.isDebugEnabled()) {
      logger.debug(
        "WebSocket version 13 client handshake key: {}, expected response: {}",
        key, expectedChallengeResponseString);
    }

    // Format request
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, upgradeUrl(wsURL),
      Unpooled.EMPTY_BUFFER);
    HttpHeaders headers = request.headers();

    if (customHeaders != null) {
      headers.add(customHeaders);
    }

    headers.set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET)
      .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE)
      .set(HttpHeaderNames.SEC_WEBSOCKET_KEY, key)
      .set(HttpHeaderNames.HOST, websocketHostValue(wsURL));

    if (!headers.contains(HttpHeaderNames.ORIGIN)) {
      headers.set(HttpHeaderNames.ORIGIN, websocketOriginValue(wsURL));
    }

    String expectedSubprotocol = expectedSubprotocol();
    if (expectedSubprotocol != null && !expectedSubprotocol.isEmpty()) {
      headers.set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, expectedSubprotocol);
    }

    headers.set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "13");
    return request;
  }

  /**
   * <p>
   * Process server response:
   * </p>
   *
   * <pre>
   * HTTP/1.1 101 Switching Protocols
   * Upgrade: websocket
   * Connection: Upgrade
   * Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
   * Sec-WebSocket-Protocol: chat
   * </pre>
   *
   * @param response
   *            HTTP response returned from the server for the request sent by beginOpeningHandshake00().
   * @throws WebSocketHandshakeException if handshake response is invalid.
   */
  private void verify(FullHttpResponse response) {
    final HttpResponseStatus status = HttpResponseStatus.SWITCHING_PROTOCOLS;
    final HttpHeaders headers = response.headers();

    if (!response.status().equals(status)) {
      throw new WebSocketHandshakeException("Invalid handshake response getStatus: " + response.status());
    }

    CharSequence upgrade = headers.get(HttpHeaderNames.UPGRADE);
    if (!HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgrade)) {
      throw new WebSocketHandshakeException("Invalid handshake response upgrade: " + upgrade);
    }

    if (!headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
      throw new WebSocketHandshakeException("Invalid handshake response connection: "
        + headers.get(HttpHeaderNames.CONNECTION));
    }

    CharSequence accept = headers.get(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
    if (accept == null || !accept.equals(expectedChallengeResponseString)) {
      throw new WebSocketHandshakeException(String.format(
        "Invalid challenge. Actual: %s. Expected: %s", accept, expectedChallengeResponseString));
    }
  }

  private WebSocketFrameDecoder newWebsocketDecoder() {
    return new WebSocket13FrameDecoder(false, allowExtensions, maxFramePayloadLength(), allowMaskMismatch);
  }

  private WebSocketFrameEncoder newWebSocketEncoder() {
    return new WebSocket13FrameEncoder(performMasking);
  }
}
