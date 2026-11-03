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

import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.impl.Http2ClientHarness.RecordingListener;
import io.netty.handler.codec.http2.Http2Error;
import java.util.List;
import org.junit.jupiter.api.Test;

class Http2StreamFailuresTest {

  @Test
  void shouldFailStreamResetByServer() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener1 = harness.sendRequest();
      RecordingListener listener2 = harness.sendRequest();
      List<Integer> streamIds = harness.streamIdsOpenedByClient();
      assertEquals(2, streamIds.size());

      harness.resetStream(streamIds.get(0), Http2Error.INTERNAL_ERROR);

      assertEquals(1, listener1.failureCount, "the reset stream must be failed right away");
      assertTrue(
          listener1.failure.getMessage().contains("INTERNAL_ERROR"),
          () -> "unexpected message: " + listener1.failure.getMessage());

      assertTrue(harness.client.isActive(), "the connection must remain usable");
      harness.respond(streamIds.get(1));
      assertEquals(200, listener2.status);
      assertTrue(listener2.completed);
      assertEquals(0, listener2.failureCount);
    }
  }

  @Test
  void shouldFailInFlightStreamsWhenServerDisconnects() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener1 = harness.sendRequest();
      RecordingListener listener2 = harness.sendRequest();
      assertEquals(2, harness.streamIdsOpenedByClient().size());

      harness.serverDisconnects();

      assertEquals(1, listener1.failureCount, "in-flight streams must be failed right away");
      assertEquals(1, listener2.failureCount, "in-flight streams must be failed right away");
    }
  }

  @Test
  void shouldFailQueuedStreamsOnlyOnceWhenServerDisconnects() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(1)) {
      RecordingListener listener1 = harness.sendRequest();
      RecordingListener listener2 = harness.sendRequest();
      assertEquals(1, harness.streamIdsOpenedByClient().size());

      harness.serverDisconnects();

      assertEquals(1, listener1.failureCount, "the in-flight stream must be failed exactly once");
      assertEquals(1, listener2.failureCount, "the queued stream must be failed exactly once");
    }
  }
}
