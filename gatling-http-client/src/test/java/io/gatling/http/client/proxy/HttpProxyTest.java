/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.http.client.Request;
import io.gatling.http.client.test.TestServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpProxyTest extends HttpTest {

  public static class ProxyHandler extends AbstractHandler {
    public void handle(String s, org.eclipse.jetty.server.Request r, HttpServletRequest request, HttpServletResponse response) throws IOException {
      if ("GET".equalsIgnoreCase(request.getMethod())) {
        response.addHeader("target", r.getHttpURI().getPath());
        response.setStatus(HttpServletResponse.SC_OK);
      } else {
        // this handler is to handle POST request
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
      r.setHandled(true);
    }
  }

  private static class HttpProxy implements AutoCloseable {

    private final Server jetty;

    private final int port;

    public HttpProxy(int port) {
      this.port = port;
      jetty = new Server();
      ServerConnector connector = new ServerConnector(jetty);
      connector.setPort(8888);
      jetty.addConnector(connector);
      jetty.setHandler(new ProxyHandler());
    }

    public int getPort() {
      return port;
    }

    public void start() throws Exception {
      jetty.start();
    }

    @Override
    public void close() throws Exception {
      jetty.stop();
    }
  }

  private static HttpProxy proxy;
  private static TestServer target;

  @BeforeAll
  static void start() throws Throwable {
    target = new TestServer();
    target.start();
    proxy = new HttpProxy(8888);
    proxy.start();
  }

  @AfterAll
  static void stop() throws Throwable {
    target.close();
    proxy.close();
  }

  @Test
  void testRequestProxy() throws Throwable {
    withClient().run(client ->
      withServer(target).run(target -> {

        target.enqueueEcho();

        HttpHeaders h = new DefaultHttpHeaders();
        for (int i = 1; i < 5; i++) {
          h.add("Test" + i, "Test" + i);
        }

        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(target.getHttpUrl()))
          .setHeaders(h)
          .setProxyServer(new HttpProxyServer("localhost", proxy.getPort(), 0, null))
          .build();

        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(HttpResponseStatus.OK, status);
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      })
    );
  }
}
