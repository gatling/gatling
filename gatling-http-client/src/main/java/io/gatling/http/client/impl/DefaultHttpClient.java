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

package io.gatling.http.client.impl;

import io.gatling.http.client.*;
import io.gatling.http.client.impl.chunk.ForkedChunkedWriteHandler;
import io.gatling.http.client.impl.compression.CustomDelegatingDecompressorFrameListener;
import io.gatling.http.client.impl.compression.CustomHttpContentDecompressor;
import io.gatling.http.client.pool.ChannelPool;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.gatling.http.client.pool.RemoteKey;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyProtocolHandler;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.ssl.Tls;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.util.Pair;
import io.gatling.netty.util.Transports;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.*;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultHttpClient implements HttpClient {

  static Http2Settings initialHttp2Settings(Http2FlowControlWindows windows) {
    return Http2Settings.defaultSettings()
        // we don't support server push: without ENABLE_PUSH=0, servers are allowed to push
        .pushEnabled(false)
        .initialWindowSize(windows.getStreamWindowSize());
  }

  static {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpClient.class);

  private static final String PINNED_HANDLER = "pinned";
  private static final String PROXY_SSL_HANDLER = "ssl-proxy";
  private static final String PROXY_HANDLER = "proxy";
  private static final String PROXY_PROTOCOL_HANDLER = "proxy-protocol";
  private static final String SSL_HANDLER = "ssl";
  public static final String HTTP_CLIENT_CODEC = "http";
  private static final String HTTP2_HANDLER = "http2";
  private static final String HTTP2_CONTENT_ENCODER = "http2-content-encoder";
  private static final String INFLATER_HANDLER = "inflater";
  private static final String CHUNKED_WRITER_HANDLER = "chunked-writer";
  private static final String WS_OBJECT_AGGREGATOR = "ws-object-aggregator";
  private static final String WS_COMPRESSION = "ws-compression";
  private static final String WS_FRAME_AGGREGATOR = "ws-frame-aggregator";
  private static final String APP_WS_HANDLER = "app-ws";
  private static final String ALPN_HANDLER = "alpn";
  static final String APP_HTTP2_HANDLER = "app-http2";

  public static final String APP_HTTP_HANDLER = "app-http";

  private static final HttpDecoderConfig HTTP_DECODER_CONFIG =
      new HttpDecoderConfig().setMaxHeaderSize(Integer.MAX_VALUE).setValidateHeaders(false);

  private ChannelHandler newHttpClientCodec() {
    return new HttpClientCodec(HTTP_DECODER_CONFIG, false, false);
  }

  private final class EventLoopResources {

    private static final int POOL_CLEANER_PERIOD_MS = 1_000;

    private final Bootstrap http1Bootstrap;
    private final Bootstrap http2Bootstrap;
    private final Bootstrap wsBootstrap;
    private final ChannelPool channelPool;

    private void addHttpHandlers(Channel channel) {
      CustomHttpContentDecompressor decompressor = new CustomHttpContentDecompressor();
      channel
          .pipeline()
          .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
          .addLast(INFLATER_HANDLER, decompressor)
          .addLast(CHUNKED_WRITER_HANDLER, new ChunkedWriteHandler())
          .addLast(
              APP_HTTP_HANDLER,
              new HttpAppHandler(DefaultHttpClient.this, channelPool, decompressor));
    }

    private void addWsHandlers(Channel channel) {
      channel
          .pipeline()
          .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
          .addLast(WS_OBJECT_AGGREGATOR, new HttpObjectAggregator(Integer.MAX_VALUE))
          .addLast(WS_COMPRESSION, AllowClientNoContextWebSocketClientCompressionHandler.INSTANCE)
          .addLast(WS_FRAME_AGGREGATOR, new WebSocketFrameAggregator(Integer.MAX_VALUE))
          .addLast(APP_WS_HANDLER, new WebSocketHandler());
    }

    private void addProxyHandlers(Channel ch, HttpTx tx, ProxyServer proxyServer) {
      ChannelPipeline pipeline = ch.pipeline();
      pipeline.addLast(PINNED_HANDLER, NoopHandler.INSTANCE);

      if (proxyServer instanceof HttpProxyServer) {
        if (((HttpProxyServer) proxyServer).isSecured()) {
          installSslHandler(
                  tx,
                  tx.proxySslContext(),
                  ch,
                  proxyServer.getHost(),
                  proxyServer.getPort(),
                  PROXY_SSL_HANDLER)
              .addListener(
                  f -> {
                    if (tx.requestTimeout.isDone() || !f.isSuccess()) {
                      ch.close();
                    }
                  });
        }

        if (tx.request.getProxyProtocolSourceIpV4Address() != null
            || tx.request.getProxyProtocolSourceIpV6Address() != null) {
          pipeline.addLast(
              PROXY_PROTOCOL_HANDLER,
              new ProxyProtocolHandler(
                  tx.request.getProxyProtocolSourceIpV4Address(),
                  tx.request.getProxyProtocolSourceIpV6Address()));
        }
      }

      if (proxyHandlerSupportsUri(proxyServer, tx.request.getUri())) {
        pipeline.addLast(PROXY_HANDLER, proxyServer.newProxyHandler());
      }
    }

    private EventLoopResources(EventLoop eventLoop) {
      channelPool = new ChannelPool();
      eventLoop.scheduleWithFixedDelay(
          () -> channelPool.closeIdleChannels(idleTimeoutNanos),
          POOL_CLEANER_PERIOD_MS,
          POOL_CLEANER_PERIOD_MS,
          TimeUnit.MILLISECONDS);

      http1Bootstrap =
          new Bootstrap()
              .channelFactory(
                  Transports.newSocketChannelFactory(
                      config.isUseNativeTransport(), config.isUseIoUring()))
              .group(eventLoop)
              .resolver(NoopAddressResolverGroup.INSTANCE)
              .handler(
                  new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                      channel.pipeline().addLast(PINNED_HANDLER, NoopHandler.INSTANCE);
                      addHttpHandlers(channel);
                    }
                  });

      Transports.configureTcpOptions(
          http1Bootstrap,
          (int) config.getConnectTimeout(),
          config.isSoKeepAlive(),
          config.isUseNativeTransport() && !config.isUseIoUring() && Epoll.isAvailable());

      http2Bootstrap =
          http1Bootstrap
              .clone()
              .handler(
                  new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                      channel.pipeline().addLast(PINNED_HANDLER, NoopHandler.INSTANCE);
                    }
                  });

      wsBootstrap =
          http1Bootstrap
              .clone()
              .handler(
                  new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                      channel.pipeline().addLast(PINNED_HANDLER, NoopHandler.INSTANCE);
                      addWsHandlers(channel);
                    }
                  });
    }

    private Bootstrap getHttp1BootstrapWithProxy(HttpTx tx, ProxyServer proxy) {
      return http1Bootstrap
          .clone()
          .handler(
              new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                  addProxyHandlers(ch, tx, proxy);
                  addHttpHandlers(ch);
                }
              });
    }

    private Bootstrap getHttp2BootstrapWithProxy(HttpTx tx, ProxyServer proxy) {
      return http2Bootstrap
          .clone()
          .handler(
              new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                  addProxyHandlers(ch, tx, proxy);
                }
              });
    }

    private Bootstrap getWsBootstrapWithProxy(HttpTx tx, ProxyServer proxy) {
      return wsBootstrap
          .clone()
          .handler(
              new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                  addProxyHandlers(ch, tx, proxy);
                  addWsHandlers(ch);
                }
              });
    }
  }

  private final AtomicBoolean closed = new AtomicBoolean();
  private final HttpClientConfig config;
  private final EventExecutor channelGroupEventExecutor;
  private final ChannelGroup channelGroup;
  private final FastThreadLocal<EventLoopResources> eventLoopResources = new FastThreadLocal<>();
  private final long idleTimeoutNanos;

  public DefaultHttpClient(HttpClientConfig config) {
    this.config = config;
    channelGroupEventExecutor = new DefaultEventExecutor();
    channelGroup = new DefaultChannelGroup(channelGroupEventExecutor);
    idleTimeoutNanos = config.getChannelPoolIdleTimeout() * 1_000_000;
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      channelGroup.close().awaitUninterruptibly();
      channelGroupEventExecutor.shutdownGracefully(0, 1, TimeUnit.SECONDS);
      SslContextsHolder defaultSslContextsHolder = config.getDefaultSslContextsHolder();
      if (defaultSslContextsHolder != null) {
        ReferenceCountUtil.release(defaultSslContextsHolder.getSslContext());
        ReferenceCountUtil.release(defaultSslContextsHolder.getAlpnSslContext());
      }
    }
  }

  @Override
  public void sendRequest(
      Request request,
      long clientId,
      EventLoop eventLoop,
      HttpListener listener,
      SslContextsHolder sslContextsHolder) {
    if (isClosed()) {
      return;
    }

    if (sslContextsHolder == null) {
      sslContextsHolder = config.getDefaultSslContextsHolder();
    }

    HttpTx tx = buildTx(request, clientId, listener, sslContextsHolder);

    if (eventLoop.inEventLoop()) {
      sendTx(tx, eventLoop);
    } else if (!eventLoop.isShutdown()) {
      eventLoop.execute(() -> sendTx(tx, eventLoop));
    }
  }

  @Override
  public void sendHttp2Requests(
      Pair<Request, HttpListener>[] requestsAndListeners,
      long clientId,
      EventLoop eventLoop,
      SslContextsHolder sslContextsHolder) {
    if (isClosed()) {
      return;
    }

    Request headRequest = requestsAndListeners[0].getLeft();
    if (!HttpTx.isHttp2Candidate(headRequest) && !HttpTx.isH2cCandidate(headRequest)) {
      // eg through a proxy, even when the remote is known to support HTTP/2
      for (Pair<Request, HttpListener> pair : requestsAndListeners) {
        sendRequest(pair.getLeft(), clientId, eventLoop, pair.getRight(), sslContextsHolder);
      }
      return;
    }

    for (Pair<Request, HttpListener> pair : requestsAndListeners) {
      pair.getRight().onSend();
    }

    if (headRequest.getUri().isSecured() && headRequest.isHttp2Enabled() && !config.isEnableSni()) {
      for (Pair<Request, HttpListener> requestAndListener : requestsAndListeners) {
        HttpListener listener = requestAndListener.getRight();
        listener.onThrowable(
            new UnsupportedOperationException("HTTP/2 can't work if SNI is disabled."));
      }
      return;
    }

    if (sslContextsHolder == null) {
      sslContextsHolder = config.getDefaultSslContextsHolder();
    }

    List<HttpTx> txs = new ArrayList<>(requestsAndListeners.length);
    for (Pair<Request, HttpListener> requestAndListener : requestsAndListeners) {
      Request request = requestAndListener.getLeft();
      HttpListener listener = requestAndListener.getRight();
      txs.add(buildTx(request, clientId, listener, sslContextsHolder));
    }

    if (eventLoop.inEventLoop()) {
      sendHttp2Txs(txs, eventLoop);
    } else if (!eventLoop.isShutdown()) {
      eventLoop.execute(() -> sendHttp2Txs(txs, eventLoop));
    }
  }

  //////////////////// EVERYTHING BELOW ONLY HAPPENS IN SAME EVENTLOOP //////////////////

  private EventLoopResources eventLoopResources(EventLoop eventLoop) {
    EventLoopResources resources = eventLoopResources.get();
    if (resources == null) {
      resources = new EventLoopResources(eventLoop);
      eventLoopResources.set(resources);
    }
    return resources;
  }

  private HttpTx buildTx(
      Request request, long clientId, HttpListener listener, SslContextsHolder sslContextsHolder) {
    RequestTimeout requestTimeout =
        RequestTimeout.requestTimeout(request.getRequestTimeout(), listener);
    ChannelPoolKey key =
        new ChannelPoolKey(clientId, RemoteKey.newKey(request.getUri(), request.getProxyServer()));
    return new HttpTx(request, listener, requestTimeout, key, sslContextsHolder);
  }

  // only retry pooled keep-alive connections = when keep-alive timeout triggered server side while
  // we were writing and request can be replayed
  boolean canRetry(HttpTx tx) {
    return tx.channelState == HttpTx.ChannelState.POOLED && tx.isBodyReplayable();
  }

  void retry(HttpTx tx, EventLoop eventLoop) {
    if (isClosed()) {
      return;
    }

    tx.channelState = HttpTx.ChannelState.RETRY;
    LOGGER.debug("Retrying with new HTTP/1.1 connection");
    sendTx(tx, eventLoop);
  }

  void onFatalError(Throwable cause) {
    config.getFatalErrorHandler().accept(cause);
  }

  void retryHttp2(List<HttpTx> txs, EventLoop eventLoop) {
    if (isClosed()) {
      return;
    }

    for (HttpTx tx : txs) {
      tx.channelState = HttpTx.ChannelState.RETRY;
    }
    LOGGER.debug("Retrying with new HTTP/2 connection");
    for (List<HttpTx> sameRemoteTxs : groupByPoolKey(txs)) {
      sendHttp2Txs(sameRemoteTxs, eventLoop);
    }
  }

  // sendHttp2Txs sends all the txs to the remote of the first one, but the txs dropped by a GOAWAY
  // on a coalesced connection can target different domains
  static Collection<List<HttpTx>> groupByPoolKey(List<HttpTx> txs) {
    Map<ChannelPoolKey, List<HttpTx>> txsByKey = new LinkedHashMap<>();
    for (HttpTx tx : txs) {
      txsByKey.computeIfAbsent(tx.key, k -> new ArrayList<>()).add(tx);
    }
    return txsByKey.values();
  }

  private void sendTx(HttpTx tx, EventLoop eventLoop) {
    EventLoopResources resources = eventLoopResources(eventLoop);
    Request request = tx.request;
    HttpListener listener = tx.listener;
    RequestTimeout requestTimeout = tx.requestTimeout;
    Uri requestUri = request.getUri();

    // use a fresh channel for WebSocket
    Channel pooledChannel = requestUri.isWebSocket() ? null : resources.channelPool.poll(tx.key);

    listener.onSend();

    // start timeout
    tx.requestTimeout.start(eventLoop);

    if (pooledChannel != null && tx.channelState != HttpTx.ChannelState.RETRY) {
      sendTxWithChannel(tx, pooledChannel);

    } else {
      InetSocketAddress proxyHandlerUnresolvedRemoteAddress =
          proxyHandlerUnresolvedRemoteAddress(request.getProxyServer(), requestUri);
      boolean logProxyAddress = proxyHandlerUnresolvedRemoteAddress != null;

      resolveChannelRemoteAddresses(
              request, eventLoop, proxyHandlerUnresolvedRemoteAddress, listener, requestTimeout)
          .addListener(
              (Future<List<InetSocketAddress>> whenRemoteAddresses) -> {
                if (requestTimeout.isDone()) {
                  return;
                }

                if (whenRemoteAddresses.isSuccess()) {
                  List<InetSocketAddress> remoteAddresses = whenRemoteAddresses.getNow();

                  if (tx.canCoalesceConnection() && tx.channelState != HttpTx.ChannelState.RETRY) {
                    String domain = requestUri.getHost();
                    Channel coalescedChannel =
                        resources.channelPool.pollCoalescedChannel(
                            tx.key.clientId, domain, remoteAddresses);
                    if (coalescedChannel != null) {
                      tx.listener.onProtocolAwareness(true);
                      sendTxWithChannel(tx, coalescedChannel);
                    } else {
                      sendTxWithNewChannel(
                          tx, resources, eventLoop, remoteAddresses, logProxyAddress);
                    }
                  } else {
                    sendTxWithNewChannel(
                        tx, resources, eventLoop, remoteAddresses, logProxyAddress);
                  }
                }
              });
    }
  }

  private void sendHttp2Txs(List<HttpTx> txs, EventLoop eventLoop) {
    HttpTx tx = txs.get(0);
    EventLoopResources resources = eventLoopResources(eventLoop);
    Request request = tx.request;
    HttpListener listener = tx.listener;
    RequestTimeout requestTimeout = tx.requestTimeout;
    Uri requestUri = request.getUri();

    // start timeouts
    for (HttpTx t : txs) {
      t.requestTimeout.start(eventLoop);
    }

    ProxyServer proxyServer = request.getProxyServer();
    InetSocketAddress proxyHandlerUnresolvedRemoteAddress =
        proxyHandlerUnresolvedRemoteAddress(proxyServer, requestUri);
    boolean logProxyAddress = proxyHandlerUnresolvedRemoteAddress != null;

    resolveChannelRemoteAddresses(
            request, eventLoop, proxyHandlerUnresolvedRemoteAddress, listener, requestTimeout)
        .addListener(
            (Future<List<InetSocketAddress>> whenRemoteAddresses) -> {
              if (requestTimeout.isDone()) {
                return;
              }

              if (whenRemoteAddresses.isSuccess()) {
                List<InetSocketAddress> addresses = whenRemoteAddresses.getNow();

                String domain = requestUri.getHost();
                Channel pooledChannel = resources.channelPool.poll(tx.key);
                if (pooledChannel == null && tx.canCoalesceConnection()) {
                  pooledChannel =
                      resources.channelPool.pollCoalescedChannel(
                          tx.key.clientId, domain, addresses);
                }

                if (pooledChannel != null) {
                  sendHttp2TxsWithChannel(txs, pooledChannel);
                } else {
                  sendHttp2TxsWithNewChannel(txs, resources, eventLoop, addresses, logProxyAddress);
                }
              }
            });
  }

  private void sendTxWithChannel(HttpTx tx, Channel channel) {
    if (isClosed()) {
      return;
    }

    if (ChannelPool.isHttp2(channel)) {
      tx.listener.onProtocolAwareness(true);
    }

    tx.requestTimeout.setChannel(channel);

    channel.write(tx);
  }

  private void sendHttp2TxsWithChannel(List<HttpTx> txs, Channel channel) {
    if (isClosed()) {
      return;
    }

    for (HttpTx tx : txs) {
      tx.requestTimeout.setChannel(channel);
      tx.listener.onProtocolAwareness(true);
      channel.write(tx);
    }
  }

  private static boolean proxyHandlerSupportsUri(ProxyServer proxyServer, Uri uri) {
    // HttpProxyServer only supports CONNECT requests, hence secured requests
    return !(proxyServer instanceof HttpProxyServer && !uri.isSecured());
  }

  private InetSocketAddress proxyHandlerUnresolvedRemoteAddress(
      ProxyServer proxyServer, Uri requestUri) {
    // HttpProxyHandler doesn't handle clear HTTP requests (absolute URI)
    return proxyServer != null && proxyHandlerSupportsUri(proxyServer, requestUri)
        ? InetSocketAddress.createUnresolved(requestUri.getHost(), requestUri.getExplicitPort())
        : null;
  }

  private Future<List<InetSocketAddress>> resolveChannelRemoteAddresses(
      Request request,
      EventLoop eventLoop,
      InetSocketAddress proxyHandlerUnresolvedRemoteAddress,
      HttpListener listener,
      RequestTimeout requestTimeout) {
    ProxyServer proxyServer = request.getProxyServer();
    if (proxyServer != null) {
      InetSocketAddress channelRemoteAddress =
          proxyHandlerUnresolvedRemoteAddress != null
              ?
              // ProxyHandler will take care of the connect logic
              proxyHandlerUnresolvedRemoteAddress
              :
              // HttpProxyHandler only handles CONNECT requests, not clear HTTP requests with an
              // absolute URL that we have to handle ourselves
              proxyServer.getAddress();

      return ImmediateEventExecutor.INSTANCE.newSucceededFuture(List.of(channelRemoteAddress));

    } else {
      Promise<List<InetSocketAddress>> p = eventLoop.newPromise();

      request
          .getNameResolver()
          .resolveAll(request.getUri().getHost(), eventLoop.newPromise(), listener)
          .addListener(
              (Future<List<InetAddress>> whenAddresses) -> {
                if (whenAddresses.isSuccess()) {
                  List<InetSocketAddress> remoteInetSocketAddresses =
                      whenAddresses.getNow().stream()
                          .map(
                              address ->
                                  new InetSocketAddress(
                                      address, request.getUri().getExplicitPort()))
                          .collect(Collectors.toList());

                  p.setSuccess(remoteInetSocketAddresses);
                } else {
                  if (!requestTimeout.isDone()) {
                    // only report if we haven't timed out
                    listener.onThrowable(whenAddresses.cause());
                  }
                  p.setFailure(whenAddresses.cause());
                  requestTimeout.cancel();
                }
              });
      return p;
    }
  }

  private void sendTxWithNewChannel(
      HttpTx tx,
      EventLoopResources resources,
      EventLoop eventLoop,
      List<InetSocketAddress> remoteAddresses,
      boolean logProxyAddress) {
    tx.channelState = HttpTx.ChannelState.NEW;
    openNewChannel(
            tx,
            tx.request,
            logProxyAddress,
            eventLoop,
            resources,
            remoteAddresses,
            tx.listener,
            tx.requestTimeout)
        .addListener(
            (Future<Channel> whenNewChannel) -> {
              if (whenNewChannel.isSuccess()) {
                Channel channel = whenNewChannel.getNow();
                if (tx.requestTimeout.isDone()) {
                  channel.close();
                  return;
                }

                channelGroup.add(channel);
                ChannelPool.registerPoolKey(channel, tx.key);

                Uri requestUri = tx.request.getUri();
                if (requestUri.isSecured()) {
                  installSslHandler(
                          tx,
                          tx.sslContext(),
                          channel,
                          requestUri.getHost(),
                          requestUri.getExplicitPort(),
                          SSL_HANDLER)
                      .addListener(
                          f -> {
                            if (tx.requestTimeout.isDone() || !f.isSuccess()) {
                              channel.close();
                              return;
                            }

                            if (!tx.isHttp2Candidate()) {
                              sendTxWithChannel(tx, channel);
                            } else {
                              LOGGER.debug("Installing Http2Handler for {}", requestUri);
                              installHttp2Handler(tx, channel, resources.channelPool)
                                  .addListener(
                                      f2 -> {
                                        if (tx.requestTimeout.isDone() || !f2.isSuccess()) {
                                          channel.close();
                                          return;
                                        }
                                        sendTxWithChannel(tx, channel);
                                      });
                            }
                          });
                } else if (tx.isH2cCandidate()) {
                  LOGGER.debug("Installing Http2Handler without TLS (h2c) for {}", requestUri);
                  installH2cHandler(Collections.singletonList(tx), channel, resources.channelPool)
                      .addListener(
                          f -> {
                            if (tx.requestTimeout.isDone() || !f.isSuccess()) {
                              channel.close();
                              return;
                            }
                            sendTxWithChannel(tx, channel);
                          });
                } else {
                  sendTxWithChannel(tx, channel);
                }
              }
            });
  }

  private void sendHttp2TxsWithNewChannel(
      List<HttpTx> txs,
      EventLoopResources resources,
      EventLoop eventLoop,
      List<InetSocketAddress> remoteAddresses,
      boolean logProxyAddress) {
    HttpTx tx = txs.get(0);
    openNewChannel(
            tx,
            tx.request,
            logProxyAddress,
            eventLoop,
            resources,
            remoteAddresses,
            tx.listener,
            tx.requestTimeout)
        .addListener(
            (Future<Channel> whenNewChannel) -> {
              if (whenNewChannel.isSuccess()) {
                Channel channel = whenNewChannel.getNow();
                if (tx.requestTimeout.isDone()) {
                  channel.close();
                  return;
                }

                channelGroup.add(channel);
                ChannelPool.registerPoolKey(channel, tx.key);

                Uri requestUri = tx.request.getUri();
                if (tx.isH2cCandidate()) {
                  LOGGER.debug("Installing Http2Handler without TLS (h2c) for {}", requestUri);
                  installH2cHandler(txs, channel, resources.channelPool)
                      .addListener(
                          f -> {
                            if (tx.requestTimeout.isDone() || !f.isSuccess()) {
                              channel.close();
                              return;
                            }
                            sendHttp2TxsWithChannel(txs, channel);
                          });
                  return;
                }

                LOGGER.debug("Installing SslHandler for {}", requestUri);
                installSslHandler(
                        tx,
                        tx.sslContext(),
                        channel,
                        requestUri.getHost(),
                        requestUri.getExplicitPort(),
                        SSL_HANDLER)
                    .addListener(
                        f -> {
                          if (tx.requestTimeout.isDone() || !f.isSuccess()) {
                            channel.close();
                            return;
                          }
                          LOGGER.debug("Installing Http2Handler for {}", requestUri);
                          installHttp2Handler(tx, channel, resources.channelPool)
                              .addListener(
                                  f2 -> {
                                    if (tx.requestTimeout.isDone() || !f2.isSuccess()) {
                                      channel.close();
                                      return;
                                    }
                                    sendHttp2TxsWithChannel(txs, channel);
                                  });
                        });
              }
            });
  }

  private Bootstrap bootstrap(HttpTx tx, Request request, EventLoopResources resources) {
    Uri requestUri = request.getUri();
    ProxyServer proxyServer = request.getProxyServer();

    if (proxyServer != null) {
      if (requestUri.isWebSocket()) {
        return resources.getWsBootstrapWithProxy(tx, proxyServer);
      } else if (tx.isHttp2Candidate()) {
        // the remote's TLS connection is tunneled, the HTTP/2 or HTTP/1 handlers are installed
        // once ALPN is done
        return resources.getHttp2BootstrapWithProxy(tx, proxyServer);
      } else {
        // HttpProxyHandler doesn't handle clear HTTP requests, only CONNECT ones
        return resources.getHttp1BootstrapWithProxy(tx, proxyServer);
      }
    }

    if (requestUri.isWebSocket()) {
      return resources.wsBootstrap;
    } else if (tx.isHttp2Candidate() || tx.isH2cCandidate()) {
      return resources.http2Bootstrap;
    } else {
      return resources.http1Bootstrap;
    }
  }

  private Future<Channel> openNewChannel(
      HttpTx tx,
      Request request,
      boolean logProxyAddress,
      EventLoop eventLoop,
      EventLoopResources resources,
      List<InetSocketAddress> remoteAddresses,
      HttpListener listener,
      RequestTimeout requestTimeout) {
    LOGGER.debug(
        "Opening new channel to remote={} from local={}",
        remoteAddresses,
        request.getLocalAddresses());
    Bootstrap bootstrap = bootstrap(tx, request, resources);
    Promise<Channel> channelPromise = eventLoop.newPromise();
    InetSocketAddress loggedProxyAddress =
        logProxyAddress ? request.getProxyServer().getAddress() : null;
    openNewChannelRec(
        remoteAddresses,
        loggedProxyAddress,
        request.getLocalAddresses(),
        0,
        channelPromise,
        bootstrap,
        listener,
        requestTimeout);
    return channelPromise;
  }

  private static final Exception IGNORE_REQUEST_TIMEOUT_REACHED_WHILE_TRYING_TO_CONNECT =
      new TimeoutException("Request timeout reached while trying to connect, should be ignored") {
        @Override
        public synchronized Throwable fillInStackTrace() {
          return this;
        }
      };

  private void openNewChannelRec(
      List<InetSocketAddress> remoteAddresses,
      InetSocketAddress loggedProxyAddress,
      LocalAddresses localAddresses,
      int i,
      Promise<Channel> channelPromise,
      Bootstrap bootstrap,
      HttpListener listener,
      RequestTimeout requestTimeout) {

    if (isClosed()) {
      return;
    }

    InetSocketAddress remoteAddress = remoteAddresses.get(i);
    InetSocketAddress localAddress;
    boolean forceMoveToNextRemoteAddress = false;

    if (localAddresses == null) {
      // non explicit local addresses, skip
      localAddress = null;
    } else {
      InetSocketAddress localAddressForRemote =
          localAddresses.getLocalAddressForRemote(remoteAddress.getAddress());
      if (localAddressForRemote == null) {
        // no match
        localAddress = null;
        forceMoveToNextRemoteAddress = true;
      } else {
        localAddress = localAddressForRemote;
      }
    }

    if (forceMoveToNextRemoteAddress) {
      int nextI = i + 1;
      if (nextI < remoteAddresses.size()) {
        openNewChannelRec(
            remoteAddresses,
            loggedProxyAddress,
            localAddresses,
            nextI,
            channelPromise,
            bootstrap,
            listener,
            requestTimeout);

      } else {
        requestTimeout.cancel();
        Exception cause =
            new UnsupportedOperationException(
                "Can't connect to remote " + remoteAddresses + " + from local " + localAddresses);
        listener.onThrowable(cause);
        channelPromise.setFailure(cause);
      }
    } else {
      // [e]
      //
      // [e]
      ChannelFuture whenChannel = bootstrap.connect(remoteAddress, localAddress);

      whenChannel.addListener(
          f -> {
            if (f.isSuccess()) {
              // [e]
              //
              //
              //
              //
              //
              // [e]
              LOGGER.debug(
                  "Connected to remoteAddress={} from localAddress={}",
                  remoteAddress,
                  whenChannel.channel().localAddress());
              channelPromise.setSuccess(whenChannel.channel());

            } else {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Failed to connect to remoteAddress="
                        + remoteAddress
                        + " from localAddress="
                        + localAddress,
                    f.cause());
              }
              // [e]
              //
              // [e]

              if (requestTimeout.isDone()) {
                channelPromise.setFailure(IGNORE_REQUEST_TIMEOUT_REACHED_WHILE_TRYING_TO_CONNECT);
                return;
              }

              int nextI = i + 1;
              if (nextI < remoteAddresses.size()) {
                openNewChannelRec(
                    remoteAddresses,
                    loggedProxyAddress,
                    localAddresses,
                    nextI,
                    channelPromise,
                    bootstrap,
                    listener,
                    requestTimeout);

              } else {
                requestTimeout.cancel();
                listener.onThrowable(f.cause());
                channelPromise.setFailure(f.cause());
              }
            }
          });
    }
  }

  private Future<Channel> installSslHandler(
      HttpTx tx,
      SslContext sslContext,
      Channel channel,
      String peerHost,
      int peerPort,
      String sslHandlerName) {
    LOGGER.debug("Installing SslHandler for {}:{}", peerHost, peerPort);
    // [e]
    //
    // [e]

    try {
      SslHandler sslHandler =
          SslHandlers.newSslHandler(sslContext, channel.alloc(), peerHost, peerPort, config);

      ChannelPipeline pipeline = channel.pipeline();
      String after = pipeline.get(PROXY_HANDLER) != null ? PROXY_HANDLER : PINNED_HANDLER;
      pipeline.addAfter(after, sslHandlerName, sslHandler);

      return sslHandler
          .handshakeFuture()
          .addListener(
              f -> {
                if (tx.requestTimeout.isDone()) {
                  return;
                }

                if (f.isSuccess()) {
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                        "TLS handshake successful: peerHost={} peerPort={} protocol={} cipher suite={}",
                        sslHandler.engine().getSession().getPeerHost(),
                        sslHandler.engine().getSession().getPeerPort(),
                        sslHandler.engine().getSession().getProtocol(),
                        sslHandler.engine().getSession().getCipherSuite());
                  }

                  // [e]
                  //
                  // [e]
                } else {
                  tx.requestTimeout.cancel();
                  // [e]
                  //
                  // [e]
                  tx.listener.onThrowable(f.cause());
                }
              });
    } catch (RuntimeException e) {
      tx.requestTimeout.cancel();
      // [e]
      //
      // [e]
      tx.listener.onThrowable(e);
      return new DefaultPromise<Channel>(ImmediateEventExecutor.INSTANCE).setFailure(e);
    }
  }

  static void installHttp2Handlers(
      ChannelPipeline pipeline,
      Http2FlowControlWindows windows,
      BiConsumer<List<HttpTx>, EventLoop> retry,
      Consumer<Throwable> fatalErrorHandler,
      ChannelPool channelPool,
      Promise<Void> whenAlpn)
      throws Http2Exception {
    Http2Connection connection = new DefaultHttp2Connection(false);

    ChannelPool.registerHttp2Connection(pipeline.channel(), connection);

    CustomDelegatingDecompressorFrameListener decompressorFrameListener =
        new CustomDelegatingDecompressorFrameListener(
            connection, new NotAggregatingInboundHttp2ToHttpAdapter(connection, whenAlpn));

    HttpToHttp2ConnectionHandler http2Handler =
        new HttpToHttp2ConnectionHandlerBuilder()
            .initialSettings(initialHttp2Settings(windows))
            .connection(connection)
            .frameListener(decompressorFrameListener)
            // queue streams exceeding the server's MAX_CONCURRENT_STREAMS instead of failing them
            .encoderEnforceMaxConcurrentStreams(true)
            .build();

    Http2AppHandler http2AppHandler =
        new Http2AppHandler(retry, fatalErrorHandler, http2Handler, channelPool);
    decompressorFrameListener.setDecompressionRequired(
        http2AppHandler::isResponseBodyDecompressionRequired);

    pipeline
        .addLast(HTTP2_HANDLER, http2Handler)
        .addLast(HTTP2_CONTENT_ENCODER, ByteBufToHttpContentEncoder.INSTANCE)
        .addLast(CHUNKED_WRITER_HANDLER, new ForkedChunkedWriteHandler())
        .addLast(APP_HTTP2_HANDLER, http2AppHandler);

    // the connection window isn't a setting: it always starts at 65,535 bytes and can only be
    // enlarged with a WINDOW_UPDATE, sent right after the preface
    int connectionWindowIncrement =
        windows.getConnectionWindowSize() - Http2CodecUtil.DEFAULT_WINDOW_SIZE;
    if (connectionWindowIncrement > 0) {
      connection
          .local()
          .flowController()
          .incrementWindowSize(connection.connectionStream(), connectionWindowIncrement);
      pipeline.channel().flush();
    }
  }

  /**
   * With prior knowledge, there's no negotiation: the client sends the HTTP/2 preface right away,
   * and the server's SETTINGS tell it speaks HTTP/2.
   */
  private Future<Void> installH2cHandler(
      List<HttpTx> txs, Channel channel, ChannelPool channelPool) {
    HttpTx tx = txs.get(0);
    Promise<Void> whenSettings = channel.eventLoop().newPromise();
    try {
      installHttp2Handlers(
          channel.pipeline(),
          tx.request.getHttp2FlowControlWindows(),
          DefaultHttpClient.this::retryHttp2,
          DefaultHttpClient.this::onFatalError,
          channelPool,
          whenSettings);
    } catch (Http2Exception e) {
      whenSettings.tryFailure(e);
    }

    channel
        .closeFuture()
        .addListener(
            f ->
                whenSettings.tryFailure(
                    new IOException(
                        "Connection was closed before receiving HTTP/2 SETTINGS, does "
                            + tx.request.getUri().getHost()
                            + " really support HTTP/2 without TLS (h2c)?")));

    whenSettings.addListener(
        f -> {
          if (f.isSuccess()) {
            // only pool once the remote proved it speaks HTTP/2
            channelPool.offer(channel);
          } else {
            for (HttpTx t : txs) {
              t.requestTimeout.cancel();
              t.listener.onThrowable(f.cause());
            }
          }
        });
    return whenSettings;
  }

  private Future<Void> installHttp2Handler(HttpTx tx, Channel channel, ChannelPool channelPool) {

    Promise<Void> whenAlpn = channel.eventLoop().newPromise();

    channel
        .pipeline()
        .addAfter(
            SSL_HANDLER,
            ALPN_HANDLER,
            new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
              @Override
              protected void configurePipeline(
                  ChannelHandlerContext ctx, String firstSslHandlerProtocol) throws Exception {
                // ApplicationProtocolNegotiationHandler reads the protocol from the first
                // SslHandler in the pipeline, which is the proxy's one with an HTTPS proxy
                String remoteProtocol =
                    ((SslHandler) ctx.pipeline().get(SSL_HANDLER)).applicationProtocol();
                String protocol =
                    remoteProtocol != null ? remoteProtocol : ApplicationProtocolNames.HTTP_1_1;
                switch (protocol) {
                  case ApplicationProtocolNames.HTTP_2:
                    LOGGER.debug(
                        "ALPN led to HTTP/2 with remote {}", tx.request.getUri().getHost());
                    tx.listener.onProtocolAwareness(true);
                    installHttp2Handlers(
                        ctx.pipeline(),
                        tx.request.getHttp2FlowControlWindows(),
                        DefaultHttpClient.this::retryHttp2,
                        DefaultHttpClient.this::onFatalError,
                        channelPool,
                        whenAlpn);
                    channelPool.offer(channel);

                    SslHandler sslHandler = (SslHandler) ctx.pipeline().get(SSL_HANDLER);
                    Set<String> subjectAlternativeNames =
                        Tls.extractSubjectAlternativeNames(sslHandler.engine());
                    if (LOGGER.isDebugEnabled()) {
                      LOGGER.debug(
                          "TLS handshake successful: protocol={} cipher suite={}",
                          sslHandler.engine().getSession().getProtocol(),
                          sslHandler.engine().getSession().getCipherSuite());
                    }
                    if (subjectAlternativeNames.size() > 1 && tx.canCoalesceConnection()) {
                      channelPool.offerCoalescedChannel(
                          subjectAlternativeNames,
                          (InetSocketAddress) channel.remoteAddress(),
                          channel,
                          tx.key);
                    }
                    break;

                  case ApplicationProtocolNames.HTTP_1_1:
                    LOGGER.debug(
                        "ALPN led to HTTP/1 with remote {}", tx.request.getUri().getHost());
                    if (tx.request.getHttp2PriorKnowledge()
                        == Http2PriorKnowledge.HTTP2_SUPPORTED) {
                      IllegalStateException e =
                          new IllegalStateException(
                              "HTTP/2 Prior knowledge was set on host "
                                  + tx.request.getUri().getHost()
                                  + " but it only supports HTTP/1");
                      whenAlpn.setFailure(e);
                      throw e;
                    }
                    tx.listener.onProtocolAwareness(false);
                    CustomHttpContentDecompressor decompressor =
                        new CustomHttpContentDecompressor();
                    ctx.pipeline()
                        .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
                        .addLast(INFLATER_HANDLER, decompressor)
                        .addLast(CHUNKED_WRITER_HANDLER, new ForkedChunkedWriteHandler())
                        .addLast(
                            APP_HTTP_HANDLER,
                            new HttpAppHandler(DefaultHttpClient.this, channelPool, decompressor));
                    whenAlpn.setSuccess(null);
                    break;

                  default:
                    IllegalStateException e =
                        new IllegalStateException("Unknown protocol: " + protocol);
                    whenAlpn.setFailure(e);
                    ctx.close();
                    // FIXME do we really need to throw?
                    throw e;
                }
              }
            });

    whenAlpn.addListener(
        f -> {
          if (!f.isSuccess()) {
            tx.listener.onThrowable(f.cause());
          }
        });

    return whenAlpn;
  }

  @Override
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void flushClientIdChannels(long clientId, EventLoop eventLoop) {
    if (eventLoop.inEventLoop()) {
      eventLoopResources(eventLoop).channelPool.flushClientIdChannelPoolPartitions(clientId);
    } else if (!eventLoop.isShutdown()) {
      eventLoop.execute(
          () ->
              eventLoopResources(eventLoop)
                  .channelPool
                  .flushClientIdChannelPoolPartitions(clientId));
    }
  }
}
