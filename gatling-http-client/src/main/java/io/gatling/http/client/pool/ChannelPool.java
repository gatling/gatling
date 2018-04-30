/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import io.gatling.http.client.impl.DefaultHttpClient;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import static io.gatling.http.client.ahc.util.Assertions.assertNotNull;

public class ChannelPool {

  private static final AttributeKey<ChannelPoolKey> CHANNEL_POOL_KEY_ATTRIBUTE_KEY = AttributeKey.valueOf("poolKey");
  private static final AttributeKey<Long> CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY = AttributeKey.valueOf("poolTimestamp");
  private static final AttributeKey<Integer> CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY = AttributeKey.valueOf("poolStreamCount");

  private final HashMap<ChannelPoolKey, ArrayDeque<Channel>> channels = new HashMap<>();

  private ArrayDeque<Channel> keyChannels(ChannelPoolKey key) {
    return channels.computeIfAbsent(key, k -> new ArrayDeque<>());
  }

  private boolean isHttp1(Channel channel) {
    return channel.pipeline().get(DefaultHttpClient.APP_HTTP_HANDLER) != null;
  }

  public Channel poll(ChannelPoolKey key) {
    ArrayDeque<Channel> channels = keyChannels(key);

    while(true) {
      Channel channel = channels.peekLast();

      if (channel == null) {
        return null;
      }

      if (!channel.isActive()) {
        channels.removeLast();
      } else if (isHttp1(channel)) {
        channels.removeLast();
        return channel;
      } else {
        Attribute<Integer> streamCountAttr = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY);
        streamCountAttr.set(streamCountAttr.get() + 1);
        return channel;
      }
    }
  }

  public void register(Channel channel, ChannelPoolKey key) {
    channel.attr(CHANNEL_POOL_KEY_ATTRIBUTE_KEY).set(key);
  }

  private void touch(Channel channel) {
    channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).set(System.currentTimeMillis());
  }

  public void offer(Channel channel) {
    ChannelPoolKey key = channel.attr(CHANNEL_POOL_KEY_ATTRIBUTE_KEY).get();
    assertNotNull(key, "Channel doesn't have a key");

    if (isHttp1(channel)) {
      keyChannels(key).offerFirst(channel);
      touch(channel);
    } else {
      Attribute<Integer> streamCountAttr = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY);
      Integer currentStreamCount = streamCountAttr.get();
      if (currentStreamCount == null) {
        keyChannels(key).offerFirst(channel);
        streamCountAttr.set(1);
      } else {
        streamCountAttr.set(currentStreamCount - 1);
        if (currentStreamCount == 1) {
          // so new value is 0
          touch(channel);
        }
      }
    }
  }

  public void closeIdleChannels(long idleTimeout) {
    long now = System.currentTimeMillis();
    for (Map.Entry<ChannelPoolKey, ArrayDeque<Channel>> entry : channels.entrySet()) {
      ArrayDeque<Channel> deque = entry.getValue();
      for (Channel channel : deque) {
        if (now - channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).get() > idleTimeout) {
          Integer currentStreamCount = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY).get();
          if (currentStreamCount == null || currentStreamCount == 0) {
            // HTTP/1.1 or unused
            channel.close();
            deque.remove(channel);
          }
        }
      }
    }
  }

  public void flushClientIdChannelPoolPartitions(long clientId) {
    for (Map.Entry<ChannelPoolKey, ArrayDeque<Channel>> entry : channels.entrySet()) {
      ChannelPoolKey key = entry.getKey();
      if (key.clientId == clientId) {
        for (Channel channel: entry.getValue()) {
          channel.close();
        }
        channels.remove(key);
      }
    }
  }
}
