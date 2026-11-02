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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.TestServer;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResponseDecompressionTest extends HttpTest {

  private static final String BODY = "Hello World, Hello World, Hello World";

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

  static byte[] gzip(String s) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(s.getBytes(UTF_8));
    }
    return baos.toByteArray();
  }

  private static void enqueueGzippedResponse(byte[] gzipped) {
    server.enqueueResponse(
        (request, response, callback) -> {
          response.setStatus(HttpStatus.OK_200);
          response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/plain; charset=utf-8");
          response.getHeaders().add(HttpHeader.CONTENT_ENCODING, "gzip");
          response.write(true, ByteBuffer.wrap(gzipped), callback);
        });
  }

  private static final class BytesListener extends TestListener {
    private final boolean decompress;
    private final AtomicReference<Channel> channel;
    private final AtomicReference<byte[]> bytes;

    private BytesListener(
        boolean decompress, AtomicReference<Channel> channel, AtomicReference<byte[]> bytes) {
      this.decompress = decompress;
      this.channel = channel;
      this.bytes = bytes;
    }

    @Override
    public boolean isResponseBodyDecompressionRequired() {
      return decompress;
    }

    @Override
    public void onWrite(Channel channel) {
      this.channel.set(channel);
    }

    @Override
    public void onComplete0() {
      // Content-Encoding is retained in both cases
      assertEquals("gzip", headers.get(HttpHeaderNames.CONTENT_ENCODING));
      ByteBuf composite = Unpooled.wrappedBuffer(chunks.toArray(new ByteBuf[0]));
      try {
        bytes.set(ByteBufUtil.getBytes(composite));
      } finally {
        composite.release();
      }
    }
  }

  @Test
  void testDecompressionIsSkippedOnlyWhenListenerDoesntNeedIt() throws Throwable {
    byte[] gzipped = gzip(BODY);

    withClient()
        .run(
            client ->
                withServer(server)
                    .run(
                        server -> {
                          Request request =
                              client
                                  .newRequestBuilder(
                                      HttpMethod.GET, Uri.create(server.getHttpUrl() + "/foo"))
                                  .build();
                          // channels are pooled per event loop
                          EventLoop eventLoop = client.nextEventLoop();

                          // 1. decompression disabled: raw gzipped bytes
                          AtomicReference<Channel> channel1 = new AtomicReference<>();
                          AtomicReference<byte[]> bytes1 = new AtomicReference<>();
                          enqueueGzippedResponse(gzipped);
                          client
                              .test(
                                  request, 0, eventLoop, new BytesListener(false, channel1, bytes1))
                              .get(TIMEOUT_SECONDS, SECONDS);
                          assertArrayEquals(gzipped, bytes1.get());

                          // 2. decompression enabled on the same pooled connection: flag must not
                          // stick
                          AtomicReference<Channel> channel2 = new AtomicReference<>();
                          AtomicReference<byte[]> bytes2 = new AtomicReference<>();
                          enqueueGzippedResponse(gzipped);
                          client
                              .test(
                                  request, 0, eventLoop, new BytesListener(true, channel2, bytes2))
                              .get(TIMEOUT_SECONDS, SECONDS);
                          assertSame(channel1.get(), channel2.get());
                          assertEquals(BODY, new String(bytes2.get(), UTF_8));

                          // 3. and disabled again
                          AtomicReference<Channel> channel3 = new AtomicReference<>();
                          AtomicReference<byte[]> bytes3 = new AtomicReference<>();
                          enqueueGzippedResponse(gzipped);
                          client
                              .test(
                                  request, 0, eventLoop, new BytesListener(false, channel3, bytes3))
                              .get(TIMEOUT_SECONDS, SECONDS);
                          assertSame(channel1.get(), channel3.get());
                          assertArrayEquals(gzipped, bytes3.get());
                        }));
  }
}
