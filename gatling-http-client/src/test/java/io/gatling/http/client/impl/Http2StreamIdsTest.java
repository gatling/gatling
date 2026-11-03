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
import java.util.List;
import org.junit.jupiter.api.Test;

class Http2StreamIdsTest {

  @Test
  void shouldUseOddIncreasingStreamIds() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      harness.sendRequest();
      harness.sendRequest();
      harness.sendRequest();
      // like Netty, 1 is left for the HTTP/1.1 upgrade
      assertEquals(List.of(3, 5, 7), harness.streamIdsOpenedByClient());
    }
  }

  @Test
  void shouldRetryOnAnotherConnectionOnceStreamIdsAreExhausted() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      // the last stream id a client can use
      harness.connection().local().createStream(Integer.MAX_VALUE, true).close();

      RecordingListener listener = harness.sendRequest();

      assertEquals(List.of(), harness.streamIdsOpenedByClient());
      assertEquals(0, listener.failureCount, "the request must not be failed");
      assertEquals(1, harness.retried.size(), "the request must be sent on another connection");
      assertSame(listener, harness.retried.get(0).listener);
      assertFalse(harness.client.isActive(), "the exhausted connection must be closed");
    }
  }
}
