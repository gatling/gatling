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

import io.gatling.core.javaapi.Session;
import io.gatling.http.action.cookie.GetCookieDsl;

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public final class GetCookie {

  private final GetCookieDsl wrapped;

  public GetCookie(GetCookieDsl wrapped) {
    this.wrapped = wrapped;
  }

  public GetCookie withDomain(String domain) {
    return new GetCookie(wrapped.withDomain(toStringExpression(domain)));
  }
  public GetCookie withDomain(Function<Session, String> domain) {
    return new GetCookie(wrapped.withDomain(toTypedGatlingSessionFunction(domain)));
  }
  public GetCookie withPath(String path) {
    return new GetCookie(wrapped.withPath(path));
  }
  public GetCookie withSecure(boolean secure) {
    return new GetCookie(wrapped.withSecure(secure));
  }
  public GetCookie saveAs(String saveAs) {
    return new GetCookie(wrapped.saveAs(saveAs));
  }

  public GetCookieDsl asScala() {
    return wrapped;
  }
}
