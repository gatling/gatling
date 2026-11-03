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

class Http2MaxConcurrentStreamsTest {

  @Test
  void shouldQueueStreamsExceedingMaxConcurrentStreams() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(1)) {
      RecordingListener listener1 = harness.sendRequest();
      RecordingListener listener2 = harness.sendRequest();

      List<Integer> firstStreamIds = harness.streamIdsOpenedByClient();
      assertNull(listener1.failure, "the first request must be sent");
      assertEquals(1, firstStreamIds.size(), "only 1 stream must be opened");
      assertNull(listener2.failure, "the second request must be queued, not failed");

      harness.respond(firstStreamIds.get(0));
      assertEquals(200, listener1.status);
      assertTrue(listener1.completed);

      List<Integer> secondStreamIds = harness.streamIdsOpenedByClient();
      assertEquals(
          1,
          secondStreamIds.size(),
          "the queued stream must be opened once the first one is closed");

      harness.respond(secondStreamIds.get(0));
      assertEquals(200, listener2.status);
      assertTrue(listener2.completed);
      assertNull(listener1.failure);
      assertNull(listener2.failure);
    }
  }

  @Test
  void shouldRetryQueuedStreamDroppedByGoAwayWithoutFailingIt() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(1)) {
      RecordingListener listener1 = harness.sendRequest();
      RecordingListener listener2 = harness.sendRequest();
      List<Integer> streamIds = harness.streamIdsOpenedByClient();
      assertEquals(1, streamIds.size());

      harness.goAway(streamIds.get(0), Http2Error.INTERNAL_ERROR);

      assertEquals(0, listener1.failureCount, "the stream processed by the server must go on");
      assertEquals(1, harness.retried.size(), "the queued stream must be retried");
      assertSame(listener2, harness.retried.get(0).listener);
      // StreamBufferingEncoder fails the queued stream's write after we retried it
      assertEquals(0, listener2.failureCount, "the retried stream must not be failed");
    }
  }
}
