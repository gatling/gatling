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

import io.gatling.http.client.Http2FlowControlWindows;
import io.gatling.http.client.impl.Http2ClientHarness.ClientFrames;
import io.gatling.http.client.impl.Http2ClientHarness.RecordingListener;
import io.netty.handler.codec.http2.Http2CodecUtil;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Http2FlowControlWindowsTest {

  private static final int KB = 1024;
  private static final int MAX_FRAME_SIZE = 16 * KB;

  private static void assertPrefaceAdvertises(
      ClientFrames preface, int streamWindowSize, int connectionWindowSize) {
    assertEquals(1, preface.settings.size());
    assertEquals(Integer.valueOf(streamWindowSize), preface.settings.get(0).initialWindowSize());
    assertEquals(
        Map.of(
            Http2CodecUtil.CONNECTION_STREAM_ID,
            connectionWindowSize - Http2CodecUtil.DEFAULT_WINDOW_SIZE),
        preface.windowUpdates,
        "the connection window must be enlarged with a WINDOW_UPDATE");
  }

  @Test
  void shouldAdvertiseBrowserLikeWindowsByDefault() throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      assertPrefaceAdvertises(harness.preface, 6 * KB * KB, 15 * KB * KB);
    }
  }

  @Test
  void shouldAdvertiseConfiguredWindows() throws Exception {
    try (Http2ClientHarness harness =
        new Http2ClientHarness(100, new Http2FlowControlWindows(KB * KB, 2 * KB * KB))) {
      assertPrefaceAdvertises(harness.preface, KB * KB, 2 * KB * KB);
    }
  }

  @Test
  void shouldNotSendWindowUpdateWhenConnectionWindowIsTheProtocolDefault() throws Exception {
    try (Http2ClientHarness harness =
        new Http2ClientHarness(
            100, new Http2FlowControlWindows(KB * KB, Http2CodecUtil.DEFAULT_WINDOW_SIZE))) {
      assertEquals(Map.of(), harness.preface.windowUpdates);
    }
  }

  @Test
  void shouldLetServerSendMoreThanTheProtocolDefaultWindowWithoutWaitingForWindowUpdate()
      throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      harness.ackSettings();
      RecordingListener listener = harness.sendRequest();
      int streamId = harness.streamIdsOpenedByClient().get(0);

      harness.sendResponseHeaders(streamId);
      // 256KB, way above the 64KB protocol default
      int frames = 16;
      for (int i = 0; i < frames; i++) {
        harness.sendData(streamId, MAX_FRAME_SIZE, i == frames - 1);
      }

      // with the 64KB defaults, the server would have had to wait for several WINDOW_UPDATEs,
      // each costing a round trip
      assertEquals(
          Map.of(),
          harness.clientFrames().windowUpdates,
          "the windows must be large enough for the server not to wait for any WINDOW_UPDATE");
      assertTrue(harness.client.isActive());
      assertNull(listener.failure);
      assertEquals(200, listener.status);
      assertTrue(listener.completed);
    }
  }

  @Test
  void shouldRejectInvalidWindows() {
    assertThrows(IllegalArgumentException.class, () -> new Http2FlowControlWindows(0, 15 * KB));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Http2FlowControlWindows(KB, Http2CodecUtil.DEFAULT_WINDOW_SIZE - 1));
  }
}
