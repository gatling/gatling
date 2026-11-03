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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.Http2PriorKnowledge;
import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.Request;
import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.realm.BasicRealm;
import io.gatling.http.client.test.ConnectProxy;
import io.gatling.http.client.test.Http2TlsTestServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.TestClient;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.util.Pair;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Base64;
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

  private static RequestBuilder request(
      TestClient client,
      Http2TlsTestServer origin,
      ConnectProxy proxy,
      boolean securedProxy,
      BasicRealm proxyRealm)
      throws Exception {
    RequestBuilder requestBuilder =
        client.newRequestBuilder(HttpMethod.GET, Uri.create(origin.getUrl())).setHttp2Enabled(true);
    if (proxy != null) {
      requestBuilder.setProxyServer(
          new HttpProxyServer(
              "localhost", proxy.getPort(), proxyRealm, securedProxy, EmptyHttpHeaders.INSTANCE));
    }
    return requestBuilder;
  }

  /** a listener that records the protocol the origin received the request with */
  private static TestListener protocolListener(AtomicReference<String> protocol) {
    return new TestListener() {
      @Override
      public void onComplete0() {
        assertEquals(HttpResponseStatus.OK, status);
        protocol.set(responseBody());
      }
    };
  }

  /**
   * @return the protocol the origin received the request with
   */
  private static String get(TestClient client, RequestBuilder requestBuilder) throws Exception {
    return get(client, requestBuilder, client.nextEventLoop());
  }

  /** connections are pooled per event loop, so requests must share one to reuse connections */
  private static String get(TestClient client, RequestBuilder requestBuilder, EventLoop eventLoop)
      throws Exception {
    AtomicReference<String> protocol = new AtomicReference<>();
    client
        .test(requestBuilder.build(), 0, eventLoop, protocolListener(protocol))
        .get(TIMEOUT_SECONDS, SECONDS);
    return protocol.get();
  }

  @Test
  void shouldNegotiateHttp2WithoutProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true)) {
      withClient(config())
          .run(client -> assertEquals(H2, get(client, request(client, origin, null, false, null))));
    }
  }

  @Test
  void shouldNegotiateHttp2ThroughProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client -> {
                assertEquals(H2, get(client, request(client, origin, proxy, false, null)));
                assertEquals(List.of("localhost:" + origin.getPort()), proxy.connectTargets);
              });
    }
  }

  @Test
  void shouldReuseHttp2ConnectionThroughProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client -> {
                EventLoop eventLoop = client.nextEventLoop();
                assertEquals(
                    H2, get(client, request(client, origin, proxy, false, null), eventLoop));
                assertEquals(
                    H2, get(client, request(client, origin, proxy, false, null), eventLoop));
                assertEquals(1, proxy.connectTargets.size(), "only one tunnel must be opened");
              });
    }
  }

  @Test
  void shouldFallBackToHttp1ThroughProxyWhenRemoteDoesntSupportHttp2() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(false);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client ->
                  assertEquals(HTTP_1_1, get(client, request(client, origin, proxy, false, null))));
    }
  }

  @Test
  void shouldAuthenticateWithProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client -> {
                BasicRealm realm = new BasicRealm("user", "password");
                assertEquals(H2, get(client, request(client, origin, proxy, false, realm)));
                assertEquals(
                    List.of(
                        "Basic "
                            + Base64.getEncoder().encodeToString("user:password".getBytes(UTF_8))),
                    proxy.connectAuthorizations);
              });
    }
  }

  @Test
  void shouldNegotiateHttp2WithRemoteButNotWithHttpsProxy() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(true)) {
      withClient(config())
          .run(
              client -> {
                assertEquals(H2, get(client, request(client, origin, proxy, true, null)));
                assertFalse(
                    proxy.negotiatedProtocols.contains(H2),
                    "the proxy connection must not negotiate HTTP/2, the CONNECT request is HTTP/1.1: "
                        + proxy.negotiatedProtocols);
              });
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSendBatchThroughProxyForRemotesKnownToSupportHttp2() throws Throwable {
    try (Http2TlsTestServer origin = new Http2TlsTestServer(true);
        ConnectProxy proxy = new ConnectProxy(false)) {
      withClient(config())
          .run(
              client -> {
                AtomicReference<String> protocol1 = new AtomicReference<>();
                AtomicReference<String> protocol2 = new AtomicReference<>();
                TestListener listener1 = protocolListener(protocol1);
                TestListener listener2 = protocolListener(protocol2);
                Request request1 =
                    request(client, origin, proxy, false, null)
                        .setHttp2PriorKnowledge(Http2PriorKnowledge.HTTP2_SUPPORTED)
                        .build();
                Request request2 =
                    request(client, origin, proxy, false, null)
                        .setHttp2PriorKnowledge(Http2PriorKnowledge.HTTP2_SUPPORTED)
                        .build();

                client.executeHttp2(
                    new Pair[] {new Pair<>(request1, listener1), new Pair<>(request2, listener2)},
                    0);
                listener1.getResult().get(TIMEOUT_SECONDS, SECONDS);
                listener2.getResult().get(TIMEOUT_SECONDS, SECONDS);

                assertEquals(H2, protocol1.get());
                assertEquals(H2, protocol2.get());
                assertEquals(1, proxy.connectTargets.size(), "the batch must share one tunnel");
              });
    }
  }
}
