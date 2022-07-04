/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Unwrap;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * DSL for bootstrapping Proxies.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Proxy implements Unwrap<io.gatling.http.protocol.Proxy> {
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
  @Nonnull
  public Proxy http() {
    return new Proxy(wrapped.http());
  }

  /**
   * Define this proxy is an SOCKS4 once
   *
   * @return a new Proxy instance
   */
  @Nonnull
  public Proxy socks4() {
    return new Proxy(wrapped.socks4());
  }

  /**
   * Define this proxy is an SOCKS5 once
   *
   * @return a new Proxy instance
   */
  @Nonnull
  public Proxy socks5() {
    return new Proxy(wrapped.socks5());
  }

  /**
   * Define this proxy uses a different port for HTTPS
   *
   * @param port the https port
   * @return a new Proxy instance
   */
  @Nonnull
  public Proxy httpsPort(Integer port) {
    return new Proxy(wrapped.httpsPort(toIntExpression(port.toString())));
  }

  /**
   * Define this proxy uses a different port for HTTPS
   *
   * @param port the https port, expressed as a function
   * @return a new Proxy instance
   */
  @Nonnull
  public Proxy httpsPort(Function<Session, Integer> port) {
    return new Proxy(wrapped.httpsPort(javaIntegerFunctionToExpression(port)));
  }

  /**
   * Define some Basic Auth credentials for this proxy
   *
   * @param username the username
   * @param password the password
   * @return a new Proxy instance
   */
  @Nonnull
  public Proxy credentials(@Nonnull String username, @Nonnull String password) {
    return new Proxy(
        wrapped.credentials(toStringExpression(username), toStringExpression(password)));
  }

  /**
   * Define some Basic Auth credentials for this proxy
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new Proxy instance
   */
  @Nonnull
  public Proxy credentials(
      @Nonnull Function<Session, String> username, @Nonnull Function<Session, String> password) {
    return new Proxy(
        wrapped.credentials(
            javaFunctionToExpression(username), javaFunctionToExpression(password)));
  }
}
