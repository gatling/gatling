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

import static io.gatling.javaapi.core.internal.Expressions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.commons.validation.Validation;
import io.gatling.javaapi.core.Session;
import java.util.function.Function;
import scala.Function1;

/**
 * DSL for bootstrapping HTTP requests.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Http {

  private final Function1<io.gatling.core.session.Session, Validation<String>> name;

  Http(Function1<io.gatling.core.session.Session, Validation<String>> name) {
    this.name = name;
  }

  /**
   * Define a GET request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder get(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).get(toStringExpression(url)));
  }

  /**
   * Define a GET request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder get(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).get(javaFunctionToExpression(url)));
  }

  /**
   * Define a PUT request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder put(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).put(toStringExpression(url)));
  }

  /**
   * Define a PUT request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder put(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).put(javaFunctionToExpression(url)));
  }

  /**
   * Define a POST request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder post(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).post(toStringExpression(url)));
  }

  /**
   * Define a POST request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder post(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).post(javaFunctionToExpression(url)));
  }

  /**
   * Define a PATCH request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder patch(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).patch(toStringExpression(url)));
  }

  /**
   * Define a PATCH request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder patch(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).patch(javaFunctionToExpression(url)));
  }

  /**
   * Define a HEAD request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder head(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).head(toStringExpression(url)));
  }

  /**
   * Define a HEAD request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder head(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).head(javaFunctionToExpression(url)));
  }

  /**
   * Define a DELETE request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder delete(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).delete(toStringExpression(url)));
  }

  /**
   * Define a DELETE request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder delete(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).delete(javaFunctionToExpression(url)));
  }

  /**
   * Define a OPTIONS request
   *
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder options(@NonNull String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).options(toStringExpression(url)));
  }

  /**
   * Define a OPTIONS request
   *
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder options(@NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name).options(javaFunctionToExpression(url)));
  }

  /**
   * Define a HTTP request
   *
   * @param method the HTTP method
   * @param url the url, expressed as a Gatling Expression Language String
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder httpRequest(@NonNull String method, String url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name)
            .httpRequest(method, toStringExpression(url)));
  }

  /**
   * Define a HTTP request
   *
   * @param method the HTTP method
   * @param url the url, expressed as a function
   * @return a new instance of HttpRequestActionBuilder
   */
  @NonNull
  public HttpRequestActionBuilder httpRequest(
      @NonNull String method, @NonNull Function<Session, String> url) {
    return new HttpRequestActionBuilder(
        new io.gatling.http.request.builder.Http(name)
            .httpRequest(method, javaFunctionToExpression(url)));
  }
}
