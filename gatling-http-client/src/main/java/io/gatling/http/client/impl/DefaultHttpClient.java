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

package io.gatling.http.client.impl;

import io.gatling.http.client.HttpClient;
import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.ahc.uri.Uri;
import io.gatling.http.client.body.InputStreamRequestBody;
import io.gatling.http.client.pool.ChannelPool;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.realm.DigestRealm;
import io.gatling.http.client.realm.Realm;
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
import io.netty.handler.ssl.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.*;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.gatling.http.client.ahc.util.MiscUtils.isNonEmpty;
import static java.util.Collections.singletonList;

public class DefaultHttpClient implements HttpClient {

  static {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
  }

  private static final String PROXY_HANDLER = "proxy";
  private static final String SSL_HANDLER = "ssl";
  private static final String HTTP_CLIENT_CODEC = "http";
  private static final String INFLATER_HANDLER = "inflater";
  private static final String CHUNKED_WRITER_HANDLER = "chunked-writer";
  private static final String DIGEST_AUTH_HANDLER = "digest";
  private static final String APP_HTTP_HANDLER = "app-http";
  private static final String WS_OBJECT_AGGREGATOR = "ws-aggregator";
  private static final String WS_COMPRESSION = "ws-compression";
  private static final String APP_WS_HANDLER = "app-ws";

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

    private final Bootstrap bootstrap;
    private final Bootstrap wsBoostrap;
    private final ChannelPool channelPool;

    private void addDefaultHttpHandlers(ChannelPipeline pipeline) {
      pipeline
        .addLast(HTTP_CLIENT_CODEC, newHttpClientCodec())
        .addLast(INFLATER_HANDLER, newHttpContentDecompressor())
        .addLast(CHUNKED_WRITER_HANDLER, new ChunkedWriteHandler())
        .addLast(APP_HTTP_HANDLER, new HttpHandler(DefaultHttpClient.this, channelPool, config));
    }

    private EventLoopResources(EventLoop eventLoop) {
      channelPool = new ChannelPool();
      long channelPoolIdleCleanerPeriod = config.getChannelPoolIdleCleanerPeriod();
      eventLoop.scheduleWithFixedDelay(
        () -> channelPool.closeIdleChannels(config.getChannelPoolIdleTimeout()),
        channelPoolIdleCleanerPeriod,
        channelPoolIdleCleanerPeriod,
        TimeUnit.MILLISECONDS);
      bootstrap = new Bootstrap()
        .channelFactory(config.isUseNativeTransport() ? EpollSocketChannel::new : NioSocketChannel::new)
        .group(eventLoop)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout())
        .option(ChannelOption.SO_REUSEADDR, config.isSoReuseAddress())
        .option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) {
            addDefaultHttpHandlers(ch.pipeline());
            if (config.getAdditionalChannelInitializer() != null) {
              config.getAdditionalChannelInitializer().accept(ch);
            }
          }
        });

      wsBoostrap = bootstrap.clone().handler(new ChannelInitializer<Channel>() {

        @Override
        protected void initChannel(Channel ch) {
          ch.pipeline()
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

      return bootstrap.clone().handler(new ChannelInitializer<Channel>() {

        @Override
        protected void initChannel(Channel ch) {
          ChannelPipeline pipeline = ch.pipeline()
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
  private final SslContext sslContext;
  private final HttpClientConfig config;
  private final MultithreadEventExecutorGroup eventLoopGroup;
  private final AffinityEventLoopPicker eventLoopPicker;
  private final ChannelGroup channelGroup;
  private final ThreadLocal<EventLoopResources> eventLoopResources = new ThreadLocal<>();

  public DefaultHttpClient(HttpClientConfig config) {
    this.config = config;
    try {
      SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

      if (config.getSslSessionCacheSize() > 0) {
        sslContextBuilder.sessionCacheSize(config.getSslSessionCacheSize());
      }

      if (config.getSslSessionTimeout() > 0) {
        sslContextBuilder.sessionTimeout(config.getSslSessionTimeout());
      }

      if (isNonEmpty(config.getEnabledSslProtocols())) {
        sslContextBuilder.protocols(config.getEnabledSslProtocols());
      }

      if (isNonEmpty(config.getEnabledSslCipherSuites())) {
        sslContextBuilder.ciphers(Arrays.asList(config.getEnabledSslCipherSuites()));
      } else if (!config.isFilterInsecureCipherSuites()) {
        sslContextBuilder.ciphers(null, IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS);
      }

      this.sslContext = sslContextBuilder
        .sslProvider(config.isUseOpenSsl() ? SslProvider.OPENSSL : SslProvider.JDK)
        .keyManager(config.getKeyManagerFactory())
        .trustManager(config.getTrustManagerFactory())
        .build();
    } catch (SSLException e) {
      throw new IllegalArgumentException("Impossible to create SslContext", e);
    }

    eventLoopGroup = config.isUseNativeTransport() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
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
  public void sendRequest(Request request, long clientId, boolean shared, HttpListener listener) {

    if (isClosed()) {
      return;
    }

    EventLoop eventLoop = eventLoopPicker.eventLoopWithAffinity(clientId);

    if (eventLoop.inEventLoop()) {
      sendRequest0(request, clientId, shared, listener, eventLoop);
    } else {
      eventLoop.execute(() -> sendRequest0(request, clientId, shared, listener, eventLoop));
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

  private void sendRequest0(Request request, long clientId, boolean shared, HttpListener listener, EventLoop eventLoop) {

    RequestTimeout requestTimeout = RequestTimeout.newRequestTimeout(request.getRequestTimeout(), listener, eventLoop);
    ChannelPoolKey key = ChannelPoolKey.newKey(shared ? clientId : -1, request.getUri(), request.getVirtualHost(), request.getProxyServer());
    HttpTx tx = new HttpTx(request, listener, requestTimeout, key, config.getMaxRetry());
    sendRequest1(tx, eventLoop);
  }

  boolean retry(HttpTx tx, EventLoop eventLoop) {

    if (isClosed()) {
      return false;
    }

    if (tx.remainingTries > 0 && ! (tx.request.getBody() instanceof InputStreamRequestBody)) {
      tx.remainingTries = tx.remainingTries - 1;
      sendRequest1(tx, eventLoop);
      return true;
    }
    return false;
  }

  private void sendRequest1(HttpTx tx, EventLoop eventLoop) {

    EventLoopResources resources = eventLoopResources(eventLoop);
    Request request = tx.request;
    HttpListener listener = tx.listener;
    RequestTimeout requestTimeout = tx.requestTimeout;

    // use a fresh channel for WebSocket
    Channel pooledChannel = request.getUri().isWebSocket() ? null : poolChannelRec(resources, tx.key);
    tx.usingPooledChannel = pooledChannel != null;

    if (tx.usingPooledChannel) {
      sendRequest(tx, pooledChannel);

    } else {
      resolveRemoteAddresses(request, eventLoop, listener, requestTimeout)
        .addListener((Future<List<InetSocketAddress>> whenRemoteAddresses) -> {
          if (requestTimeout.isDone()) {
            return;
          }

          if (whenRemoteAddresses.isSuccess()) {
            openNewChannel(request, eventLoop, resources, whenRemoteAddresses.getNow(), listener, requestTimeout)
              .addListener((Future<Channel> whenNewChannel) -> {
                if (whenNewChannel.isSuccess()) {
                  Channel channel = whenNewChannel.getNow();
                  if (requestTimeout.isDone()) {
                    channel.close();
                    return;
                  }

                  channelGroup.add(channel);
                  resources.channelPool.register(channel, tx.key);

                  if (request.getUri().isSecured()) {
                    installSslHandler(tx, channel).addListener(f -> {
                      if (requestTimeout.isDone() || !f.isSuccess()) {
                        channel.close();
                        return;
                      }
                      sendRequest(tx, channel);
                    });
                  } else {
                    sendRequest(tx, channel);
                  }
                }
              });
          }
        });
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

  private Channel poolChannelRec(EventLoopResources resources, ChannelPoolKey key) {
    Channel pooledChannel = resources.channelPool.acquire(key);
    if (pooledChannel == null) {
      return null;

    } else if (pooledChannel.isActive()) {
      return pooledChannel;

    } else {
      return poolChannelRec(resources, key);
    }
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
          resources.bootstrap;

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
    // FIXME For bootstrapping with a ProxyHandler, we must wait for the ProxyConnectEvent
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
          listener.onThrowable(f.cause());
          channelPromise.setFailure(f.cause());
        }
      }
    });
  }

  private Future<Channel> installSslHandler(HttpTx tx, Channel channel) {
    SslHandler sslHandler = SslHandlers.newSslHandler(sslContext, channel, tx.request.getUri(), tx.request.getVirtualHost(), config);
    tx.listener.onTlsHandshakeAttempt();

    channel.pipeline().addBefore(HTTP_CLIENT_CODEC, SSL_HANDLER, sslHandler);
    return sslHandler.handshakeFuture().addListener(f -> {

      SSLSession sslSession = sslHandler.engine().getHandshakeSession();
      if (sslSession != null && sslSession.isValid() && config.isDisableSslSessionResumption()) {
        sslSession.invalidate();
      }

      if (tx.requestTimeout.isDone()) {
        return;
      }

      if (f.isSuccess()) {
        tx.listener.onTlsHandshakeSuccess();
      } else {
        tx.listener.onTlsHandshakeFailure(f.cause());
        tx.listener.onThrowable(f.cause());
      }
    });
  }

  private void sendRequest(HttpTx tx, Channel channel) {

    if (isClosed()) {
      return;
    }

    tx.requestTimeout.setChannel(channel);

    Realm realm = tx.request.getRealm();
    if (realm instanceof DigestRealm) {
      // FIXME is it the right place?
      // FIXME wouldn't work for WebSocket
      channel.pipeline().addBefore(APP_HTTP_HANDLER, DIGEST_AUTH_HANDLER, new DigestAuthHandler(tx, (DigestRealm) realm, config));
    }
    channel.write(tx);
  }

  @Override
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void flushChannelPoolPartitions(Predicate<ChannelPoolKey> predicate, long affinity) {
    EventLoop eventLoop = eventLoopPicker.eventLoopWithAffinity(affinity);

    if (eventLoop.inEventLoop()) {
      eventLoopResources(eventLoop).channelPool.flushChannelPoolPartitions(predicate);
    } else {
      eventLoop.execute(() -> eventLoopResources(eventLoop).channelPool.flushChannelPoolPartitions(predicate));
    }
  }
}
