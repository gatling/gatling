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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.regex.Pattern;

public class CoalescingChannelPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(CoalescingChannelPool.class);

  private final Map<IpAndPort, AbstractMap.Entry<Set<String>, Queue<Channel>>> pool = new HashMap<>();
  private static final Pattern pattern = Pattern.compile("\\.");


  static boolean isCertificateAuthoritative(String san, String domain) {
    String[] sanSplit = pattern.split(san);
    String[] domainSplit = pattern.split(domain);

    if (sanSplit.length != domainSplit.length)
      return false;

    String firstDomainChunk = domainSplit[0];

    String firstLabel = sanSplit[0];
    int firstLabelLength = firstLabel.length();

    int wildCardPosition = firstLabel.indexOf('*');

    boolean isFirstLabelValid;

    if (wildCardPosition == -1) {
      isFirstLabelValid = false;
    } else if (wildCardPosition == 0 && firstLabelLength == 1) {
      isFirstLabelValid = true;
    } else if (wildCardPosition == 0) {
      String after = firstLabel.substring(wildCardPosition + 1);
      isFirstLabelValid = firstDomainChunk.endsWith(after);
    } else if (wildCardPosition == firstLabelLength - 1) {
      String before = firstLabel.substring(0, wildCardPosition);
      isFirstLabelValid = firstDomainChunk.startsWith(before);
    } else {
      String after = firstLabel.substring(wildCardPosition + 1);
      String before = firstLabel.substring(0, wildCardPosition);
      isFirstLabelValid = firstDomainChunk.endsWith(after) && firstDomainChunk.startsWith(before);
    }

    boolean isCertificateAuthoritative = true;

    for (int i = isFirstLabelValid ? 1 : 0; i < domainSplit.length && isCertificateAuthoritative; i++) {
      isCertificateAuthoritative = sanSplit[i].equals(domainSplit[i]);
    }

    return isCertificateAuthoritative;
  }

  public Channel getCoalescedChannel(String domain, List<InetSocketAddress> addresses) {
    for (InetSocketAddress address : addresses) {
      IpAndPort ipAndPort = new IpAndPort(address.getAddress().getAddress(), address.getPort());
      AbstractMap.Entry<Set<String>, Queue<Channel>> entry = pool.get(ipAndPort);
      if (entry != null) {
        for (String subjectAlternativeName : entry.getKey()) {
          if (isCertificateAuthoritative(subjectAlternativeName, domain)) {
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

  public void deleteEntry(IpAndPort ipAndPort) {
    pool.remove(ipAndPort);
  }

  public void addEntry(IpAndPort ipAndPort, Set<String> subjectAlternativeNames, Channel channel) {
    LOGGER.debug("Adding entry " + ipAndPort + " with subjectAlterativeNames " + subjectAlternativeNames + " to coalescing pool");
    AbstractMap.Entry<Set<String>, Queue<Channel>> entry = pool.get(ipAndPort);
    if (entry == null) {
      Queue<Channel> channels = new ArrayDeque<>(ChannelPool.INITIAL_CHANNEL_QUEUE_SIZE);
      channels.add(channel);
      pool.put(ipAndPort, new AbstractMap.SimpleEntry<>(subjectAlternativeNames, channels));
    } else {
      entry.getValue().offer(channel);
    }
  }
}
