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

package io.gatling.javaapi.http;

import static io.gatling.javaapi.core.internal.Converters.toScalaMap;
import static io.gatling.javaapi.core.internal.Expressions.javaFunctionToExpression;
import static io.gatling.javaapi.core.internal.Expressions.toStringExpression;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.Session;
import java.util.Map;
import java.util.function.Function;

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
   * Define this proxy is an HTTPS one
   *
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy https() {
    return new Proxy(wrapped.http().https());
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
   * Define some username-password credentials for this proxy
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy credentials(@NonNull String username, @NonNull String password) {
    return new Proxy(
        wrapped.credentials(toStringExpression(username), toStringExpression(password)));
  }

  /**
   * Define some username-password credentials for this proxy
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a function
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy credentials(@NonNull String username, @NonNull Function<Session, String> password) {
    return new Proxy(
        wrapped.credentials(toStringExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Define some username-password credentials for this proxy
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy credentials(@NonNull Function<Session, String> username, @NonNull String password) {
    return new Proxy(
        wrapped.credentials(javaFunctionToExpression(username), toStringExpression(password)));
  }

  /**
   * Define some Basic Auth credentials for this proxy
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy credentials(
      @NonNull Function<Session, String> username, @NonNull Function<Session, String> password) {
    return new Proxy(
        wrapped.credentials(
            javaFunctionToExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set a header for the CONNECT request (HTTP(S) proxies only)
   *
   * @param name the static header name
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy connectHeader(@NonNull CharSequence name, @NonNull String value) {
    return new Proxy(wrapped.connectHeader(name, toStringExpression(value)));
  }

  /**
   * Set a header for the CONNECT request (HTTP(S) proxies only)
   *
   * @param name the static header name
   * @param value the header value, expressed as a function
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy connectHeader(@NonNull CharSequence name, @NonNull Function<Session, String> value) {
    return new Proxy(wrapped.connectHeader(name, javaFunctionToExpression(value)));
  }

  /**
   * Set a header for the CONNECT request (HTTP(S) proxies only)
   *
   * @param headers the headers, names are static but values are expressed as a Gatling Expression
   *     Language String
   * @return a new Proxy instance
   */
  @NonNull
  public Proxy connectHeaders(Map<? extends CharSequence, String> headers) {
    return new Proxy(wrapped.connectHeaders(toScalaMap(headers)));
  }
}
