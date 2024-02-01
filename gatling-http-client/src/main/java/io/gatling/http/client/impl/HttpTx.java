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

package io.gatling.http.client.impl;

import io.gatling.http.client.Http2PriorKnowledge;
import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.SslContextsHolder;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.pool.ChannelPoolKey;
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

  SslContext sslContext() {
    if (!request.isHttp2Enabled()
        || request.getHttp2PriorKnowledge() == Http2PriorKnowledge.HTTP1_ONLY) {
      return sslContextsHolder.getSslContext();
    } else {
      if (sslContextsHolder.getAlpnSslContext() == null) {
        throw new UnsupportedOperationException(
            "ALPN is not available (this path shouldn't be possible, please report).");
      }
      return sslContextsHolder.getAlpnSslContext();
    }
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
