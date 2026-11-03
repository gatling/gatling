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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RequestTimeoutTest {

  private static final long TIMEOUT_MS = 1000;

  /** RequestTimeout expects an InetSocketAddress, unlike EmbeddedChannel's. */
  private static EmbeddedChannel socketChannel() {
    return new EmbeddedChannel() {
      @Override
      public SocketAddress remoteAddress() {
        return new InetSocketAddress("127.0.0.1", 443);
      }
    };
  }

  @Test
  void shouldKeepOriginalDeadlineAndNotifyOnlyOnceWhenStartedAgainOnRetry() {
    EmbeddedChannel channel = new EmbeddedChannel();
    RecordingListener listener = new RecordingListener();
    RequestTimeout requestTimeout = RequestTimeout.requestTimeout(TIMEOUT_MS, listener);

    requestTimeout.start(channel.eventLoop());
    channel.advanceTimeBy(TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);
    // a retry starts the timeout again
    requestTimeout.start(channel.eventLoop());

    channel.advanceTimeBy(TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);
    channel.runScheduledPendingTasks();
    assertEquals(1, listener.failureCount, "the original deadline must be kept");
    assertTrue(requestTimeout.isDone());

    channel.advanceTimeBy(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    channel.runScheduledPendingTasks();
    assertEquals(1, listener.failureCount, "the listener must only be notified once");

    channel.finishAndReleaseAll();
  }

  @Test
  void shouldNotResetPreviousConnectionStreamIdOnNewConnection() {
    List<Object> userEvents = new ArrayList<>();
    EmbeddedChannel previousChannel = socketChannel();
    EmbeddedChannel newChannel = socketChannel();
    newChannel
        .pipeline()
        .addLast(
            DefaultHttpClient.APP_HTTP2_HANDLER,
            new ChannelInboundHandlerAdapter() {
              @Override
              public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                userEvents.add(evt);
              }
            });
    RecordingListener listener = new RecordingListener();
    RequestTimeout requestTimeout = RequestTimeout.requestTimeout(TIMEOUT_MS, listener);
    requestTimeout.start(newChannel.eventLoop());

    requestTimeout.setChannel(previousChannel);
    requestTimeout.setStreamId(3);
    // retried on a new connection, times out before the new stream is written
    requestTimeout.setChannel(newChannel);

    newChannel.advanceTimeBy(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    newChannel.runScheduledPendingTasks();
    assertEquals(1, listener.failureCount);
    assertEquals(
        List.of(),
        userEvents,
        "stream 3 belongs to another request on the new connection, it must not be reset");

    previousChannel.finishAndReleaseAll();
    newChannel.finishAndReleaseAll();
  }
}
