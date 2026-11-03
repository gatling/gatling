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

package io.gatling.http.client.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.body.is.InputStreamRequestBodyBuilder;
import io.gatling.http.client.impl.Http2ClientHarness.RecordingListener;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Error;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class Http2RetryTest {

  @Test
  void shouldRetryRefusedStreamOnlyOnce() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener = harness.sendRequest();
      int streamId = harness.streamIdsOpenedByClient().get(0);

      harness.resetStream(streamId, Http2Error.REFUSED_STREAM);
      assertEquals(1, harness.retried.size(), "a refused stream must be retried");
      HttpTx tx = harness.retried.get(0);
      assertSame(listener, tx.listener);
      assertEquals(0, listener.failureCount, "a retried stream must not be failed");

      harness.resend(tx);
      int retriedStreamId = harness.streamIdsOpenedByClient().get(0);
      harness.resetStream(retriedStreamId, Http2Error.REFUSED_STREAM);
      assertEquals(1, harness.retried.size(), "a stream must only be retried once");
      assertEquals(1, listener.failureCount);
      assertTrue(listener.failure.getMessage().contains("REFUSED_STREAM"));
    }
  }

  @Test
  void shouldNotRetryRefusedStreamWhoseBodyCantBeReplayed() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener =
          harness.sendRequest(
              Http2ClientHarness.requestBuilder(HttpMethod.POST)
                  .setBodyBuilder(
                      new InputStreamRequestBodyBuilder(
                          new ByteArrayInputStream("Hello".getBytes(UTF_8)))));
      int streamId = harness.streamIdsOpenedByClient().get(0);

      harness.resetStream(streamId, Http2Error.REFUSED_STREAM);
      assertEquals(List.of(), harness.retried, "a consumed InputStream can't be sent again");
      assertEquals(1, listener.failureCount);
    }
  }

  @Test
  void shouldNotRetryStreamsResetForOtherReasons() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener = harness.sendRequest();
      int streamId = harness.streamIdsOpenedByClient().get(0);

      harness.resetStream(streamId, Http2Error.CANCEL);
      assertEquals(List.of(), harness.retried, "the server might have processed the request");
      assertEquals(1, listener.failureCount);
    }
  }

  @Test
  void shouldRetryStreamsDroppedByGoAwayWhateverTheErrorCode() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener processed = harness.sendRequest();
      RecordingListener dropped = harness.sendRequest();
      List<Integer> streamIds = harness.streamIdsOpenedByClient();
      assertEquals(2, streamIds.size());

      harness.goAway(streamIds.get(0), Http2Error.INTERNAL_ERROR);

      assertEquals(1, harness.retried.size(), "the stream dropped by the GOAWAY must be retried");
      assertSame(dropped, harness.retried.get(0).listener);
      assertEquals(0, dropped.failureCount);

      harness.respond(streamIds.get(0));
      assertEquals(200, processed.status, "the stream processed by the server must go on");
      assertEquals(0, processed.failureCount);
    }
  }
}
