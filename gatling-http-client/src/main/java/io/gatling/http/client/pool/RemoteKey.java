/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.uri.Uri;
import java.util.Objects;

public final class RemoteKey {

  public static RemoteKey newKey(Uri uri, ProxyServer proxyServer) {
    String targetHostBaseUrl = uri.getBaseUrl();
    if (proxyServer == null) {
      return new RemoteKey(targetHostBaseUrl, null, 0);
    } else {
      return new RemoteKey(targetHostBaseUrl, proxyServer.getHost(), proxyServer.getPort());
    }
  }

  private final String targetHostBaseUrl;
  private final String proxyHost;
  private final int proxyPort;

  private RemoteKey(String targetHostBaseUrl, String proxyHost, int proxyPort) {
    this.targetHostBaseUrl = targetHostBaseUrl;
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
    return Objects.equals(proxyHost, that.proxyHost);
  }

  @Override
  public int hashCode() {
    int result = targetHostBaseUrl.hashCode();
    result = 31 * result + (proxyHost != null ? proxyHost.hashCode() : 0);
    result = 31 * result + proxyPort;
    return result;
  }

  @Override
  public String toString() {
    return "RemoteKey{"
        + "targetHostBaseUrl='"
        + targetHostBaseUrl
        + '\''
        + ", proxyHost='"
        + proxyHost
        + '\''
        + ", proxyPort="
        + proxyPort
        + '}';
  }
}
