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

import io.gatling.http.client.ssl.Tls;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

class CoalescingChannelPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(CoalescingChannelPool.class);

  private final Map<IpAndPort, Map.Entry<Set<String>, Queue<Channel>>> pool = new HashMap<>();

  Channel getCoalescedChannel(String domain, List<InetSocketAddress> addresses) {
    for (InetSocketAddress address : addresses) {
      IpAndPort ipAndPort = new IpAndPort(address.getAddress().getAddress(), address.getPort());
      Map.Entry<Set<String>, Queue<Channel>> entry = pool.get(ipAndPort);
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

  void deleteEntry(IpAndPort ipAndPort) {
    pool.remove(ipAndPort);
  }

  void addEntry(IpAndPort ipAndPort, Set<String> subjectAlternativeNames, Channel channel) {
    LOGGER.debug("Adding entry {} with subjectAlternativeNames {} to coalescing pool", ipAndPort, subjectAlternativeNames);
    Map.Entry<Set<String>, Queue<Channel>> entry = pool.get(ipAndPort);
    if (entry == null) {
      Queue<Channel> channels = new ArrayDeque<>(ChannelPool.INITIAL_CHANNEL_QUEUE_SIZE);
      channels.add(channel);
      pool.put(ipAndPort, new AbstractMap.SimpleEntry<>(subjectAlternativeNames, channels));
    } else {
      entry.getValue().offer(channel);
    }
  }
}
