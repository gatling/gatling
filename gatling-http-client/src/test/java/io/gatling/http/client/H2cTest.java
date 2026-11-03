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

package io.gatling.http.client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.test.Http2TestServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.TestClient;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.util.Pair;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** HTTP/2 without TLS, with prior knowledge. */
class H2cTest extends HttpTest {

  private static final String H2 = "h2";
  private static final String HTTP_1_1 = "http/1.1";

  private static Request request(
      TestClient client, Http2TestServer server, Http2PriorKnowledge priorKnowledge) {
    return client
        .newRequestBuilder(HttpMethod.GET, Uri.create(server.getUrl()))
        .setHttp2Enabled(true)
        .setHttp2PriorKnowledge(priorKnowledge)
        .build();
  }

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
   * @return the protocol the server received the request with
   */
  private static String get(TestClient client, Request request, EventLoop eventLoop)
      throws Exception {
    AtomicReference<String> protocol = new AtomicReference<>();
    client.test(request, 0, eventLoop, protocolListener(protocol)).get(TIMEOUT_SECONDS, SECONDS);
    return protocol.get();
  }

  @Test
  void shouldUseH2cWithPriorKnowledge() throws Throwable {
    try (Http2TestServer server = Http2TestServer.cleartext(true)) {
      withClient()
          .run(
              client ->
                  assertEquals(
                      H2,
                      get(
                          client,
                          request(client, server, Http2PriorKnowledge.HTTP2_SUPPORTED),
                          client.nextEventLoop())));
    }
  }

  @Test
  void shouldUseHttp1InClearWithoutPriorKnowledge() throws Throwable {
    try (Http2TestServer server = Http2TestServer.cleartext(false)) {
      withClient()
          .run(
              client ->
                  assertEquals(
                      HTTP_1_1,
                      get(client, request(client, server, null), client.nextEventLoop())));
    }
  }

  @Test
  void shouldReuseH2cConnection() throws Throwable {
    try (Http2TestServer server = Http2TestServer.cleartext(true)) {
      withClient()
          .run(
              client -> {
                // connections are pooled per event loop
                EventLoop eventLoop = client.nextEventLoop();
                Request request = request(client, server, Http2PriorKnowledge.HTTP2_SUPPORTED);
                assertEquals(H2, get(client, request, eventLoop));
                assertEquals(H2, get(client, request, eventLoop));
                assertEquals(1, server.connections.get(), "the connection must be reused");
              });
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSendBatchOverOneH2cConnection() throws Throwable {
    try (Http2TestServer server = Http2TestServer.cleartext(true)) {
      withClient()
          .run(
              client -> {
                AtomicReference<String> protocol1 = new AtomicReference<>();
                AtomicReference<String> protocol2 = new AtomicReference<>();
                TestListener listener1 = protocolListener(protocol1);
                TestListener listener2 = protocolListener(protocol2);
                Request request = request(client, server, Http2PriorKnowledge.HTTP2_SUPPORTED);

                client.executeHttp2(
                    new Pair[] {new Pair<>(request, listener1), new Pair<>(request, listener2)}, 0);
                listener1.getResult().get(TIMEOUT_SECONDS, SECONDS);
                listener2.getResult().get(TIMEOUT_SECONDS, SECONDS);

                assertEquals(H2, protocol1.get());
                assertEquals(H2, protocol2.get());
                assertEquals(1, server.connections.get(), "the batch must share one connection");
              });
    }
  }

  @Test
  void shouldFailClearlyWhenServerDoesntSupportH2c() throws Throwable {
    try (Http2TestServer server = Http2TestServer.cleartext(false)) {
      withClient()
          .run(
              client -> {
                ExecutionException e =
                    assertThrows(
                        ExecutionException.class,
                        () ->
                            get(
                                client,
                                request(client, server, Http2PriorKnowledge.HTTP2_SUPPORTED),
                                client.nextEventLoop()));
                assertTrue(
                    e.getCause().getMessage().contains("h2c"),
                    () -> "unexpected failure: " + e.getCause());
              });
    }
  }
}
