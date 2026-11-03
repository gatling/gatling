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

import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.TestServer;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** HTTP/1.1 fatal errors are reported to the configured handler, instead of exiting the JVM. */
class FatalErrorTest extends HttpTest {

  private static TestServer server;

  @BeforeAll
  static void start() throws Throwable {
    server = new TestServer();
    server.start();
  }

  @AfterAll
  static void stop() throws Throwable {
    server.close();
  }

  @Test
  void shouldFailRequestAndReportFatalErrorInsteadOfExiting() throws Throwable {
    List<Throwable> fatalErrors = new CopyOnWriteArrayList<>();
    // reported once the request has been failed
    CountDownLatch fatalErrorReported = new CountDownLatch(1);
    OutOfMemoryError error = new OutOfMemoryError("emulated");

    withClient(
            new HttpClientConfig()
                .setFatalErrorHandler(
                    e -> {
                      fatalErrors.add(e);
                      fatalErrorReported.countDown();
                    }))
        .run(
            client ->
                withServer(server)
                    .run(
                        server -> {
                          server.enqueueOk();
                          Request request =
                              client
                                  .newRequestBuilder(
                                      HttpMethod.GET, Uri.create(server.getHttpUrl()))
                                  .build();

                          ExecutionException e =
                              assertThrows(
                                  ExecutionException.class,
                                  () ->
                                      client
                                          .test(
                                              request,
                                              0,
                                              new TestListener() {
                                                @Override
                                                public void onHttpResponse(
                                                    HttpResponseStatus status,
                                                    HttpHeaders headers) {
                                                  throw error;
                                                }

                                                @Override
                                                public void onComplete0() {}
                                              })
                                          .get(TIMEOUT_SECONDS, SECONDS));

                          // the JVM is still there to run these assertions
                          assertSame(error, e.getCause(), "the request must be failed");
                          assertTrue(fatalErrorReported.await(TIMEOUT_SECONDS, SECONDS));
                          assertEquals(
                              List.of(error), fatalErrors, "the fatal error must be reported");
                        }));
  }
}
