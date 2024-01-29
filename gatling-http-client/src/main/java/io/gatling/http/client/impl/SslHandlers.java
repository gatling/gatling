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

import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.ssl.Tls;
import io.gatling.http.client.uri.Uri;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

public class SslHandlers {

  public static SslHandler newSslHandler(
      SslContext sslContext, ByteBufAllocator allocator, Uri uri, HttpClientConfig config) {

    SSLEngine sslEngine =
        config.isEnableSni()
            ? sslContext.newEngine(allocator, Tls.domain(uri.getHost()), uri.getExplicitPort())
            : sslContext.newEngine(allocator);

    sslEngine.setUseClientMode(true);
    if (config.isEnableHostnameVerification()) {
      SSLParameters params = sslEngine.getSSLParameters();
      params.setEndpointIdentificationAlgorithm("HTTPS");
      sslEngine.setSSLParameters(params);
    }

    SslHandler sslHandler = new SslHandler(sslEngine);
    if (config.getHandshakeTimeout() > 0)
      sslHandler.setHandshakeTimeoutMillis(config.getHandshakeTimeout());
    return sslHandler;
  }
}
