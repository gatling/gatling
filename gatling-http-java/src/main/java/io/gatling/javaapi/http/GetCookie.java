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

import static io.gatling.javaapi.core.internal.Expressions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.http.action.cookie.GetCookieDsl;
import io.gatling.javaapi.core.Session;
import java.util.function.Function;

/**
 * DSL for fetching the value of a <a
 * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies">cookie</a> from the virtual
 * user's CookieJar into its {@link Session}.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class GetCookie {

  private final GetCookieDsl wrapped;

  GetCookie(GetCookieDsl wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Define the domain of the cookie. If undefined, will try to use the domain of {@link
   * HttpProtocolBuilder#baseUrl(String)}
   *
   * @param domain the cookie domain, expressed as a Gatling Expression Language String
   * @return a new GetCookie
   */
  @NonNull
  public GetCookie withDomain(@NonNull String domain) {
    return new GetCookie(wrapped.withDomain(toStringExpression(domain)));
  }

  /**
   * Define the domain of the cookie. If undefined, will try to use the domain of {@link
   * HttpProtocolBuilder#baseUrl(String)}
   *
   * @param domain the cookie domain, expressed as a function
   * @return a new GetCookie
   */
  @NonNull
  public GetCookie withDomain(@NonNull Function<Session, String> domain) {
    return new GetCookie(wrapped.withDomain(javaFunctionToExpression(domain)));
  }

  /**
   * Define the path of the cookie.
   *
   * @param path the cookie path
   * @return a new GetCookie
   */
  @NonNull
  public GetCookie withPath(@NonNull String path) {
    return new GetCookie(wrapped.withPath(path));
  }

  /**
   * Define the secure attribute of the cookie.
   *
   * @param secure the cookie secure attribute
   * @return a new GetCookie
   */
  @NonNull
  public GetCookie withSecure(boolean secure) {
    return new GetCookie(wrapped.withSecure(secure));
  }

  /**
   * Define the {@link Session} key to save the cookie value. If undefined, will use the cookie name
   *
   * @param saveAs the key
   * @return a new GetCookie
   */
  @NonNull
  public GetCookie saveAs(@NonNull String saveAs) {
    return new GetCookie(wrapped.saveAs(saveAs));
  }

  public GetCookieDsl asScala() {
    return wrapped;
  }
}
