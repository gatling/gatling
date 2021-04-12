/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import static io.gatling.http.client.util.Assertions.assertNotNull;

public class ChannelPool {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelPool.class);

  private static final AttributeKey<ChannelPoolKey> CHANNEL_POOL_KEY_ATTRIBUTE_KEY = AttributeKey.valueOf("poolKey");
  private static final AttributeKey<Long> CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY = AttributeKey.valueOf("poolTimestamp");
  private static final AttributeKey<Integer> CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY = AttributeKey.valueOf("poolStreamCount");
  private static final AttributeKey<Long> CHANNEL_MAX_CONCURRENT_STREAMS = AttributeKey.valueOf("maxConcurrentStreams");
  static final int INITIAL_CLIENT_MAP_SIZE = 1000;
  static final int INITIAL_KEY_PER_CLIENT_MAP_SIZE = 2;
  static final int INITIAL_CHANNEL_QUEUE_SIZE = 2;

  private final Map<Long, Map<RemoteKey, Queue<Channel>>> channels = new HashMap<>(INITIAL_CLIENT_MAP_SIZE);
  private final CoalescingChannelPool coalescingChannelPool = new CoalescingChannelPool();

  private Queue<Channel> remoteChannels(ChannelPoolKey key) {
    return channels
      .computeIfAbsent(key.clientId, k -> new HashMap<>(INITIAL_KEY_PER_CLIENT_MAP_SIZE))
      .computeIfAbsent(key.remoteKey, k -> new ArrayDeque<>(INITIAL_CHANNEL_QUEUE_SIZE));
  }

  private static boolean isHttp1(Channel channel) {
    return channel.pipeline().get(DefaultHttpClient.APP_HTTP_HANDLER) != null;
  }

  public static boolean isHttp2(Channel channel) {
    return !isHttp1(channel);
  }

  private boolean tryIncrementingStreamCount(Channel channel) {
    Attribute<Integer> streamCountAttr = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY);
    Long maxConcurrentStreams = channel.attr(CHANNEL_MAX_CONCURRENT_STREAMS).get();
    int currentCount = streamCountAttr.get();
    if (maxConcurrentStreams == null || currentCount < maxConcurrentStreams.longValue()) {
      streamCountAttr.set(streamCountAttr.get() + 1);
      return true;
    } else {
      return false;
    }
  }

  public Channel poll(ChannelPoolKey key) {
    Queue<Channel> channels = remoteChannels(key);

    Iterator<Channel> it = channels.iterator();

    while (it.hasNext()) {
      Channel channel = it.next();

      if (!channel.isActive()) {
        it.remove();
        break;
      } else if (isHttp1(channel)) {
        it.remove();
        return channel;
      } else if (tryIncrementingStreamCount(channel)) {
        return channel;
      }
    }

    return null;
  }

  public Channel pollCoalescedChannel(long clientId, String domain, List<InetSocketAddress> addresses) {
    Channel channel = coalescingChannelPool.getCoalescedChannel(clientId, domain, addresses, this::tryIncrementingStreamCount);
    if (channel != null) {
      LOGGER.debug("Retrieving channel from coalescing pool for domain {}", domain);
    }
    return channel;
  }

  public void offerCoalescedChannel(Set<String> subjectAlternativeNames, InetSocketAddress address, Channel channel, ChannelPoolKey key) {
    IpAndPort ipAndPort = new IpAndPort(address.getAddress().getAddress(), address.getPort());
    coalescingChannelPool.addEntry(key.clientId, ipAndPort, subjectAlternativeNames, channel);
  }

  public void updateMaxConcurrentStreams(Channel channel, long maxConcurrentStreams) {
    channel.attr(CHANNEL_MAX_CONCURRENT_STREAMS).set(maxConcurrentStreams);
  }

  public void register(Channel channel, ChannelPoolKey key) {
    channel.attr(CHANNEL_POOL_KEY_ATTRIBUTE_KEY).set(key);
  }

  private void touch(Channel channel) {
    channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).set(System.nanoTime());
  }

  public void offer(Channel channel) {
    ChannelPoolKey key = channel.attr(CHANNEL_POOL_KEY_ATTRIBUTE_KEY).get();
    assertNotNull(key, "Channel doesn't have a key");

    if (isHttp1(channel)) {
      remoteChannels(key).offer(channel);
      touch(channel);
    } else {
      Attribute<Integer> streamCountAttr = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY);
      Integer currentStreamCount = streamCountAttr.get();
      if (currentStreamCount == null) {
        remoteChannels(key).offer(channel);
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

  public void closeIdleChannels(long idleTimeoutNanos) {
    long now = System.nanoTime();
    for (Map.Entry<Long, Map<RemoteKey, Queue<Channel>>> clientEntry : channels.entrySet()) {
      for (Map.Entry<RemoteKey, Queue<Channel>> entry : clientEntry.getValue().entrySet()) {
        Queue<Channel> deque = entry.getValue();
        for (Channel channel : deque) {
          if (now - channel.attr(CHANNEL_POOL_TIMESTAMP_ATTRIBUTE_KEY).get() > idleTimeoutNanos) {
            Integer currentStreamCount = channel.attr(CHANNEL_POOL_STREAM_COUNT_ATTRIBUTE_KEY).get();
            if (currentStreamCount == null || currentStreamCount == 0) {
              // HTTP/1.1 or unused
              channel.close();
              deque.remove(channel);
              if (isHttp2(channel)) {
                coalescingChannelPool.deleteIdleEntry(clientEntry.getKey(), channel);
              }
            }
          }
        }
      }
    }
  }

  public void flushClientIdChannelPoolPartitions(long clientId) {
    Map<RemoteKey, Queue<Channel>> clientChannel = channels.get(clientId);
    if (clientChannel != null) {
      clientChannel.entrySet().stream().flatMap(e -> e.getValue().stream()).forEach(Channel::close);
      channels.remove(clientId);
      coalescingChannelPool.deleteClientEntries(clientId);
    }
  }

  @Override
  public String toString() {
    return "ChannelPool{" +
      "channels=" + channels +
      ", coalescingChannelPool=" + coalescingChannelPool +
      '}';
  }
}
