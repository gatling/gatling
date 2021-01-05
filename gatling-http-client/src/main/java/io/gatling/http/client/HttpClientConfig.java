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

package io.gatling.http.client;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpClientConfig {

  private long connectTimeout = 5_000;

  private Charset defaultCharset = UTF_8;

  private long handshakeTimeout = 10_000;

  private SslContext defaultSslContext;

  private SslContext defaultAlpnSslContext;

  private boolean enableSni;

  private boolean enableHostnameVerification;

  private boolean useNativeTransport;

  private long channelPoolIdleTimeout = 30_000;

  private long channelPoolIdleCleanerPeriod = 1_000;

  private Consumer<Channel> additionalChannelInitializer;

  private boolean tcpNoDelay;

  private boolean soKeepAlive;

  private boolean soReuseAddress;

  private String threadPoolName = "gatling-http-client";

  public long getConnectTimeout() {
    return connectTimeout;
  }

  public HttpClientConfig setConnectTimeout(long connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public Charset getDefaultCharset() {
    return defaultCharset;
  }

  public HttpClientConfig setDefaultCharset(Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
    return this;
  }

  public long getHandshakeTimeout() {
    return handshakeTimeout;
  }

  public HttpClientConfig setHandshakeTimeout(long handshakeTimeout) {
    this.handshakeTimeout = handshakeTimeout;
    return this;
  }

  public boolean isEnableSni() {
    return enableSni;
  }

  public HttpClientConfig setEnableSni(boolean enableSni) {
    this.enableSni = enableSni;
    return this;
  }

  public boolean isEnableHostnameVerification() {
    return enableHostnameVerification;
  }

  public HttpClientConfig setEnableHostnameVerification(boolean enableHostnameVerification) {
    this.enableHostnameVerification = enableHostnameVerification;
    return this;
  }

  public HttpClientConfig setUseNativeTransport(boolean useNativeTransport) {
    this.useNativeTransport = useNativeTransport;
    return this;
  }

  public boolean isUseNativeTransport() {
    return useNativeTransport;
  }

  public HttpClientConfig setChannelPoolIdleTimeout(long channelPoolIdleTimeout) {
    this.channelPoolIdleTimeout = channelPoolIdleTimeout;
    return this;
  }

  public long getChannelPoolIdleTimeout() {
    return channelPoolIdleTimeout;
  }

  public HttpClientConfig setChannelPoolIdleCleanerPeriod(long channelPoolIdleCleanerPeriod) {
    this.channelPoolIdleCleanerPeriod = channelPoolIdleCleanerPeriod;
    return this;
  }

  public long getChannelPoolIdleCleanerPeriod() {
    return channelPoolIdleCleanerPeriod;
  }

  public HttpClientConfig setAdditionalChannelInitializer(Consumer<Channel> additionalChannelInitializer) {
    this.additionalChannelInitializer = additionalChannelInitializer;
    return this;
  }

  public Consumer<Channel> getAdditionalChannelInitializer() {
    return additionalChannelInitializer;
  }

  public boolean isTcpNoDelay() {
    return tcpNoDelay;
  }

  public HttpClientConfig setTcpNoDelay(boolean tcpNoDelay) {
    this.tcpNoDelay = tcpNoDelay;
    return this;
  }

  public boolean isSoKeepAlive() {
    return soKeepAlive;
  }

  public HttpClientConfig setSoKeepAlive(boolean soKeepAlive) {
    this.soKeepAlive = soKeepAlive;
    return this;
  }

  public boolean isSoReuseAddress() {
    return soReuseAddress;
  }

  public HttpClientConfig setSoReuseAddress(boolean soReuseAddress) {
    this.soReuseAddress = soReuseAddress;
    return this;
  }

  public String getThreadPoolName() {
    return threadPoolName;
  }

  public HttpClientConfig setThreadPoolName(String threadPoolName) {
    this.threadPoolName = threadPoolName;
    return this;
  }

  public SslContext getDefaultSslContext() {
    return defaultSslContext;
  }

  public HttpClientConfig setDefaultSslContext(SslContext sslContext) {
    this.defaultSslContext = sslContext;
    return this;
  }

  public SslContext getDefaultAlpnSslContext() {
    return defaultAlpnSslContext;
  }

  public HttpClientConfig setDefaultAlpnSslContext(SslContext sslContext) {
    this.defaultAlpnSslContext = sslContext;
    return this;
  }
}
