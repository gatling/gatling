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

import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.file.FileRequestBodyBuilder;
import io.gatling.http.client.body.is.InputStreamRequestBodyBuilder;
import io.gatling.http.client.impl.Http2ClientHarness.ClientFrames;
import io.gatling.http.client.impl.Http2ClientHarness.RecordingListener;
import io.netty.handler.codec.http.HttpMethod;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Http2StreamedBodyTest {

  private static final String BODY = "Hello World";

  private static void assertBodyIsSentInDataFrames(RequestBodyBuilder bodyBuilder)
      throws Exception {
    try (Http2ClientHarness harness = new Http2ClientHarness(100)) {
      RecordingListener listener =
          harness.sendRequest(
              Http2ClientHarness.requestBuilder(HttpMethod.POST).setBodyBuilder(bodyBuilder));

      ClientFrames frames = harness.clientFrames();
      assertEquals(1, frames.openedStreamIds.size());
      int streamId = frames.openedStreamIds.get(0);
      assertEquals(BODY, frames.data.toString());
      assertEquals(List.of(streamId), frames.endedStreamIds);

      harness.respond(streamId);
      assertEquals(200, listener.status);
      assertTrue(listener.completed);
      assertNull(listener.failure);
    }
  }

  @Test
  void shouldSendInputStreamBodyInDataFrames() throws Exception {
    assertBodyIsSentInDataFrames(
        new InputStreamRequestBodyBuilder(new ByteArrayInputStream(BODY.getBytes(UTF_8))));
  }

  @Test
  void shouldSendFileBodyInDataFrames(@TempDir Path tempDir) throws Exception {
    Path file = Files.writeString(tempDir.resolve("body.txt"), BODY);
    assertBodyIsSentInDataFrames(new FileRequestBodyBuilder(file.toFile()));
  }
}
