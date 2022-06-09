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

import static io.gatling.javaapi.core.internal.Bodies.*;
import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;
import static io.gatling.javaapi.http.internal.HttpChecks.toScalaChecks;

import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.http.response.Response;
import io.gatling.javaapi.core.Body;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.ScalaHttpRequestActionBuilderConditions;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * DSL for building HTTP requests configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class HttpRequestActionBuilder
    extends RequestActionBuilder<
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
  @Nonnull
  public HttpRequestActionBuilder check(@Nonnull CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  /**
   * Apply some checks
   *
   * @param checks the checks
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder check(@Nonnull List<CheckBuilder> checks) {
    return new HttpRequestActionBuilder(wrapped.check(toScalaChecks(checks)));
  }

  /**
   * Apply some checks if some condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public UntypedCondition checkIf(@Nonnull String condition) {
    return new UntypedCondition(
        ScalaHttpRequestActionBuilderConditions.untyped(wrapped, condition));
  }

  /**
   * Apply some checks if some condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public UntypedCondition checkIf(@Nonnull Function<Session, Boolean> condition) {
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
    @Nonnull
    public HttpRequestActionBuilder then(@Nonnull CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    /**
     * Define the checks to be applied when the condition holds true
     *
     * @param thenChecks the checks
     * @return a new HttpRequestActionBuilder instance
     */
    @Nonnull
    public HttpRequestActionBuilder then(@Nonnull List<CheckBuilder> thenChecks) {
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
    @Nonnull
    public HttpRequestActionBuilder then(@Nonnull CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    /**
     * Define the checks to be applied when the condition holds true
     *
     * @param thenChecks the checks
     * @return a new HttpRequestActionBuilder instance
     */
    @Nonnull
    public HttpRequestActionBuilder then(@Nonnull List<CheckBuilder> thenChecks) {
      return wrapped.then_(thenChecks);
    }
  }

  /**
   * Have this request ignore the common checks defined on the HTTP protocol configuration
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder ignoreProtocolChecks() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::ignoreProtocolChecks);
  }

  /**
   * Instruct the reporting engine to not report stats about this request
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder silent() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::silent);
  }

  /**
   * Instruct the reporting engine to forcefully report stats about this request, ignoring HTTP
   * protocol configuration
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder notSilent() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::notSilent);
  }

  /**
   * Disable automatic redirect following
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder disableFollowRedirect() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::disableFollowRedirect);
  }

  /**
   * Define a transformation on the response before applying the checks.
   *
   * @param f the transformation
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder transformResponse(
      @Nonnull BiFunction<Response, Session, Response> f) {
    return make(wrapped -> wrapped.transformResponse(javaBiFunctionToExpression(f)));
  }

  /**
   * Define a request body
   *
   * @param body the request body
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder body(Body body) {
    return make(wrapped -> wrapped.body(body.asScala()));
  }

  /**
   * Define to transform the request body before writing it on the wire
   *
   * @param processor the processing function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder processRequestBody(
      @Nonnull Function<Body, ? extends Body> processor) {
    return make(
        wrapped ->
            wrapped.processRequestBody(
                scalaBody -> processor.apply(toJavaBody(scalaBody)).asScala()));
  }

  /**
   * Set a multipart body part
   *
   * @param part the part
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder bodyPart(@Nonnull BodyPart part) {
    return make(wrapped -> wrapped.bodyPart(part.asScala()));
  }

  /**
   * Set multiple multipart body parts
   *
   * @param parts the parts
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder bodyParts(@Nonnull BodyPart... parts) {
    return make(
        wrapped ->
            wrapped.bodyParts(
                toScalaSeq(
                    Arrays.stream(parts).map(BodyPart::asScala).collect(Collectors.toList()))));
  }

  /**
   * Set multiple multipart body parts
   *
   * @param parts the parts
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder bodyParts(@Nonnull List<BodyPart> parts) {
    return make(
        wrapped ->
            wrapped.bodyParts(
                toScalaSeq(parts.stream().map(BodyPart::asScala).collect(Collectors.toList()))));
  }

  /**
   * Set some resources to be fetched concurrently after the main request. Next action in the
   * Scenario will be performed once all resources are fetched.
   *
   * @param res the resources
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder resources(@Nonnull HttpRequestActionBuilder... res) {
    return resources(Arrays.asList(res));
  }

  /**
   * Set some resources to be fetched concurrently after the main request. Next action in the
   * Scenario will be performed once all resources are fetched.
   *
   * @param res the resources
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder resources(@Nonnull List<HttpRequestActionBuilder> res) {
    return make(
        wrapped ->
            wrapped.resources(
                toScalaSeq(res.stream().map(r -> r.wrapped).collect(Collectors.toList()))));
  }

  /**
   * Set the content-type header for multipart body.
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder asMultipartForm() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::asMultipartForm);
  }

  /**
   * Set the content-type header for form-urlencoding body.
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder asFormUrlEncoded() {
    return make(io.gatling.http.request.builder.HttpRequestBuilder::asFormUrlEncoded);
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param value the parameter value, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParam(@Nonnull String key, @Nonnull String value) {
    return make(wrapped -> wrapped.formParam(toStringExpression(key), toAnyExpression(value)));
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a function
   * @param value the parameter value, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParam(
      @Nonnull Function<Session, String> key, @Nonnull String value) {
    return make(
        wrapped -> wrapped.formParam(javaFunctionToExpression(key), toAnyExpression(value)));
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param value the parameter static value
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParam(@Nonnull String key, @Nonnull Object value) {
    return make(
        wrapped -> wrapped.formParam(toStringExpression(key), toStaticValueExpression(value)));
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a function
   * @param value the parameter static value
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParam(
      @Nonnull Function<Session, String> key, @Nonnull Object value) {
    return make(
        wrapped ->
            wrapped.formParam(javaFunctionToExpression(key), toStaticValueExpression(value)));
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param value the parameter value, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParam(
      @Nonnull String key, @Nonnull Function<Session, Object> value) {
    return make(
        wrapped -> wrapped.formParam(toStringExpression(key), javaFunctionToExpression(value)));
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a function
   * @param value the parameter value, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParam(
      @Nonnull Function<Session, String> key, @Nonnull Function<Session, Object> value) {
    return make(
        wrapped ->
            wrapped.formParam(javaFunctionToExpression(key), javaFunctionToExpression(value)));
  }

  /**
   * Set an HTML form multivalued parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param values the static parameter values
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder multivaluedFormParam(
      @Nonnull String key, @Nonnull List<Object> values) {
    return make(
        wrapped ->
            wrapped.multivaluedFormParam(
                toStringExpression(key), toStaticValueExpression(toScalaSeq(values))));
  }

  /**
   * Set an HTML form multivalued parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param values the parameter values, as a Gatling EL string
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder multivaluedFormParam(
      @Nonnull String key, @Nonnull String values) {
    return make(
        wrapped -> wrapped.multivaluedFormParam(toStringExpression(key), toSeqExpression(values)));
  }

  /**
   * Set an HTML form multivalued parameter
   *
   * @param key the parameter key, expressed as a function
   * @param values the static parameter values
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder multivaluedFormParam(
      @Nonnull Function<Session, String> key, @Nonnull List<Object> values) {
    return make(
        wrapped ->
            wrapped.multivaluedFormParam(
                javaFunctionToExpression(key), toStaticValueExpression(toScalaSeq(values))));
  }

  /**
   * Set an HTML form multivalued parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param values the parameter values, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder multivaluedFormParam(
      @Nonnull String key, @Nonnull Function<Session, List<Object>> values) {
    return make(
        wrapped ->
            wrapped.multivaluedFormParam(
                toStringExpression(key), javaListFunctionToExpression(values)));
  }

  /**
   * Set an HTML form multivalued parameter
   *
   * @param key the parameter key, expressed as a function
   * @param values the parameter values, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder multivaluedFormParam(
      @Nonnull Function<Session, String> key, @Nonnull Function<Session, List<Object>> values) {
    return make(
        wrapped ->
            wrapped.multivaluedFormParam(
                javaFunctionToExpression(key), javaListFunctionToExpression(values)));
  }

  /**
   * Set multiple form parameters
   *
   * @param seq the static parameters
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParamSeq(@Nonnull List<Map.Entry<String, Object>> seq) {
    return make(wrapped -> wrapped.formParamSeq(toScalaTuple2Seq(seq)));
  }

  /**
   * Set multiple form parameters
   *
   * @param seq the parameters, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParamSeq(
      @Nonnull Function<Session, List<Map.Entry<String, Object>>> seq) {
    return make(wrapped -> wrapped.formParamSeq(javaPairListFunctionToTuple2SeqExpression(seq)));
  }

  /**
   * Set multiple form parameters
   *
   * @param map the static parameters
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParamMap(@Nonnull Map<String, Object> map) {
    return make(wrapped -> wrapped.formParamMap(toScalaMap(map)));
  }

  /**
   * Set multiple form parameters
   *
   * @param map the parameters, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formParamMap(
      @Nonnull Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.formParamMap(javaMapFunctionToExpression(map)));
  }

  /**
   * Set a form, typically captured from a form check
   *
   * @param form the form inputs, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder form(@Nonnull String form) {
    return make(wrapped -> wrapped.formParamMap(toMapExpression(form)));
  }

  /**
   * Set a form, typically captured from a form check
   *
   * @param map the form inputs, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder form(@Nonnull Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.form(javaMapFunctionToExpression(map)));
  }

  /**
   * Set a form file upload
   *
   * @param name the name of the file part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formUpload(@Nonnull String name, @Nonnull String filePath) {
    return make(
        wrapped ->
            wrapped.formUpload(
                toStringExpression(name),
                toStringExpression(filePath),
                io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Set a form file upload
   *
   * @param name the name of the file part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formUpload(
      @Nonnull Function<Session, String> name, @Nonnull String filePath) {
    return make(
        wrapped ->
            wrapped.formUpload(
                javaFunctionToExpression(name),
                toStringExpression(filePath),
                io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Set a form file upload
   *
   * @param name the name of the file part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formUpload(
      @Nonnull String name, Function<Session, String> filePath) {
    return make(
        wrapped ->
            wrapped.formUpload(
                toStringExpression(name),
                javaFunctionToExpression(filePath),
                io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Set a form file upload
   *
   * @param name the name of the file part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder formUpload(
      @Nonnull Function<Session, String> name, @Nonnull Function<Session, String> filePath) {
    return make(
        wrapped ->
            wrapped.formUpload(
                javaFunctionToExpression(name),
                javaFunctionToExpression(filePath),
                io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Override the default request timeout defined in gatling.conf
   *
   * @param timeout the timeout in seconds
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder requestTimeout(int timeout) {
    return requestTimeout(Duration.ofSeconds(timeout));
  }

  /**
   * Override the default request timeout defined in gatling.conf
   *
   * @param timeout the timeout
   * @return a new HttpRequestActionBuilder instance
   */
  @Nonnull
  public HttpRequestActionBuilder requestTimeout(@Nonnull Duration timeout) {
    return make(wrapped -> wrapped.requestTimeout(toScalaDuration(timeout)));
  }
}
