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

import io.gatling.http.client.ahc.uri.Uri;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyServer;

public class ChannelPoolKey {

  public static ChannelPoolKey newKey(long clientId, Uri uri, String virtualHost, ProxyServer proxyServer) {
    String targetHostBaseUrl = uri.getBaseUrl();
    if (proxyServer == null) {
      return new ChannelPoolKey(
        clientId,
        targetHostBaseUrl,
        virtualHost,
        null,
        0);
    } else {
      return new ChannelPoolKey(
        clientId,
        targetHostBaseUrl,
        virtualHost,
        proxyServer.getHost(),
        uri.isSecured() && proxyServer instanceof HttpProxyServer ?
          ((HttpProxyServer) proxyServer).getSecuredPort() :
          proxyServer.getPort());
    }
  }

  public final long clientId;
  private final String targetHostBaseUrl;
  private final String virtualHost;
  private final String proxyHost;
  private final int proxyPort;

  private ChannelPoolKey(long clientId, String targetHostBaseUrl, String virtualHost, String proxyHost, int proxyPort) {
    this.clientId = clientId;
    this.targetHostBaseUrl = targetHostBaseUrl;
    this.virtualHost = virtualHost;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChannelPoolKey that = (ChannelPoolKey) o;

    if (clientId != that.clientId) return false;
    if (!targetHostBaseUrl.equals(that.targetHostBaseUrl)) return false;
    if (virtualHost != null && !virtualHost.equals(that.virtualHost)) return false;
    if (proxyPort != that.proxyPort) return false;
    return proxyHost != null ? proxyHost.equals(that.proxyHost) : that.proxyHost == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (clientId ^ (clientId >>> 32));
    result = 31 * result + targetHostBaseUrl.hashCode();
    result = 31 * result + (virtualHost != null ? virtualHost.hashCode() : 0);
    result = 31 * result + (proxyHost != null ? proxyHost.hashCode() : 0);
    result = 31 * result + proxyPort;
    return result;
  }
}
