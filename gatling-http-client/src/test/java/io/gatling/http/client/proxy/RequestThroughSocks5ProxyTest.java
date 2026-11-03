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

import io.gatling.http.client.Request;
import io.gatling.http.client.realm.BasicRealm;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.Socks5Proxy;
import io.gatling.http.client.test.TestClient;
import io.gatling.http.client.test.TestServer;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestThroughSocks5ProxyTest extends HttpTest {

  private static TestServer target;

  @BeforeAll
  static void start() throws Throwable {
    target = new TestServer();
    target.start();
  }

  @AfterAll
  static void stop() throws Throwable {
    target.close();
  }

  private static void get(TestClient client, String url, Socks5Proxy proxy, BasicRealm realm)
      throws Exception {
    Request request =
        client
            .newRequestBuilder(HttpMethod.GET, Uri.create(url))
            .setProxyServer(new SocksProxyServer("localhost", proxy.getPort(), realm, true))
            .build();

    client
        .test(
            request,
            0,
            new TestListener() {
              @Override
              public void onComplete0() {
                assertEquals(HttpResponseStatus.OK, status);
              }
            })
        .get(TIMEOUT_SECONDS, SECONDS);
  }

  @Test
  void shouldSendHttpsRequestThroughSocks5Proxy() throws Throwable {
    try (Socks5Proxy proxy = new Socks5Proxy(null, null)) {
      withClient()
          .run(
              client ->
                  withServer(target)
                      .run(
                          server -> {
                            server.enqueueEcho();
                            get(client, server.getHttpsUrl(), proxy, null);
                            assertEquals(
                                List.of("localhost:" + server.getHttpsPort()),
                                proxy.connectTargets);
                          }));
    }
  }

  @Test
  void shouldTunnelClearHttpRequestThroughSocks5Proxy() throws Throwable {
    // unlike HTTP proxies, SOCKS proxies tunnel clear HTTP requests too
    try (Socks5Proxy proxy = new Socks5Proxy(null, null)) {
      withClient()
          .run(
              client ->
                  withServer(target)
                      .run(
                          server -> {
                            server.enqueueEcho();
                            get(client, server.getHttpUrl(), proxy, null);
                            assertEquals(
                                List.of("localhost:" + server.getHttpPort()), proxy.connectTargets);
                          }));
    }
  }

  @Test
  void shouldAuthenticateWithSocks5Proxy() throws Throwable {
    try (Socks5Proxy proxy = new Socks5Proxy("user", "password")) {
      withClient()
          .run(
              client ->
                  withServer(target)
                      .run(
                          server -> {
                            server.enqueueEcho();
                            get(
                                client,
                                server.getHttpsUrl(),
                                proxy,
                                new BasicRealm("user", "password"));
                            assertEquals(List.of("user"), proxy.authenticatedUsernames);
                          }));
    }
  }

  @Test
  void shouldFailWithWrongSocks5Credentials() throws Throwable {
    try (Socks5Proxy proxy = new Socks5Proxy("user", "password")) {
      withClient()
          .run(
              client ->
                  withServer(target)
                      .run(
                          server -> {
                            assertThrows(
                                ExecutionException.class,
                                () ->
                                    get(
                                        client,
                                        server.getHttpsUrl(),
                                        proxy,
                                        new BasicRealm("user", "wrong")));
                            assertEquals(List.of(), proxy.authenticatedUsernames);
                            assertEquals(List.of(), proxy.connectTargets);
                          }));
    }
  }
}
