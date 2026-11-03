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

import io.gatling.http.client.body.string.StringRequestBodyBuilder;
import io.gatling.http.client.test.ExpectationRejectingServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.TestClient;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * A final response, eg a 417, to a 100-continue expectation is reported as is, and the connection
 * isn't reused as the body wasn't sent.
 */
class ExpectationFailedTest extends HttpTest {

  private static final String BODY = "Hello World";

  private static int post(TestClient client, String url, EventLoop eventLoop, boolean expect)
      throws Exception {
    HttpHeaders headers = new DefaultHttpHeaders();
    if (expect) {
      headers.add(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);
    }
    Request request =
        client
            .newRequestBuilder(HttpMethod.POST, Uri.create(url))
            .setHeaders(headers)
            .setBodyBuilder(new StringRequestBodyBuilder(BODY))
            .build();

    AtomicInteger result = new AtomicInteger();
    client
        .test(
            request,
            0,
            eventLoop,
            new TestListener() {
              @Override
              public void onComplete0() {
                result.set(status.code());
              }
            })
        .get(TIMEOUT_SECONDS, SECONDS);
    return result.get();
  }

  @Test
  void shouldReport417AndNotReuseConnection() throws Throwable {
    try (ExpectationRejectingServer server = new ExpectationRejectingServer()) {
      withClient()
          .run(
              client -> {
                // connections are pooled per event loop
                EventLoop eventLoop = client.nextEventLoop();
                assertEquals(417, post(client, server.getUrl(), eventLoop, true));
                assertEquals(200, post(client, server.getUrl(), eventLoop, false));

                assertEquals(
                    List.of("expect", "body=" + BODY),
                    server.requests,
                    "the body must not be sent after the 417");
                assertEquals(
                    2,
                    server.connections.get(),
                    "the connection must not be reused as the server might still expect the body");
              });
    }
  }
}
