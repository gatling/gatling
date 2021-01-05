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

import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyServer;

public class RemoteKey {

  public static RemoteKey newKey(Uri uri, String virtualHost, ProxyServer proxyServer) {
    String targetHostBaseUrl = uri.getBaseUrl();
    if (proxyServer == null) {
      return new RemoteKey(
        targetHostBaseUrl,
        virtualHost,
        null,
        0);
    } else {
      return new RemoteKey(
        targetHostBaseUrl,
        virtualHost,
        proxyServer.getHost(),
        uri.isSecured() && proxyServer instanceof HttpProxyServer ?
          ((HttpProxyServer) proxyServer).getSecuredPort() :
          proxyServer.getPort());
    }
  }

  private final String targetHostBaseUrl;
  private final String virtualHost;
  private final String proxyHost;
  private final int proxyPort;

  private RemoteKey(String targetHostBaseUrl, String virtualHost, String proxyHost, int proxyPort) {
    this.targetHostBaseUrl = targetHostBaseUrl;
    this.virtualHost = virtualHost;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RemoteKey that = (RemoteKey) o;

    if (proxyPort != that.proxyPort) return false;
    if (!targetHostBaseUrl.equals(that.targetHostBaseUrl)) return false;
    if (virtualHost != null ? !virtualHost.equals(that.virtualHost) : that.virtualHost != null) return false;
    return proxyHost != null ? proxyHost.equals(that.proxyHost) : that.proxyHost == null;
  }

  @Override
  public int hashCode() {
    int result = targetHostBaseUrl.hashCode();
    result = 31 * result + (virtualHost != null ? virtualHost.hashCode() : 0);
    result = 31 * result + (proxyHost != null ? proxyHost.hashCode() : 0);
    result = 31 * result + proxyPort;
    return result;
  }

  @Override
  public String toString() {
    return "RemoteKey{" +
      "targetHostBaseUrl='" + targetHostBaseUrl + '\'' +
      ", virtualHost='" + virtualHost + '\'' +
      ", proxyHost='" + proxyHost + '\'' +
      ", proxyPort=" + proxyPort +
      '}';
  }
}
