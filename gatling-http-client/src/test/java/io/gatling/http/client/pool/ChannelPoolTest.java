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

package io.gatling.http.client.pool;

import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.impl.DefaultHttpClient;
import io.gatling.http.client.uri.Uri;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import org.junit.jupiter.api.Test;

class ChannelPoolTest {

  private static final ChannelPoolKey KEY =
      new ChannelPoolKey(1, RemoteKey.newKey(Uri.create("https://gatling.io/"), null));

  private static EmbeddedChannel http1Channel() {
    EmbeddedChannel channel = new EmbeddedChannel();
    channel
        .pipeline()
        .addLast(DefaultHttpClient.APP_HTTP_HANDLER, new ChannelInboundHandlerAdapter());
    ChannelPool.registerPoolKey(channel, KEY);
    return channel;
  }

  private static EmbeddedChannel http2Channel() {
    EmbeddedChannel channel = new EmbeddedChannel();
    ChannelPool.registerPoolKey(channel, KEY);
    ChannelPool.registerHttp2Connection(channel, new DefaultHttp2Connection(false));
    return channel;
  }

  @Test
  void shouldSkipClosedHttp1ChannelsAndReturnNextActiveOne() {
    ChannelPool pool = new ChannelPool();
    EmbeddedChannel closed = http1Channel();
    EmbeddedChannel active = http1Channel();
    pool.offer(closed);
    pool.offer(active);
    closed.close();

    assertSame(active, pool.poll(KEY));
    assertNull(pool.poll(KEY), "both channels must have been removed from the pool");

    active.finishAndReleaseAll();
  }

  @Test
  void shouldSkipClosedHttp2ChannelsAndReturnNextActiveOne() {
    ChannelPool pool = new ChannelPool();
    EmbeddedChannel closed = http2Channel();
    EmbeddedChannel active = http2Channel();
    pool.offer(closed);
    pool.offer(active);
    closed.close();

    assertSame(active, pool.poll(KEY));
    assertSame(active, pool.poll(KEY), "HTTP/2 channels must remain in the pool");

    active.finishAndReleaseAll();
  }
}
