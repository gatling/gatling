/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;
import static io.gatling.javaapi.http.internal.HttpChecks.toScalaChecks;

import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.http.response.Response;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.ScalaHttpRequestActionBuilderConditions;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * DSL for building HTTP requests configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class HttpRequestActionBuilder
    extends RequestWithBodyActionBuilder<
        HttpRequestActionBuilder, io.gatling.http.request.builder.HttpRequestBuilder> {

  public HttpRequestActionBuilder(io.gatling.http.request.builder.HttpRequestBuilder wrapped) {
    super(wrapped);
  }

  @Override
  protected HttpRequestActionBuilder make(
      Function<
              io.gatling.http.request.builder.HttpRequestBuilder,
              io.gatling.http.request.builder.HttpRequestBuilder>
          f) {
    return new HttpRequestActionBuilder(f.apply(wrapped));
  }

  @Override
  public ActionBuilder asScala() {
    return wrapped;
  }

  /**
   * Apply some checks
   *
   * @param checks the checks
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder check(@NonNull CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  /**
   * Apply some checks
   *
   * @param checks the checks
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder check(@NonNull List<CheckBuilder> checks) {
    return new HttpRequestActionBuilder(wrapped.check(toScalaChecks(checks)));
  }

  /**
   * Apply some checks if some condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  public @NonNull UntypedCondition checkIf(@NonNull String condition) {
    return new UntypedCondition(
        ScalaHttpRequestActionBuilderConditions.untyped(wrapped, condition));
  }

  /**
   * Apply some checks if some condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return the next DSL step
   */
  public @NonNull UntypedCondition checkIf(@NonNull Function<Session, Boolean> condition) {
    return new UntypedCondition(
        ScalaHttpRequestActionBuilderConditions.untyped(wrapped, condition));
  }

  public static final class UntypedCondition {
    private final ScalaHttpRequestActionBuilderConditions.Untyped wrapped;

    public UntypedCondition(ScalaHttpRequestActionBuilderConditions.Untyped wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the checks to be applied when the condition holds true
     *
     * @param thenChecks the checks
     * @return a new HttpRequestActionBuilder instance
     */
    public @NonNull HttpRequestActionBuilder then(@NonNull CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    /**
     * Define the checks to be applied when the condition holds true
     *
     * @param thenChecks the checks
     * @return a new HttpRequestActionBuilder instance
     */
    public @NonNull HttpRequestActionBuilder then(@NonNull List<CheckBuilder> thenChecks) {
      return wrapped.then_(thenChecks);
    }
  }

  /**
   * Apply some checks if some condition holds true
   *
   * @param condition the condition, expressed as a function, aware of the Response and the Session
   * @return the next DSL step
   */
  public TypedCondition checkIf(BiFunction<Response, Session, Boolean> condition) {
    return new TypedCondition(ScalaHttpRequestActionBuilderConditions.typed(wrapped, condition));
  }

  public static final class TypedCondition {
    private final ScalaHttpRequestActionBuilderConditions.Typed wrapped;

    public TypedCondition(ScalaHttpRequestActionBuilderConditions.Typed wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the checks to be applied when the condition holds true
     *
     * @param thenChecks the checks
     * @return a new HttpRequestActionBuilder instance
     */
    public @NonNull HttpRequestActionBuilder then(@NonNull CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    /**
     * Define the checks to be applied when the condition holds true
     *
     * @param thenChecks the checks
     * @return a new HttpRequestActionBuilder instance
     */
    public @NonNull HttpRequestActionBuilder then(@NonNull List<CheckBuilder> thenChecks) {
      return wrapped.then_(thenChecks);
    }
  }

  /**
   * Have this request ignore the common checks defined on the HTTP protocol configuration
   *
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder ignoreProtocolChecks() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::ignoreProtocolChecks);
  }

  /**
   * Instruct the reporting engine to not report stats about this request
   *
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder silent() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::silent);
  }

  /**
   * Instruct the reporting engine to forcefully report stats about this request, ignoring HTTP
   * protocol configuration
   *
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder notSilent() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::notSilent);
  }

  /**
   * Disable automatic redirect following
   *
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder disableFollowRedirect() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::disableFollowRedirect);
  }

  /**
   * Define a transformation on the response before applying the checks.
   *
   * @param f the transformation
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder transformResponse(
      @NonNull BiFunction<Response, Session, Response> f) {
    return make(wrapped -> wrapped.transformResponse(javaBiFunctionToExpression(f)));
  }

  /**
   * Set some resources to be fetched concurrently after the main request. Next action in the
   * Scenario will be performed once all resources are fetched.
   *
   * @param res the resources
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder resources(@NonNull HttpRequestActionBuilder... res) {
    return resources(Arrays.asList(res));
  }

  /**
   * Set some resources to be fetched concurrently after the main request. Next action in the
   * Scenario will be performed once all resources are fetched.
   *
   * @param res the resources
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder resources(@NonNull List<HttpRequestActionBuilder> res) {
    return make(
        wrapped ->
            wrapped.resources(
                toScalaSeq(res.stream().map(r -> r.wrapped).collect(Collectors.toList()))));
  }

  /**
   * Override the default request timeout defined in gatling.conf
   *
   * @param timeout the timeout in seconds
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder requestTimeout(int timeout) {
    return requestTimeout(Duration.ofSeconds(timeout));
  }

  /**
   * Override the default request timeout defined in gatling.conf
   *
   * @param timeout the timeout
   * @return a new HttpRequestActionBuilder instance
   */
  public @NonNull HttpRequestActionBuilder requestTimeout(@NonNull Duration timeout) {
    return make(wrapped -> wrapped.requestTimeout(toScalaDuration(timeout)));
  }
}
