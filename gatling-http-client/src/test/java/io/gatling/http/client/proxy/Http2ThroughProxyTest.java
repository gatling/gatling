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

package io.gatling.http.client.proxy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.Http2PriorKnowledge;
import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.Request;
import io.gatling.http.client.test.ConnectProxy;
import io.gatling.http.client.test.Http2TlsTestServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.TestClient;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.util.Pair;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class Http2ThroughProxyTest extends HttpTest {

  private static final String H2 = "h2";
  private static final String HTTP_1_1 = "http/1.1";

  private static HttpClientConfig config() {
    // HTTP/2 requires SNI
    return new HttpClientConfig().setEnableSni(true);
  }

  /**
   * @return the protocol the origin received the request with
   */
  private static String get(TestClient client, String url, ConnectProxy proxy, boolean securedProxy)
      throws Exception {
    Request request =
        client
            .newRequestBuilder(HttpMethod.GET, Uri.create(url))
            .setHttp2Enabled(true)
            .setProxyServer(
                proxy == null
                    ? null
                    : new HttpProxyServer(
                        "localhost",
                        proxy.getPort(),
                        null,
                        securedProxy,
                        EmptyHttpHeaders.INSTANCE))
            .build();

    AtomicReference<String> protocol = new AtomicReference<>();
    client
        .test(
            request,
            0,
            new TestListener() {
              @Override
              public void onComplete0() {
                assertEquals(HttpResponseStatus.OK, status);
                protocol.set(responseBody());
              }
            })
        .get(TIMEOUT_SECONDS, SECONDS);
    return protocol.get();
  }

  @Test
  void shouldNegotiateHttp2WithoutProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true)) {
      withClient(config())
          .run(client -> assertEquals(H2, get(client, origin.getUrl(), null, false)));
    }
  }

  @Test
  void shouldFallBackToHttp1ThroughProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client -> {
                assertEquals(HTTP_1_1, get(client, origin.getUrl(), proxy, false));
                assertEquals(List.of("localhost:" + origin.getPort()), proxy.connectTargets);
              });
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFallBackToHttp1ThroughProxyForRemotesKnownToSupportHttp2() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client -> {
                Request request =
                    client
                        .newRequestBuilder(HttpMethod.GET, Uri.create(origin.getUrl()))
                        .setHttp2Enabled(true)
                        .setHttp2PriorKnowledge(Http2PriorKnowledge.HTTP2_SUPPORTED)
                        .setProxyServer(
                            new HttpProxyServer(
                                "localhost",
                                proxy.getPort(),
                                null,
                                false,
                                EmptyHttpHeaders.INSTANCE))
                        .build();
                AtomicReference<String> protocol = new AtomicReference<>();
                TestListener listener =
                    new TestListener() {
                      @Override
                      public void onComplete0() {
                        assertEquals(HttpResponseStatus.OK, status);
                        protocol.set(responseBody());
                      }
                    };
                client.executeHttp2(new Pair[] {new Pair<>(request, listener)}, 0);
                listener.getResult().get(TIMEOUT_SECONDS, SECONDS);
                assertEquals(HTTP_1_1, protocol.get());
              });
    }
  }

  @Test
  void shouldNotNegotiateHttp2WithHttpsProxyItself() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(true)) {
      withClient(config())
          .run(
              client -> {
                get(client, origin.getUrl(), proxy, true);
                assertFalse(
                    proxy.negotiatedProtocols.contains(H2),
                    "the proxy connection must not negotiate HTTP/2, the CONNECT request is HTTP/1.1: "
                        + proxy.negotiatedProtocols);
              });
    }
  }
}
