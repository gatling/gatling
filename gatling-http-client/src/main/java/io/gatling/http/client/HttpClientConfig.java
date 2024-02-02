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

package io.gatling.http.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;

public final class HttpClientConfig {

  private long connectTimeout = 5_000;

  private Charset defaultCharset = UTF_8;

  private long handshakeTimeout = 10_000;

  private SslContextsHolder defaultSslContextHolder;

  private boolean enableSni;

  private boolean enableHostnameVerification;

  private boolean useNativeTransport;

  private boolean useIoUring;

  private long channelPoolIdleTimeout = 30_000;

  private boolean tcpNoDelay;

  private boolean soKeepAlive;

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

  public HttpClientConfig setUseIoUring(boolean useIoUring) {
    this.useIoUring = useIoUring;
    return this;
  }

  public boolean isUseIoUring() {
    return useIoUring;
  }

  public HttpClientConfig setChannelPoolIdleTimeout(long channelPoolIdleTimeout) {
    this.channelPoolIdleTimeout = channelPoolIdleTimeout;
    return this;
  }

  public long getChannelPoolIdleTimeout() {
    return channelPoolIdleTimeout;
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

  public SslContextsHolder getDefaultSslContextsHolder() {
    return defaultSslContextHolder;
  }

  public HttpClientConfig setDefaultSslContextsHolder(SslContextsHolder sslContextHolder) {
    this.defaultSslContextHolder = sslContextHolder;
    return this;
  }
}
