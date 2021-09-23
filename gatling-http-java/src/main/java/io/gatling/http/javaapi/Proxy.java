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

package io.gatling.http.javaapi;

public final class Proxy {
  private final io.gatling.http.protocol.ProxyBuilder wrapped;

  public Proxy(io.gatling.http.protocol.ProxyBuilder wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.http.protocol.Proxy asScala() {
    return wrapped.proxy();
  }

  public Proxy http() {
    return new Proxy(wrapped.http());
  }

  public Proxy socks4() {
    return new Proxy(wrapped.socks4());
  }

  public Proxy socks5() {
    return new Proxy(wrapped.socks5());
  }

  public Proxy httpsPort(int port) {
    return new Proxy(wrapped.httpsPort(port));
  }

  public Proxy credentials(String username, String password) {
    return new Proxy(wrapped.credentials(username, password));
  }
}
