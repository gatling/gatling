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

class Http2FatalErrorTest {

  @Test
  void shouldFailStreamsAndReportFatalErrorInsteadOfExiting() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener crashing = harness.sendRequest();
      RecordingListener inFlight = harness.sendRequest();
      List<Integer> streamIds = harness.streamIdsOpenedByClient();
      OutOfMemoryError error = new OutOfMemoryError("emulated");
      crashing.errorOnResponse = error;

      harness.respond(streamIds.get(0));

      // the JVM is still there to run these assertions
      assertEquals(List.of(error), harness.fatalErrors, "the fatal error must be reported");
      assertSame(error, inFlight.failure, "the in-flight streams must be failed");
      assertFalse(harness.client.isActive(), "the connection must be closed");
    }
  }
}
