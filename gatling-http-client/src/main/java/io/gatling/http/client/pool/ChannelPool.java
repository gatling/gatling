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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

import static io.gatling.http.client.ahc.util.Assertions.assertNotNull;

public class ChannelPool {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelPool.class);

  private static final AttributeKey<ChannelPoolKey> CHANNEL_POOL_KEY_ATTRIBUTE_KEY = AttributeKey.valueOf("poolKey");
  private static final AttributeKey<Long> CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY = AttributeKey.valueOf("poolTimestamp");
  private static final AttributeKey<Integer> CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY = AttributeKey.valueOf("poolStreamCount");
  static final int INITIAL_CHANNEL_QUEUE_SIZE = 2;

  private final Map<ChannelPoolKey, Queue<Channel>> channels = new HashMap<>();
  private final CoalescingChannelPool coalescingChannelPool = new CoalescingChannelPool();
  private final Map<ChannelPoolKey, IpAndPort> keyToIp = new HashMap<>();

  private Queue<Channel> keyChannels(ChannelPoolKey key) {
    return channels.computeIfAbsent(key, k -> new ArrayDeque<>(INITIAL_CHANNEL_QUEUE_SIZE));
  }

  private boolean isHttp1(Channel channel) {
    return channel.pipeline().get(DefaultHttpClient.APP_HTTP_HANDLER) != null;
  }

  public boolean isHttp2(Channel channel) {
    return !isHttp1(channel);
  }

  private void incrementStreamCount(Channel channel) {
    Attribute<Integer> streamCountAttr = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY);
    streamCountAttr.set(streamCountAttr.get() + 1);
  }

  public Channel poll(ChannelPoolKey key) {
    Queue<Channel> channels = keyChannels(key);

    while (true) {
      Channel channel = channels.peek();

      if (channel == null) {
        return null;
      }

      if (!channel.isActive()) {
        channels.remove();
      } else if (isHttp1(channel)) {
        channels.remove();
        return channel;
      } else {
        incrementStreamCount(channel);
        return channel;
      }
    }
  }

  public Channel pollCoalescedChannel(String domain, List<InetSocketAddress> addresses) {
    Channel channel;
    if ((channel = coalescingChannelPool.getCoalescedChannel(domain, addresses)) != null) {
      LOGGER.debug("Retrieving channel from coalescing pool for domain " + domain);
      incrementStreamCount(channel);
    }
    return channel;
  }

  public void addCoalescedChannel(Set<String> subjectAlternativeNames, InetSocketAddress address, Channel channel, ChannelPoolKey key) {
    IpAndPort ipAndPort = new IpAndPort(address.getAddress().getAddress(), address.getPort());
    coalescingChannelPool.addEntry(ipAndPort, subjectAlternativeNames, channel);
    keyToIp.put(key, ipAndPort);
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
      keyChannels(key).offer(channel);
      touch(channel);
    } else {
      Attribute<Integer> streamCountAttr = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY);
      Integer currentStreamCount = streamCountAttr.get();
      if (currentStreamCount == null) {
        keyChannels(key).offer(channel);
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

  private void cleanCoalescingPool(ChannelPoolKey key) {
    IpAndPort ipAndPort = keyToIp.get(key);
    if (ipAndPort != null) {
      coalescingChannelPool.deleteEntry(ipAndPort);
    }
  }

  public void closeIdleChannels(long idleTimeout) {
    long now = System.currentTimeMillis();
    for (Map.Entry<ChannelPoolKey, Queue<Channel>> entry : channels.entrySet()) {
      Queue<Channel> deque = entry.getValue();
      for (Channel channel : deque) {
        if (now - channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).get() > idleTimeout) {
          Integer currentStreamCount = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY).get();
          if (currentStreamCount == null || currentStreamCount == 0) {
            // HTTP/1.1 or unused
            channel.close();
            deque.remove(channel);
            if (isHttp2(channel)) {
              cleanCoalescingPool(entry.getKey());
            }
          }
        }
      }
    }
  }

  public void flushClientIdChannelPoolPartitions(long clientId) {
    for (Map.Entry<ChannelPoolKey, Queue<Channel>> entry : channels.entrySet()) {
      ChannelPoolKey key = entry.getKey();
      if (key.clientId == clientId) {
        for (Channel channel: entry.getValue()) {
          channel.close();
        }
        channels.remove(key);
      }
    }
  }

  @Override
  public String toString() {
    return "ChannelPool{" +
      "channels=" + channels +
      ", coalescingChannelPool=" + coalescingChannelPool +
      ", keyToIp=" + keyToIp +
      '}';
  }
}
