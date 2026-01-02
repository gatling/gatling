/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.proxy;

import io.gatling.http.client.realm.BasicRealm;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import java.net.UnknownHostException;

public final class HttpProxyServer extends ProxyServer {

  private final boolean secured;
  private final HttpHeaders connectHeaders;

  public HttpProxyServer(
      String host, int port, BasicRealm realm, boolean secured, HttpHeaders connectHeaders)
      throws UnknownHostException {
    super(host, port, realm);
    this.secured = secured;
    this.connectHeaders = connectHeaders;
  }

  public boolean isSecured() {
    return secured;
  }

  public BasicRealm getRealm() {
    return realm;
  }

  @Override
  public ProxyHandler newProxyHandler() {
    return realm != null
        ? new HttpProxyHandler(
            getAddress(), realm.getUsername(), realm.getPassword(), connectHeaders, true)
        : new HttpProxyHandler(getAddress(), connectHeaders, true);
  }

  @Override
  public String toString() {
    return "HttpProxyServer{"
        + "secured="
        + secured
        + ", host='"
        + host
        + '\''
        + ", port="
        + port
        + ", realm="
        + realm
        + '}';
  }
}
