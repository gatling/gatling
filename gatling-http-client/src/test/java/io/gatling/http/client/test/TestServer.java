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

package io.gatling.http.client.test;

import static io.gatling.http.client.test.TestUtils.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class TestServer implements Closeable {

  private final ConcurrentLinkedQueue<Handler> handlers = new ConcurrentLinkedQueue<>();
  private int httpPort;
  private int httpsPort;
  private Server server;

  public TestServer() {}

  public TestServer(int httpPort, int httpsPort) {
    this.httpPort = httpPort;
    this.httpsPort = httpsPort;
  }

  public void start() throws Exception {
    server = new Server();

    ServerConnector httpConnector = addHttpConnector(server);
    if (httpPort != 0) {
      httpConnector.setPort(httpPort);
    }

    server.setHandler(new QueueHandler());
    ServerConnector httpsConnector = addHttpsConnector(server);
    if (httpsPort != 0) {
      httpsConnector.setPort(httpsPort);
    }
    server.start();

    httpPort = httpConnector.getLocalPort();
    httpsPort = httpsConnector.getLocalPort();
  }

  public void enqueue(Handler handler) {
    handlers.offer(handler);
  }

  public void enqueueOk() {
    enqueueResponse(
        (request, response, callback) -> {
          response.setStatus(200);
          response.getHeaders().add(HttpHeader.CONTENT_LENGTH, 0);
          callback.succeeded();
        });
  }

  public void enqueueResponse(TriConsumer<Request, Response, Callback> c) {
    handlers.offer(new ConsumerHandler(c));
  }

  public void enqueueEcho() {
    handlers.offer(new EchoHandler());
  }

  public void enqueueRedirect(int status, String location) {
    enqueueResponse(
        (request, response, callback) -> {
          Response.sendRedirect(request, response, callback, location, true);
        });
  }

  public int getHttpPort() {
    return httpPort;
  }

  public int getHttpsPort() {
    return httpsPort;
  }

  public String getHttpUrl() {
    return "http://localhost:" + httpPort;
  }

  public String getHttpsUrl() {
    return "https://localhost:" + httpsPort;
  }

  public void reset() {
    handlers.clear();
  }

  @Override
  public void close() throws IOException {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  private static class ConsumerHandler extends Handler.Abstract {

    private final TriConsumer<Request, Response, Callback> c;

    ConsumerHandler(TriConsumer<Request, Response, Callback> c) {
      this.c = c;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
      try {
        c.accept(request, response, callback);
      } catch (Exception e) {
        callback.failed(e);
        throw e;
      } catch (Throwable e) {
        callback.failed(e);
        throw new Exception(e);
      }
      return true;
    }
  }

  public static class EchoHandler extends Handler.Abstract {

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

      String delay = request.getHeaders().get("X-Delay");
      if (delay != null) {
        try {
          Thread.sleep(Long.parseLong(delay));
        } catch (NumberFormatException | InterruptedException e1) {
          throw new Exception(e1);
        }
      }

      response.setStatus(200);

      HttpFields requestHeaders = request.getHeaders();
      HttpFields.Mutable responseHeaders = response.getHeaders();

      if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
        responseHeaders.add(HttpHeader.ALLOW, "GET,HEAD,POST,OPTIONS,TRACE");
      }

      responseHeaders.add(
          HttpHeader.CONTENT_TYPE,
          requestHeaders.get("X-IsoCharset") != null
              ? TEXT_HTML_CONTENT_TYPE_WITH_ISO_8859_1_CHARSET
              : TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET);

      responseHeaders.add("X-ClientPort", String.valueOf(Request.getRemotePort(request)));

      String pathInfo = Request.getPathInContext(request);
      if (pathInfo != null) responseHeaders.add("X-PathInfo", pathInfo);

      String queryString = request.getHttpURI().getQuery();
      if (queryString != null) responseHeaders.add("X-QueryString", queryString);

      requestHeaders.forEach(it -> responseHeaders.add("X-" + it.getName(), it.getValue()));

      Request.getParameters(request)
          .forEach(it -> responseHeaders.add("X-" + it.getName(), it.getValue()));

      Request.getCookies(request).forEach(c -> Response.addCookie(response, c));

      Content.copy(request, response, callback);

      return true;
    }
  }

  private class QueueHandler extends Handler.Abstract {

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

      Handler handler = TestServer.this.handlers.poll();
      if (handler == null) {
        Response.writeError(
            request,
            response,
            callback,
            HttpStatus.INTERNAL_SERVER_ERROR_500,
            "No handler enqueued");
        callback.succeeded();
        return true;
      } else {
        return handler.handle(request, response, callback);
      }
    }
  }
}
