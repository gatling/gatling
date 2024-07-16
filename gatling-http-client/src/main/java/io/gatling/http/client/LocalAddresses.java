/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LocalAddresses {

  private final InetSocketAddress linkLocalIpV4;
  private final InetSocketAddress siteLocalIpV4;
  private final InetSocketAddress publicIpV4;
  private final InetSocketAddress linkLocalIpV6;
  private final InetSocketAddress siteLocalIpV6;
  private final InetSocketAddress publicIpV6;

  public LocalAddresses(
      InetSocketAddress linkLocalIpV4,
      InetSocketAddress siteLocalIpV4,
      InetSocketAddress publicIpV4,
      InetSocketAddress linkLocalIpV6,
      InetSocketAddress siteLocalIpV6,
      InetSocketAddress publicIpV6) {
    this.linkLocalIpV4 = linkLocalIpV4;
    this.siteLocalIpV4 = siteLocalIpV4;
    this.publicIpV4 = publicIpV4;
    this.linkLocalIpV6 = linkLocalIpV6;
    this.siteLocalIpV6 = siteLocalIpV6;
    this.publicIpV6 = publicIpV6;
  }

  public InetSocketAddress getLocalAddressForRemote(InetAddress remote) {
    if (remote.isLinkLocalAddress()) {
      return (remote instanceof Inet4Address) ? linkLocalIpV4 : linkLocalIpV6;
    } else if (remote.isSiteLocalAddress()) {
      return (remote instanceof Inet4Address) ? siteLocalIpV4 : siteLocalIpV6;
    } else {
      return (remote instanceof Inet4Address) ? publicIpV4 : publicIpV6;
    }
  }

  @Override
  public String toString() {
    return "LocalAddresses{"
        + "linkLocalIpV4="
        + linkLocalIpV4
        + ", siteLocalIpV4="
        + siteLocalIpV4
        + ", publicIpV4="
        + publicIpV4
        + ", linkLocalIpV6="
        + linkLocalIpV6
        + ", siteLocalIpV6="
        + siteLocalIpV6
        + ", publicIpV6="
        + publicIpV6
        + '}';
  }
}
