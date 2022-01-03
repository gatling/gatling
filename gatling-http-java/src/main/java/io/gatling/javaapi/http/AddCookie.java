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

import io.gatling.http.action.cookie.AddCookieDsl;
import javax.annotation.Nonnull;

/**
 * DSL for adding a <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies">cookie</a>
 * in the virtual user's CookieJar instead of having the server send a Set-Cookie header.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class AddCookie {

  private final AddCookieDsl wrapped;

  AddCookie(AddCookieDsl wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Define the domain of the cookie. If undefined, will try to use the domain of {@link
   * HttpProtocolBuilder#baseUrl(String)}
   *
   * @param domain the cookie domain
   * @return a new AddCookie
   */
  @Nonnull
  public AddCookie withDomain(@Nonnull String domain) {
    return new AddCookie(wrapped.withDomain(domain));
  }

  /**
   * Define the path of the cookie.
   *
   * @param path the cookie path
   * @return a new AddCookie
   */
  @Nonnull
  public AddCookie withPath(@Nonnull String path) {
    return new AddCookie(wrapped.withPath(path));
  }

  /**
   * Define the maxAge attribute of the cookie.
   *
   * @param maxAge the cookie maxAge
   * @return a new AddCookie
   */
  @Nonnull
  public AddCookie withMaxAge(int maxAge) {
    return new AddCookie(wrapped.withMaxAge(maxAge));
  }

  /**
   * Define the secure attribute of the cookie.
   *
   * @param secure if the cookie must only be sent with HTTPS requests
   * @return a new AddCookie
   */
  @Nonnull
  public AddCookie withSecure(boolean secure) {
    return new AddCookie(wrapped.withSecure(secure));
  }

  public AddCookieDsl asScala() {
    return wrapped;
  }
}
