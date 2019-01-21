/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.ahc.util.HttpUtils;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.netty.handler.ssl.SslContext;
import io.netty.util.ReferenceCounted;

public class HttpTx {

  final Request request;
  final HttpListener listener;
  final RequestTimeout requestTimeout;
  final ChannelPoolKey key;
  private final SslContext sslContext;
  private final SslContext alpnSslContext;

  // mutable state
  int remainingTries;
  boolean closeConnection;
  WritableRequest pendingRequestExpectingContinue;

  HttpTx(Request request, HttpListener listener, RequestTimeout requestTimeout, ChannelPoolKey key, int remainingTries, SslContext sslContext, SslContext alpnSslContext) {
    this.request = request;
    this.listener = listener;
    this.requestTimeout = requestTimeout;
    this.key = key;
    this.remainingTries = remainingTries;
    this.sslContext = sslContext;
    this.alpnSslContext = alpnSslContext;
  }

  SslContext sslContext() {
    if (request.isAlpnRequired()) {
      if (alpnSslContext == null) {
        throw new UnsupportedOperationException("ALNP is not available (this path shouldn't be possible, please report).");
      }
      return alpnSslContext;
    } else {
      return sslContext;
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
