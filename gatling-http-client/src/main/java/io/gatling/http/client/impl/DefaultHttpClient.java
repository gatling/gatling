/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import io.gatling.http.client.HttpClient;
import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.ahc.uri.Uri;
import io.gatling.http.client.body.is.InputStreamRequestBody;
import io.gatling.http.client.pool.ChannelPool;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.gatling.http.client.pool.RemoteKey;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.realm.DigestRealm;
import io.gatling.http.client.realm.Realm;
import io.gatling.http.client.ssl.Tls;
import io.gatling.http.client.util.Pair;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.*;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class DefaultHttpClient implements HttpClient {

  static {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpClient.class);

  private static final String PINNED_HANDLER = "pinned";
  private static final String PROXY_HANDLER = "proxy";
  private static final String SSL_HANDLER = "ssl";
  private static final String HTTP_CLIENT_CODEC = "http";
  private static final String HTTP2_HANDLER = "http2";
  private static final String INFLATER_HANDLER = "inflater";
  private static final String CHUNKED_WRITER_HANDLER = "chunked-writer";
  private static final String DIGEST_AUTH_HANDLER = "digest";
  private static final String WS_OBJECT_AGGREGATOR = "ws-aggregator";
  private static final String WS_COMPRESSION = "ws-compression";
  static final String WS_AGGREGATOR = "ws-aggregator";
  static final String APP_WS_HANDLER = "app-ws";
  private static final String ALPN_HANDLER = "alpn";
  private static final String APP_HTTP2_HANDLER = "app-http2";

  public static final String APP_HTTP_HANDLER = "app-http";

  private HttpClientCodec newHttpClientCodec() {
    return new HttpClientCodec(
      4096,
      8192,
      8192,
      false,
      false,
      128);
  }

  private HttpContentDecompressor newHttpContentDecompressor() {
    return new HttpContentDecompressor() {
      @Override
      protected String getTargetContentEncoding(String contentEncoding) {
        return contentEncoding;
      }
    };
  }

  private class EventLoopResources {

    private final Bootstrap http1Bootstrap;
    private final Bootstrap http2Bootstrap;
    private final Bootstrap wsBoostrap;
    private final ChannelPool channelPool;

    private void addDefaultHttpHandlers(ChannelPipeline pipeline) {
      pipeline
        .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
        .addLast(INFLATER_HANDLER, newHttpContentDecompressor())
        .addLast(CHUNKED_WRITER_HANDLER, new ChunkedWriteHandler())
        .addLast(APP_HTTP_HANDLER, new HttpAppHandler(DefaultHttpClient.this, channelPool, config));
    }

    private EventLoopResources(EventLoop eventLoop) {
      channelPool = new ChannelPool();
      long channelPoolIdleCleanerPeriod = config.getChannelPoolIdleCleanerPeriod();
      long idleTimeoutNanos = config.getChannelPoolIdleTimeout() * 1_000_000;
      eventLoop.scheduleWithFixedDelay(
        () -> channelPool.closeIdleChannels(idleTimeoutNanos),
        channelPoolIdleCleanerPeriod,
        channelPoolIdleCleanerPeriod,
        TimeUnit.MILLISECONDS);

      http1Bootstrap = new Bootstrap()
        .channelFactory(config.isUseNativeTransport() ? EpollSocketChannel::new : NioSocketChannel::new)
        .group(eventLoop)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout())
        .option(ChannelOption.SO_REUSEADDR, config.isSoReuseAddress())
        .option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) {
            ch.pipeline().addLast(PINNED_HANDLER, NoopHandler.INSTANCE);
            addDefaultHttpHandlers(ch.pipeline());
            if (config.getAdditionalChannelInitializer() != null) {
              config.getAdditionalChannelInitializer().accept(ch);
            }
          }
        });

      http2Bootstrap = http1Bootstrap.clone().handler(new ChannelInitializer<Channel>() {

        @Override
        protected void initChannel(Channel ch) {
          ch.pipeline()
            .addLast(PINNED_HANDLER, NoopHandler.INSTANCE)
            .addLast(CHUNKED_WRITER_HANDLER, new ChunkedWriteHandler());

          if (config.getAdditionalChannelInitializer() != null) {
            config.getAdditionalChannelInitializer().accept(ch);
          }
        }
      });

      wsBoostrap = http1Bootstrap.clone().handler(new ChannelInitializer<Channel>() {

        @Override
        protected void initChannel(Channel ch) {
          ch.pipeline()
            .addLast(PINNED_HANDLER, NoopHandler.INSTANCE)
            .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
            .addLast(WS_OBJECT_AGGREGATOR, new HttpObjectAggregator(8192))
            .addLast(WS_COMPRESSION, WebSocketClientCompressionHandler.INSTANCE)
            .addLast(APP_WS_HANDLER, new WebSocketHandler(config));

          if (config.getAdditionalChannelInitializer() != null) {
            config.getAdditionalChannelInitializer().accept(ch);
          }
        }
      });
    }

    private Bootstrap getBootstrapWithProxy(ProxyServer proxy) {
      return http1Bootstrap.clone().handler(new ChannelInitializer<Channel>() {

        @Override
        protected void initChannel(Channel ch) {
          ChannelPipeline pipeline = ch.pipeline()
            .addLast(PINNED_HANDLER, NoopHandler.INSTANCE)
            .addLast(PROXY_HANDLER, proxy.newHandler());

          addDefaultHttpHandlers(pipeline);
          if (config.getAdditionalChannelInitializer() != null) {
            config.getAdditionalChannelInitializer().accept(ch);
          }
        }
      });
    }
  }

  private final AtomicBoolean closed = new AtomicBoolean();
  private final HttpClientConfig config;
  private final MultithreadEventExecutorGroup eventLoopGroup;
  private final AffinityEventLoopPicker eventLoopPicker;
  private final ChannelGroup channelGroup;
  private final ThreadLocal<EventLoopResources> eventLoopResources = new ThreadLocal<>();

  public DefaultHttpClient(HttpClientConfig config) {
    this.config = config;
    DefaultThreadFactory threadFactory = new DefaultThreadFactory(config.getThreadPoolName());
    eventLoopGroup = config.isUseNativeTransport() ? new EpollEventLoopGroup(0, threadFactory) : new NioEventLoopGroup(0, threadFactory);
    eventLoopPicker = new AffinityEventLoopPicker(eventLoopGroup);
    channelGroup = new DefaultChannelGroup(eventLoopGroup.next());
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      channelGroup.close().awaitUninterruptibly();
      eventLoopGroup.shutdownGracefully();
    }
  }

  @Override
  public void sendRequest(Request request, long clientId, boolean shared, HttpListener listener, SslContext sslContext, SslContext alpnSslContext) {
    if (isClosed()) {
      return;
    }

    listener.onSend();
    if (request.getUri().isSecured() && request.isHttp2Enabled() && !config.isEnableSni()) {
      listener.onThrowable(new UnsupportedOperationException("HTTP/2 can't work if SNI is disabled."));
      return;
    }

    EventLoop eventLoop = eventLoopPicker.eventLoopWithAffinity(clientId);

    if (sslContext == null) {
      sslContext = config.getDefaultSslContext();
      alpnSslContext = config.getDefaultAlpnSslContext();
    }

    HttpTx tx = buildTx(request, clientId, shared, listener, sslContext, alpnSslContext);

    if (eventLoop.inEventLoop()) {
      sendTx(tx, eventLoop);
    } else {
      eventLoop.execute(() -> sendTx(tx, eventLoop));
    }
  }

  @Override
  public void sendHttp2Requests(Pair<Request, HttpListener>[] requestsAndListeners, long clientId, boolean shared, SslContext sslContext, SslContext alpnSslContext) {
    if (isClosed()) {
      return;
    }
    for (Pair<Request, HttpListener> pair: requestsAndListeners) {
      pair.getRight().onSend();
    }

    Request headRequest = requestsAndListeners[0].getLeft();

    if (headRequest.getUri().isSecured() && headRequest.isHttp2Enabled() && !config.isEnableSni()) {
      for (Pair<Request, HttpListener> requestAndListener: requestsAndListeners) {
        HttpListener listener = requestAndListener.getRight();
        listener.onThrowable(new UnsupportedOperationException("HTTP/2 can't work if SNI is disabled."));
      }
      return;
    }

    EventLoop eventLoop = eventLoopPicker.eventLoopWithAffinity(clientId);

    if (sslContext == null) {
      sslContext = config.getDefaultSslContext();
      alpnSslContext = config.getDefaultAlpnSslContext();
    }

    List<HttpTx> txs = new ArrayList<>();
    for (Pair<Request, HttpListener> requestAndListener : requestsAndListeners) {
      Request request = requestAndListener.getLeft();
      HttpListener listener = requestAndListener.getRight();
      txs.add(buildTx(request, clientId, shared, listener, sslContext, alpnSslContext));
    }

    if (eventLoop.inEventLoop()) {
      sendHttp2Txs(txs, eventLoop);
    } else {
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

  private HttpTx buildTx(Request request, long clientId, boolean shared, HttpListener listener, SslContext sslContext, SslContext alpnSslContext) {
    RequestTimeout requestTimeout = RequestTimeout.requestTimeout(request.getRequestTimeout(), listener);
    ChannelPoolKey key = new ChannelPoolKey(shared ? -1 : clientId, RemoteKey.newKey(request.getUri(), request.getVirtualHost(), request.getProxyServer()));
    return new HttpTx(request, listener, requestTimeout, key, config.getMaxRetry(), sslContext, alpnSslContext);
  }

  boolean retry(HttpTx tx, EventLoop eventLoop) {
    if (isClosed()) {
      return false;
    }

    if (tx.remainingTries > 0 && !(tx.request.getBody() instanceof InputStreamRequestBody)) {
      tx.remainingTries = tx.remainingTries - 1;
      sendTx(tx, eventLoop);
      return true;
    }
    return false;
  }

  private void sendTx(HttpTx tx, EventLoop eventLoop) {

    EventLoopResources resources = eventLoopResources(eventLoop);
    Request request = tx.request;
    HttpListener listener = tx.listener;
    RequestTimeout requestTimeout = tx.requestTimeout;

    // start timeout
    tx.requestTimeout.start(eventLoop);

    // use a fresh channel for WebSocket
    Channel pooledChannel = request.getUri().isWebSocket() ? null : resources.channelPool.poll(tx.key);

    if (pooledChannel != null) {
      sendTxWithChannel(tx, pooledChannel);

    } else {
      resolveRemoteAddresses(request, eventLoop, listener, requestTimeout)
        .addListener((Future<List<InetSocketAddress>> whenRemoteAddresses) -> {
          if (requestTimeout.isDone()) {
            return;
          }

          if (whenRemoteAddresses.isSuccess()) {
            List<InetSocketAddress> addresses = whenRemoteAddresses.getNow();

            if (request.isHttp2Enabled()) {
              String domain = tx.request.getUri().getHost();
              Channel coalescedChannel = resources.channelPool.pollCoalescedChannel(tx.key.clientId, domain, addresses);
              if (coalescedChannel != null) {
                tx.listener.onProtocolAwareness(true);
                sendTxWithChannel(tx, coalescedChannel);
              } else {
                sendTxWithNewChannel(tx, resources, eventLoop, addresses);
              }
            } else {
              sendTxWithNewChannel(tx, resources, eventLoop, addresses);
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

    // start timeouts
    for (HttpTx t : txs) {
      t.requestTimeout.start(eventLoop);
    }

    resolveRemoteAddresses(request, eventLoop, listener, requestTimeout)
       .addListener((Future<List<InetSocketAddress>> whenRemoteAddresses) -> {
         if (requestTimeout.isDone()) {
           return;
         }

         if (whenRemoteAddresses.isSuccess()) {
           List<InetSocketAddress> addresses = whenRemoteAddresses.getNow();

           String domain = tx.request.getUri().getHost();
           Channel coalescedChannel = resources.channelPool.pollCoalescedChannel(tx.key.clientId, domain, addresses);
           if (coalescedChannel != null) {
             sendHttp2TxsWithChannel(txs, coalescedChannel);
           } else {
             sendHttp2TxsWithNewChannel(txs, resources, eventLoop, addresses);
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

    Realm realm = tx.request.getRealm();
    if (realm instanceof DigestRealm) {
      // FIXME is it the right place?
      // FIXME wouldn't work for WebSocket
      // FIXME wouldn't work with HTTP/2
      channel.pipeline().addBefore(APP_HTTP_HANDLER, DIGEST_AUTH_HANDLER, new DigestAuthHandler(tx, (DigestRealm) realm, config));
    }

    channel.write(tx);
  }

  private void sendHttp2TxsWithChannel(List<HttpTx> txs, Channel channel) {

    if (isClosed()) {
      return;
    }

    for (HttpTx tx: txs) {
      tx.requestTimeout.setChannel(channel);
      tx.listener.onProtocolAwareness(true);
      channel.write(tx);
    }
  }

  private Future<List<InetSocketAddress>> resolveRemoteAddresses(Request request, EventLoop eventLoop, HttpListener listener, RequestTimeout requestTimeout) {
    if (!request.getUri().isSecured() && request.getProxyServer() instanceof HttpProxyServer) {
      // directly connect to proxy over clear HTTP
      InetSocketAddress remoteAddress = ((HttpProxyServer) request.getProxyServer()).getAddress();
      return ImmediateEventExecutor.INSTANCE.newSucceededFuture(singletonList(remoteAddress));
    } else {
      Promise<List<InetSocketAddress>> p = eventLoop.newPromise();

      request.getNameResolver().resolveAll(request.getUri().getHost(), eventLoop.newPromise())
        .addListener((Future<List<InetAddress>> whenAddresses) -> {
          if (whenAddresses.isSuccess()) {
            List<InetSocketAddress> remoteInetSocketAddresses = whenAddresses.getNow().stream()
              .map(address -> new InetSocketAddress(address, request.getUri().getExplicitPort()))
              .collect(Collectors.toList());

            p.setSuccess(remoteInetSocketAddresses);
          } else {
            if (!requestTimeout.isDone()) {
              // only report if we haven't timed out
              listener.onThrowable(whenAddresses.cause());
            }
            p.setFailure(whenAddresses.cause());
          }
        });
      return p;
    }
  }

  private void sendTxWithNewChannel(HttpTx tx,
                                    EventLoopResources resources,
                                    EventLoop eventLoop,
                                    List<InetSocketAddress> addresses) {
    openNewChannel(tx.request, eventLoop, resources, addresses, tx.listener, tx.requestTimeout)
      .addListener((Future<Channel> whenNewChannel) -> {
        if (whenNewChannel.isSuccess()) {
          Channel channel = whenNewChannel.getNow();
          if (tx.requestTimeout.isDone()) {
            channel.close();
            return;
          }

          channelGroup.add(channel);
          resources.channelPool.register(channel, tx.key);

          if (tx.request.getUri().isSecured()) {
            LOGGER.debug("Installing SslHandler for {}", tx.request.getUri());
            installSslHandler(tx, channel).addListener(f -> {
              if (tx.requestTimeout.isDone() || !f.isSuccess()) {
                channel.close();
                return;
              }

              if (tx.request.isAlpnRequired()) {
                LOGGER.debug("Installing Http2Handler for {}", tx.request.getUri());
                installHttp2Handler(tx, channel, resources.channelPool).addListener(f2 -> {
                  if (tx.requestTimeout.isDone() || !f2.isSuccess()) {
                    channel.close();
                    return;
                  }
                  sendTxWithChannel(tx, channel);
                });

              } else {
                sendTxWithChannel(tx, channel);
              }
            });
          } else {
            sendTxWithChannel(tx, channel);
          }
        }
      });
  }

  private void sendHttp2TxsWithNewChannel(List<HttpTx> txs,
                                          EventLoopResources resources,
                                          EventLoop eventLoop,
                                          List<InetSocketAddress> addresses) {
    HttpTx tx = txs.get(0);
    openNewChannel(tx.request, eventLoop, resources, addresses, tx.listener, tx.requestTimeout)
       .addListener((Future<Channel> whenNewChannel) -> {
         if (whenNewChannel.isSuccess()) {
           Channel channel = whenNewChannel.getNow();
           if (tx.requestTimeout.isDone()) {
             channel.close();
             return;
           }

           channelGroup.add(channel);
           resources.channelPool.register(channel, tx.key);

           LOGGER.debug("Installing SslHandler for {}", tx.request.getUri());
           installSslHandler(tx, channel).addListener(f -> {
             if (tx.requestTimeout.isDone() || !f.isSuccess()) {
               channel.close();
               return;
             }
             LOGGER.debug("Installing Http2Handler for {}", tx.request.getUri());
             installHttp2Handler(tx, channel, resources.channelPool).addListener(f2 -> {
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

  private Future<Channel> openNewChannel(Request request,
                                         EventLoop eventLoop,
                                         EventLoopResources resources,
                                         List<InetSocketAddress> remoteAddresses,
                                         HttpListener listener,
                                         RequestTimeout requestTimeout) {

    InetSocketAddress localAddress = request.getLocalAddress() != null ? new InetSocketAddress(request.getLocalAddress(), 0) : null;
    Uri uri = request.getUri();
    ProxyServer proxyServer = request.getProxyServer();

    Bootstrap bootstrap =
      uri.isWebSocket() ?
        resources.wsBoostrap :
        uri.isSecured() && proxyServer != null ?
          resources.getBootstrapWithProxy(proxyServer) :
          request.isAlpnRequired() && request.getUri().isSecured() ? resources.http2Bootstrap : resources.http1Bootstrap;

    Promise<Channel> channelPromise = eventLoop.newPromise();
    openNewChannelRec(remoteAddresses, localAddress, 0, channelPromise, bootstrap, listener, requestTimeout);
    return channelPromise;
  }

  private static final Exception IGNORE_REQUEST_TIMEOUT_REACHED_WHILE_TRYING_TO_CONNECT = new TimeoutException("Request timeout reached while trying to connect, should be ignored") {
    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  private void openNewChannelRec(List<InetSocketAddress> remoteAddresses,
                                 InetSocketAddress localAddress,
                                 int i,
                                 Promise<Channel> channelPromise,
                                 Bootstrap bootstrap,
                                 HttpListener listener,
                                 RequestTimeout requestTimeout) {

    if (isClosed()) {
      return;
    }

    InetSocketAddress remoteAddress = remoteAddresses.get(i);

    listener.onTcpConnectAttempt(remoteAddress);
    ChannelFuture whenChannel = bootstrap.connect(remoteAddress, localAddress);

    whenChannel.addListener(f -> {
      if (f.isSuccess()) {
        Channel channel = whenChannel.channel();
        listener.onTcpConnectSuccess(remoteAddress, channel);
        channelPromise.setSuccess(channel);

      } else {
        listener.onTcpConnectFailure(remoteAddress, f.cause());

        if (requestTimeout.isDone()) {
          channelPromise.setFailure(IGNORE_REQUEST_TIMEOUT_REACHED_WHILE_TRYING_TO_CONNECT);
          return;
        }

        int nextI = i + 1;
        if (nextI < remoteAddresses.size()) {
          openNewChannelRec(remoteAddresses, localAddress, nextI, channelPromise, bootstrap, listener, requestTimeout);

        } else {
          requestTimeout.cancel();
          listener.onThrowable(f.cause());
          channelPromise.setFailure(f.cause());
        }
      }
    });
  }

  private Future<Channel> installSslHandler(HttpTx tx, Channel channel) {

    try {
      SslHandler sslHandler = SslHandlers.newSslHandler(tx.sslContext(), channel.alloc(), tx.request.getUri(), tx.request.getVirtualHost(), config);
      tx.listener.onTlsHandshakeAttempt();

      ChannelPipeline pipeline = channel.pipeline();
      String after = pipeline.get(PROXY_HANDLER) != null ? PROXY_HANDLER : PINNED_HANDLER;
      pipeline.addAfter(after, SSL_HANDLER, sslHandler);

      return sslHandler.handshakeFuture().addListener(f -> {
        if (tx.requestTimeout.isDone()) {
          return;
        }

        if (f.isSuccess()) {
          tx.listener.onTlsHandshakeSuccess();
        } else {
          tx.requestTimeout.cancel();
          tx.listener.onTlsHandshakeFailure(f.cause());
          tx.listener.onThrowable(f.cause());
        }
      });
    } catch (RuntimeException e) {
      tx.requestTimeout.cancel();
      tx.listener.onThrowable(e);
      return new DefaultPromise<Channel>(ImmediateEventExecutor.INSTANCE).setFailure(e);
    }
  }

  private Future<Channel> installHttp2Handler(HttpTx tx, Channel channel, ChannelPool channelPool) {

    Promise<Channel> whenAlpn = channel.eventLoop().newPromise();

    channel.pipeline().addAfter(SSL_HANDLER, ALPN_HANDLER, new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
      @Override
      protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {

        switch (protocol) {
          case ApplicationProtocolNames.HTTP_2:
            LOGGER.debug("ALPN led to HTTP/2 with remote {}", tx.request.getUri().getHost());
            tx.listener.onProtocolAwareness(true);
            Http2Connection connection = new DefaultHttp2Connection(false);

            HttpToHttp2ConnectionHandler http2Handler = new HttpToHttp2ConnectionHandlerBuilder()
              .initialSettings(Http2Settings.defaultSettings()) // FIXME override?
              .connection(connection)
              .frameListener(
                new DelegatingDecompressorFrameListener(
                  connection,
                  new ChunkedInboundHttp2ToHttpAdapter(connection, false, true, whenAlpn))
              ).build();

            ctx.pipeline()
              .addLast(HTTP2_HANDLER, http2Handler)
              .addLast(APP_HTTP2_HANDLER, new Http2AppHandler(connection, http2Handler, channelPool, config));

            channelPool.offer(channel);

            SslHandler sslHandler = (SslHandler) ctx.pipeline().get(SSL_HANDLER);
            Set<String> subjectAlternativeNames = Tls.extractSubjectAlternativeNames(sslHandler.engine());
            if (!subjectAlternativeNames.isEmpty()) {
              channelPool.addCoalescedChannel(subjectAlternativeNames, (InetSocketAddress) channel.remoteAddress(), channel, tx.key);
            }
            break;

          case ApplicationProtocolNames.HTTP_1_1:
            LOGGER.debug("ALPN led to HTTP/1 with remote {}", tx.request.getUri().getHost());
            if (tx.request.isHttp2PriorKnowledge()) {
              IllegalStateException e =  new IllegalStateException("HTTP/2 Prior knowledge was set on host " + tx.request.getUri().getHost() + " but it only supports HTTP/1");
              whenAlpn.setFailure(e);
              throw e;
            }
            tx.listener.onProtocolAwareness(false);
            ctx.pipeline()
              .addBefore(CHUNKED_WRITER_HANDLER, HTTP_CLIENT_CODEC, newHttpClientCodec())
              .addBefore(CHUNKED_WRITER_HANDLER, INFLATER_HANDLER, newHttpContentDecompressor())
              .addAfter(CHUNKED_WRITER_HANDLER, APP_HTTP_HANDLER, new HttpAppHandler(DefaultHttpClient.this, channelPool, config));
            whenAlpn.setSuccess(ctx.channel());
            break;

          default:
            IllegalStateException e = new IllegalStateException("Unknown protocol: " + protocol);
            whenAlpn.setFailure(e);
            ctx.close();
            // FIXME do we really need to throw?
            throw e;
        }
      }
    });

    whenAlpn.addListener(f -> {
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
  public void flushClientIdChannels(long clientId) {
    EventLoop eventLoop = eventLoopPicker.eventLoopWithAffinity(clientId);
    if (eventLoop.inEventLoop()) {
      eventLoopResources(eventLoop).channelPool.flushClientIdChannelPoolPartitions(clientId);
    } else {
      eventLoop.execute(() -> eventLoopResources(eventLoop).channelPool.flushClientIdChannelPoolPartitions(clientId));
    }
  }
}
