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

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.Session;
import scala.Function1;

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public class Http {

  private final Function1<io.gatling.core.session.Session, Validation<String>> name;

  public Http(Function1<io.gatling.core.session.Session, Validation<String>> name) {
    this.name = name;
  }

  public HttpRequestActionBuilder get(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).get(toStringExpression(url)));
  }

  public HttpRequestActionBuilder get(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).get(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder put(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).put(toStringExpression(url)));
  }

  public HttpRequestActionBuilder put(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).put(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder post(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).post(toStringExpression(url)));
  }

  public HttpRequestActionBuilder post(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).post(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder patch(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).post(toStringExpression(url)));
  }

  public HttpRequestActionBuilder patch(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).patch(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder head(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).head(toStringExpression(url)));
  }

  public HttpRequestActionBuilder head(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).head(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder delete(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).delete(toStringExpression(url)));
  }

  public HttpRequestActionBuilder delete(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).delete(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder options(String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).options(toStringExpression(url)));
  }

  public HttpRequestActionBuilder options(Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).options(toTypedGatlingSessionFunction(url)));
  }

  public HttpRequestActionBuilder httpRequest(String method, String url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).httpRequest(method, toStringExpression(url)));
  }

  public HttpRequestActionBuilder httpRequest(String method, Function<Session, String> url) {
    return new HttpRequestActionBuilder(new io.gatling.http.request.builder.Http(name).httpRequest(method, toTypedGatlingSessionFunction(url)));
  }
}
