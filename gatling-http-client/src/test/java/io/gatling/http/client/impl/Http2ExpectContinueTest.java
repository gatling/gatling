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

import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.body.string.StringRequestBodyBuilder;
import io.gatling.http.client.impl.Http2ClientHarness.ClientFrames;
import io.gatling.http.client.impl.Http2ClientHarness.RecordingListener;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import org.junit.jupiter.api.Test;

class Http2ExpectContinueTest {

  private static final String BODY = "Hello World";

  private static RequestBuilder expectContinueRequest() {
    return Http2ClientHarness.requestBuilder(HttpMethod.POST)
        .setHeaders(new DefaultHttpHeaders().add(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE))
        .setBodyBuilder(new StringRequestBodyBuilder(BODY));
  }

  private static int sendAndCheckBodyIsHeldBack(
      Http2ClientHarness harness, RecordingListener[] listener) throws Exception {
    listener[0] = harness.sendRequest(expectContinueRequest());
    ClientFrames frames = harness.clientFrames();
    assertEquals(1, frames.openedStreamIds.size());
    assertEquals("", frames.data.toString(), "body must be held back until 100-Continue");
    assertEquals(List.of(), frames.endedStreamIds);
    return frames.openedStreamIds.get(0);
  }

  @Test
  void shouldSendBodyOnce100ContinueIsReceived() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener[] listener = new RecordingListener[1];
      int streamId = sendAndCheckBodyIsHeldBack(harness, listener);

      harness.sendInterimResponse(streamId, 100);
      ClientFrames frames = harness.clientFrames();
      assertEquals(BODY, frames.data.toString(), "body must be sent once 100-Continue is received");
      assertEquals(List.of(streamId), frames.endedStreamIds);

      harness.respond(streamId);
      assertEquals(200, listener[0].status);
      assertTrue(listener[0].completed);
      assertNull(listener[0].failure);
      assertEquals(List.of(), harness.clientFrames().resetStreamIds);
    }
  }

  @Test
  void shouldIgnoreOtherInterimResponsesWhileWaitingFor100Continue() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener[] listener = new RecordingListener[1];
      int streamId = sendAndCheckBodyIsHeldBack(harness, listener);

      // 103 Early Hints
      harness.sendInterimResponse(streamId, 103);
      assertEquals("", harness.clientFrames().data.toString());
      assertEquals(0, listener[0].status, "interim responses must not be reported");

      harness.sendInterimResponse(streamId, 100);
      assertEquals(BODY, harness.clientFrames().data.toString());

      harness.respond(streamId);
      assertEquals(200, listener[0].status);
      assertTrue(listener[0].completed);
      assertNull(listener[0].failure);
    }
  }

  @Test
  void shouldResetStreamWhenServerRespondsWithoutWaitingForBody() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener[] listener = new RecordingListener[1];
      int streamId = sendAndCheckBodyIsHeldBack(harness, listener);

      harness.respond(streamId, 417);
      assertEquals(417, listener[0].status);
      assertTrue(listener[0].completed);
      assertNull(listener[0].failure);

      ClientFrames frames = harness.clientFrames();
      assertEquals("", frames.data.toString(), "body must be dropped");
      assertEquals(
          List.of(streamId),
          frames.resetStreamIds,
          "the request side of the stream must be reset, otherwise the server keeps it open");
    }
  }

  @Test
  void shouldNotResetCompletedStreams() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener = harness.sendRequest();
      int streamId = harness.streamIdsOpenedByClient().get(0);

      harness.respond(streamId);
      assertEquals(200, listener.status);
      assertEquals(List.of(), harness.clientFrames().resetStreamIds);
    }
  }
}
