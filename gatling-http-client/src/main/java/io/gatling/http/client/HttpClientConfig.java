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

package io.gatling.http.client;

import io.netty.channel.Channel;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpClientConfig {

  private long connectTimeout = 5_000;

  private Charset defaultCharset = UTF_8;

  private boolean enableZeroCopy = true;

  private boolean useOpenSsl;

  private KeyManagerFactory keyManagerFactory;

  private TrustManagerFactory trustManagerFactory;

  private long handshakeTimeout = 10_000;

  private boolean disableHttpsEndpointIdentificationAlgorithm;

  private String[] enabledSslProtocols;

  private String[] enabledSslCipherSuites;

  private int sslSessionCacheSize;

  private long sslSessionTimeout;

  private boolean disableSslSessionResumption;

  private boolean filterInsecureCipherSuites;

  private boolean useNativeTransport;

  private long channelPoolIdleTimeout = 30_000;

  private long channelPoolIdleCleanerPeriod = 1_000;

  private Consumer<Channel> additionalChannelInitializer;

  private boolean tcpNoDelay;

  private boolean soReuseAddress;

  private int maxRetry;

  private int webSocketMaxFramePayloadLength;

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

  public boolean isEnableZeroCopy() {
    return enableZeroCopy;
  }

  public HttpClientConfig setEnableZeroCopy(boolean enableZeroCopy) {
    this.enableZeroCopy = enableZeroCopy;
    return this;
  }

  public boolean isUseOpenSsl() {
    return useOpenSsl;
  }

  public HttpClientConfig setUseOpenSsl(boolean useOpenSsl) {
    this.useOpenSsl = useOpenSsl;
    return this;
  }

  public KeyManagerFactory getKeyManagerFactory() {
    return keyManagerFactory;
  }

  public HttpClientConfig setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
    this.keyManagerFactory = keyManagerFactory;
    return this;
  }

  public TrustManagerFactory getTrustManagerFactory() {
    return trustManagerFactory;
  }

  public HttpClientConfig setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
    this.trustManagerFactory = trustManagerFactory;
    return this;
  }

  public long getHandshakeTimeout() {
    return handshakeTimeout;
  }

  public HttpClientConfig setHandshakeTimeout(long handshakeTimeout) {
    this.handshakeTimeout = handshakeTimeout;
    return this;
  }

  public boolean isDisableHttpsEndpointIdentificationAlgorithm() {
    return disableHttpsEndpointIdentificationAlgorithm;
  }

  public HttpClientConfig setDisableHttpsEndpointIdentificationAlgorithm(boolean disableHttpsEndpointIdentificationAlgorithm) {
    this.disableHttpsEndpointIdentificationAlgorithm = disableHttpsEndpointIdentificationAlgorithm;
    return this;
  }

  public String[] getEnabledSslProtocols() {
    return enabledSslProtocols;
  }

  public HttpClientConfig setEnabledSslProtocols(String[] enabledSslProtocols) {
    this.enabledSslProtocols = enabledSslProtocols;
    return this;
  }

  public String[] getEnabledSslCipherSuites() {
    return enabledSslCipherSuites;
  }

  public HttpClientConfig setEnabledSslCipherSuites(String[] enabledSslCipherSuites) {
    this.enabledSslCipherSuites = enabledSslCipherSuites;
    return this;
  }

  public boolean isFilterInsecureCipherSuites() {
    return filterInsecureCipherSuites;
  }

  public HttpClientConfig setFilterInsecureCipherSuites(boolean filterInsecureCipherSuites) {
    this.filterInsecureCipherSuites = filterInsecureCipherSuites;
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

  public boolean isSoReuseAddress() {
    return soReuseAddress;
  }

  public HttpClientConfig setSoReuseAddress(boolean soReuseAddress) {
    this.soReuseAddress = soReuseAddress;
    return this;
  }

  public int getMaxRetry() {
    return maxRetry;
  }

  public HttpClientConfig setMaxRetry(int maxRetry) {
    this.maxRetry = maxRetry;
    return this;
  }

  public int getWebSocketMaxFramePayloadLength() {
    return webSocketMaxFramePayloadLength;
  }

  public HttpClientConfig setWebSocketMaxFramePayloadLength(int webSocketMaxFramePayloadLength) {
    this.webSocketMaxFramePayloadLength = webSocketMaxFramePayloadLength;
    return this;
  }

  public int getSslSessionCacheSize() {
    return sslSessionCacheSize;
  }

  public HttpClientConfig setSslSessionCacheSize(int sslSessionCacheSize) {
    this.sslSessionCacheSize = sslSessionCacheSize;
    return this;
  }

  public long getSslSessionTimeout() {
    return sslSessionTimeout;
  }

  public HttpClientConfig setSslSessionTimeout(long sslSessionTimeout) {
    this.sslSessionTimeout = sslSessionTimeout;
    return this;
  }

  public boolean isDisableSslSessionResumption() {
    return disableSslSessionResumption;
  }

  public HttpClientConfig setDisableSslSessionResumption(boolean disableSslSessionResumption) {
    this.disableSslSessionResumption = disableSslSessionResumption;
    return this;
  }
}
