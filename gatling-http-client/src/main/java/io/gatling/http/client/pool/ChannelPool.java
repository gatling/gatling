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

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static io.gatling.http.client.ahc.util.Assertions.assertNotNull;

public class ChannelPool {

  private static final AttributeKey<ChannelPoolKey> CHANNEL_POOL_KEY_ATTRIBUTE_KEY = AttributeKey.valueOf("poolKey");
  private static final AttributeKey<Long> CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY = AttributeKey.valueOf("poolTimestamp");

  private final HashMap<ChannelPoolKey, ArrayDeque<Channel>> channels = new HashMap<>();

  private ArrayDeque<Channel> keyChannels(ChannelPoolKey key) {
    return channels.computeIfAbsent(key, k -> new ArrayDeque<>());
  }

  public Channel acquire(ChannelPoolKey key) {
    return keyChannels(key).pollLast();
  }

  public void register(Channel channel, ChannelPoolKey key) {
    channel.attr(CHANNEL_POOL_KEY_ATTRIBUTE_KEY).set(key);
  }

  public void release(Channel channel) {
    ChannelPoolKey key = channel.attr(CHANNEL_POOL_KEY_ATTRIBUTE_KEY).get();
    assertNotNull(key, "Channel doesn't have a key");
    channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).set(System.currentTimeMillis());
    keyChannels(key).offerFirst(channel);
  }

  public void closeIdleChannels(long idleTimeout) {
    long now = System.currentTimeMillis();
    for (Map.Entry<ChannelPoolKey, ArrayDeque<Channel>> entry : channels.entrySet()) {
      ArrayDeque<Channel> deque = entry.getValue();
      for (Channel channel : deque) {
        if (now - channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).get() > idleTimeout) {
          channel.close();
          deque.remove(channel);
        }
      }
    }
  }

  public void flushChannelPoolPartitions(Predicate<ChannelPoolKey> predicate) {
    for (Map.Entry<ChannelPoolKey, ArrayDeque<Channel>> entry : channels.entrySet()) {
      ChannelPoolKey key = entry.getKey();
      if (predicate.test(key)) {
        for (Channel channel: entry.getValue()) {
          channel.close();
        }
        channels.remove(key);
      }
    }
  }
}
