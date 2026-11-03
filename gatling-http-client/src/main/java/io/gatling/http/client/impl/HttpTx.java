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

import io.gatling.http.client.Http2PriorKnowledge;
import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.SslContextsHolder;
import io.gatling.http.client.body.is.InputStreamRequestBody;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.util.HttpUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.util.ReferenceCounted;

public final class HttpTx {

  public enum ChannelState {
    NEW,
    POOLED,
    RETRY
  }

  final Request request;
  final HttpListener listener;
  final RequestTimeout requestTimeout;
  final ChannelPoolKey key;
  private final SslContextsHolder sslContextsHolder;

  // mutable state
  ChannelState channelState;
  boolean closeConnection;
  WritableRequest pendingRequestExpectingContinue;

  HttpTx(
      Request request,
      HttpListener listener,
      RequestTimeout requestTimeout,
      ChannelPoolKey key,
      SslContextsHolder sslContextsHolder) {
    this.request = request;
    this.listener = listener;
    this.requestTimeout = requestTimeout;
    this.key = key;
    this.channelState = ChannelState.POOLED; // set to NEW in DefaultHttpClient#sendTxWithNewChannel
    this.sslContextsHolder = sslContextsHolder;
    this.closeConnection = HttpUtils.isConnectionClose(request.getHeaders());
  }

  /** Whether HTTP/2 should be negotiated with ALPN for this request. */
  static boolean isHttp2Candidate(Request request) {
    Uri uri = request.getUri();
    return request.isHttp2Enabled()
        && request.getHttp2PriorKnowledge() != Http2PriorKnowledge.HTTP1_ONLY
        && uri.isSecured()
        // secured requests are always tunneled through proxies (HTTP CONNECT or SOCKS), so HTTP/2
        // can
        // be negotiated with the remote
        && !uri.isWebSocket();
  }

  /**
   * Connection coalescing relies on the remote address, which is the proxy's when using one, and is
   * left unresolved for the proxy to resolve the remote.
   */
  boolean canCoalesceConnection() {
    return isHttp2Candidate() && request.getProxyServer() == null;
  }

  boolean isHttp2Candidate() {
    return isHttp2Candidate(request);
  }

  /**
   * Whether HTTP/2 should be used without TLS (h2c). Only with prior knowledge, ie when the remote
   * is known to support HTTP/2, as the HTTP/1.1 Upgrade mechanism is deprecated (RFC 9113 section
   * 3.1). Not through a proxy, as HTTP proxies forward clear HTTP requests themselves.
   */
  static boolean isH2cCandidate(Request request) {
    Uri uri = request.getUri();
    return request.isHttp2Enabled()
        && request.getHttp2PriorKnowledge() == Http2PriorKnowledge.HTTP2_SUPPORTED
        && !uri.isSecured()
        && !uri.isWebSocket()
        && request.getProxyServer() == null;
  }

  boolean isH2cCandidate() {
    return isH2cCandidate(request);
  }

  /** The SslContext for the TLS connection with the remote, with ALPN when trying HTTP/2. */
  SslContext sslContext() {
    if (isHttp2Candidate()) {
      if (sslContextsHolder.getAlpnSslContext() == null) {
        throw new UnsupportedOperationException(
            "ALPN is not available (this path shouldn't be possible, please report).");
      }
      return sslContextsHolder.getAlpnSslContext();
    } else {
      return sslContextsHolder.getSslContext();
    }
  }

  /**
   * The SslContext for the TLS connection with an HTTPS proxy: never with ALPN, as the CONNECT
   * request is HTTP/1.1, the remote's own TLS connection being tunneled.
   */
  SslContext proxySslContext() {
    return sslContextsHolder.getSslContext();
  }

  boolean isBodyReplayable() {
    // InputStreamRequestBody can't be replayed once consumed
    return !(request.getBody() instanceof InputStreamRequestBody
        && ((InputStreamRequestBody) request.getBody()).isConsumed());
  }

  void releasePendingRequestExpectingContinue() {
    if (pendingRequestExpectingContinue != null) {
      Object content = pendingRequestExpectingContinue.getContent();
      if (content instanceof ReferenceCounted) {
        ((ReferenceCounted) content).release();
      }
      pendingRequestExpectingContinue = null;
    }
  }
}
