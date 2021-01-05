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

import io.gatling.http.client.ssl.Tls;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

class CoalescingChannelPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(CoalescingChannelPool.class);

  // FIXME Queue or Set?
  private final Map<Long, Map<IpAndPort, Map.Entry<Set<String>, Queue<Channel>>>> channels = new HashMap<>(ChannelPool.INITIAL_CLIENT_MAP_SIZE);

  private final Map<Long, Map<Channel, Queue<Channel>>> cleanUpMap = new HashMap<>(ChannelPool.INITIAL_CLIENT_MAP_SIZE);

  private Map<IpAndPort, Map.Entry<Set<String>, Queue<Channel>>> clientChannels(long clientId) {
    return channels
      .computeIfAbsent(clientId, k -> new HashMap<>(ChannelPool.INITIAL_KEY_PER_CLIENT_MAP_SIZE));
  }

  private Map<Channel, Queue<Channel>> clientCleanUpMap(long clientId) {
    return cleanUpMap
      .computeIfAbsent(clientId, k -> new HashMap<>(ChannelPool.INITIAL_KEY_PER_CLIENT_MAP_SIZE));
  }


  void addEntry(long clientId, IpAndPort ipAndPort, Set<String> subjectAlternativeNames, Channel channel) {
    LOGGER.debug("Adding entry {} with subjectAlternativeNames {} to coalescing pool", ipAndPort, subjectAlternativeNames);
    Map<IpAndPort, Map.Entry<Set<String>, Queue<Channel>>> clientChannels = clientChannels(clientId);
    Map.Entry<Set<String>, Queue<Channel>> entry =  clientChannels.get(ipAndPort);
    Queue<Channel> channels;
    if (entry == null) {
      channels = new ArrayDeque<>(ChannelPool.INITIAL_CHANNEL_QUEUE_SIZE);
      channels.add(channel);
      clientChannels.put(ipAndPort, new AbstractMap.SimpleEntry<>(subjectAlternativeNames, channels));
    } else {
      channels = entry.getValue();
      entry.getValue().offer(channel);
    }

    clientCleanUpMap(clientId).put(channel, channels);
  }

  Channel getCoalescedChannel(long clientId, String domain, List<InetSocketAddress> addresses) {
    for (InetSocketAddress address : addresses) {
      IpAndPort ipAndPort = new IpAndPort(address.getAddress().getAddress(), address.getPort());
      Map.Entry<Set<String>, Queue<Channel>> entry = clientChannels(clientId).get(ipAndPort);
      if (entry != null) {
        for (String subjectAlternativeName : entry.getKey()) {
          if (Tls.isCertificateAuthoritative(subjectAlternativeName, domain)) {
            for (Channel channel : entry.getValue()) {
              if (channel.isActive()) {
                return channel;
              }
            }
          }
        }
      }
    }
    return null;
  }

  void deleteIdleEntry(long clientId, Channel channel) {
    Map<Channel, Queue<Channel>> clientCleanUpMap = cleanUpMap.get(clientId);
    if (clientCleanUpMap != null) {
      Queue<Channel> channels = clientCleanUpMap.get(channel);
      if (channels != null) {
        channels.remove(channel);
        clientCleanUpMap.remove(channel);
      }
    }
  }

  void deleteClientEntries(long clientId) {
    channels.remove(clientId);
    cleanUpMap.remove(clientId);
  }
}
