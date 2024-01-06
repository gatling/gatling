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

package io.gatling.javaapi.http;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * DSL for bootstrapping Proxies.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Proxy {
  private final io.gatling.http.protocol.ProxyBuilder wrapped;

  Proxy(io.gatling.http.protocol.ProxyBuilder wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.http.protocol.Proxy asScala() {
    return wrapped.proxy();
  }

  /**
   * Define this proxy is an HTTP one (default)
   *
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy http() {
    return new Proxy(wrapped.http());
  }

  /**
   * Define this proxy is an SOCKS4 once
   *
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy socks4() {
    return new Proxy(wrapped.socks4());
  }

  /**
   * Define this proxy is an SOCKS5 once
   *
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy socks5() {
    return new Proxy(wrapped.socks5());
  }

  /**
   * Define this proxy uses a different port for HTTPS
   *
   * @param port the https port
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy httpsPort(int port) {
    return new Proxy(wrapped.httpsPort(port));
  }

  /**
   * Define some Basic Auth credentials for this proxy
   *
   * @param username the username
   * @param password the password
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy credentials(@NonNull String username, @NonNull String password) {
    return new Proxy(wrapped.credentials(username, password));
  }
}
