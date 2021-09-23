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

import io.gatling.http.action.cookie.AddCookieDsl;

public final class AddCookie {

  private final AddCookieDsl wrapped;

  public AddCookie(AddCookieDsl wrapped) {
    this.wrapped = wrapped;
  }

  public AddCookie withDomain(String domain) {
    return new AddCookie(wrapped.withDomain(domain));
  }
  public AddCookie withPath(String path) {
    return new AddCookie(wrapped.withPath(path));
  }
  public AddCookie withMaxAge(int maxAge) {
    return new AddCookie(wrapped.withMaxAge(maxAge));
  }
  public AddCookie withSecure(boolean secure) {
    return new AddCookie(wrapped.withSecure(secure));
  }

  public AddCookieDsl asScala() {
    return wrapped;
  }
}
