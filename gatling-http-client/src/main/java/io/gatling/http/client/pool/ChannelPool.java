/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import static io.gatling.http.client.util.Assertions.assertNotNull;

import io.gatling.http.client.impl.DefaultHttpClient;
import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelPool {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelPool.class);

  private static final AttributeKey<ChannelPoolKey> CHANNEL_POOL_KEY =
      AttributeKey.valueOf("poolKey");
  private static final AttributeKey<Long> CHANNEL_TOUCH_TIMESTAMP =
      AttributeKey.valueOf("idleTimestamp");
  private static final AttributeKey<Http2Connection> CHANNEL_HTTP2_CONNEXION =
      AttributeKey.valueOf("http2Connection");
  private static final AttributeKey<Boolean> HTTP2_POOLED = AttributeKey.valueOf("http2Pooled");
  private static final AttributeKey<Boolean> CHANNEL_GOAWAY = AttributeKey.valueOf("goAway");

  static final int INITIAL_CLIENT_MAP_SIZE = 1000;
  static final int INITIAL_KEY_PER_CLIENT_MAP_SIZE = 2;
  static final int INITIAL_CHANNEL_QUEUE_SIZE = 2;

  private final Map<Long, Map<RemoteKey, Queue<Channel>>> channels =
      new HashMap<>(INITIAL_CLIENT_MAP_SIZE);
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

  ////////////////////////////// CHANNEL_POOL_KEY
  public static void registerPoolKey(Channel channel, ChannelPoolKey key) {
    channel.attr(CHANNEL_POOL_KEY).set(key);
  }

  ////////////////////////////// CHANNEL_IDLE_TIMESTAMP
  private static void touch(Channel channel) {
    channel.attr(CHANNEL_TOUCH_TIMESTAMP).set(System.nanoTime());
  }

  private static boolean isLastTouchTooOld(Channel channel, long now, long idleTimeoutNanos) {
    return now - channel.attr(CHANNEL_TOUCH_TIMESTAMP).get() > idleTimeoutNanos;
  }

  ////////////////////////////// CHANNEL_HTTP2_CONNEXION
  public static void registerHttp2Connection(Channel channel, Http2Connection http2Connection) {
    channel.attr(CHANNEL_HTTP2_CONNEXION).set(http2Connection);
  }

  private static Http2Connection getHttp2Connection(Channel channel) {
    return channel.attr(CHANNEL_HTTP2_CONNEXION).get();
  }

  private static boolean canOpenStream(Channel channel) {
    return getHttp2Connection(channel).local().canOpenStream();
  }

  ////////////////////////////// CHANNEL_GOAWAY
  public static void markAsGoAway(Channel channel) {
    channel.attr(CHANNEL_GOAWAY).set(Boolean.TRUE);
  }

  private static boolean isNotGoAway(Channel channel) {
    return !channel.hasAttr(CHANNEL_GOAWAY);
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
        LOGGER.debug("Retrieved HTTP/1 channel from pool for key {}", key);
        return channel;
      } else if (isNotGoAway(channel) && canOpenStream(channel)) {
        LOGGER.debug("Retrieved HTTP/2 channel from pool for key {}", key);
        touch(channel);
        return channel;
      }
    }

    LOGGER.debug("No channel in the pool for key {}", key);
    return null;
  }

  public Channel pollCoalescedChannel(
      long clientId, String domain, List<InetSocketAddress> addresses) {
    Channel channel =
        coalescingChannelPool.getCoalescedChannel(
            clientId, domain, addresses, ChannelPool::canOpenStream);
    if (channel != null) {
      LOGGER.debug("Retrieved channel from coalescing pool for domain {}", domain);
    }
    return channel;
  }

  public void offerCoalescedChannel(
      Set<String> subjectAlternativeNames,
      InetSocketAddress address,
      Channel channel,
      ChannelPoolKey key) {
    IpAndPort ipAndPort = new IpAndPort(address.getAddress().getAddress(), address.getPort());
    LOGGER.debug(
        "Offering channel entry {} with subjectAlternativeNames {} to coalescing pool",
        ipAndPort,
        subjectAlternativeNames);
    coalescingChannelPool.addEntry(key.clientId, ipAndPort, subjectAlternativeNames, channel);
  }

  public void offer(Channel channel) {
    ChannelPoolKey key = channel.attr(CHANNEL_POOL_KEY).get();
    assertNotNull(key, "Channel doesn't have a key");
    touch(channel);

    LOGGER.debug("Offering channel entry {} to pool", key);

    if (isHttp1(channel)) {
      remoteChannels(key).offer(channel);
    } else if (!channel.hasAttr(HTTP2_POOLED)) {
      channel.attr(HTTP2_POOLED).set(Boolean.TRUE);
      // we never remove from the queue, so we only offer the first time
      remoteChannels(key).offer(channel);
    }
  }

  public void closeIdleChannels(long idleTimeoutNanos) {
    long now = System.nanoTime();
    for (Map.Entry<Long, Map<RemoteKey, Queue<Channel>>> clientEntry : channels.entrySet()) {
      for (Map.Entry<RemoteKey, Queue<Channel>> entry : clientEntry.getValue().entrySet()) {
        Queue<Channel> deque = entry.getValue();
        for (Channel channel : deque) {
          boolean http2 = isHttp2(channel);
          if (isLastTouchTooOld(channel, now, idleTimeoutNanos)
              && (!http2 || getHttp2Connection(channel).numActiveStreams() == 0)) {
            channel.close();
            deque.remove(channel);
            if (http2) {
              coalescingChannelPool.deleteIdleEntry(clientEntry.getKey(), channel);
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
    return "ChannelPool{"
        + "channels="
        + channels
        + ", coalescingChannelPool="
        + coalescingChannelPool
        + '}';
  }
}
