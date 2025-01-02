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

import static io.gatling.javaapi.core.internal.Bodies.toJavaBody;
import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;
import static io.gatling.javaapi.core.internal.Expressions.javaFunctionToExpression;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.Body;
import io.gatling.javaapi.core.Session;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class RequestWithBodyActionBuilder<
        T extends RequestWithBodyActionBuilder<T, W>,
        W extends io.gatling.http.request.builder.RequestWithBodyBuilder<W>>
    extends RequestActionBuilder<T, W> {

  RequestWithBodyActionBuilder(W wrapped) {
    super(wrapped);
  }

  /**
   * Define a request body
   *
   * @param body the request body
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T body(Body body) {
    return make(wrapped -> wrapped.body(body.asScala()));
  }

  /**
   * Define to transform the request body before writing it on the wire
   *
   * @param processor the processing function
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T processRequestBody(@NonNull Function<Body, ? extends Body> processor) {
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
  @NonNull
  public T bodyPart(@NonNull BodyPart part) {
    return make(wrapped -> wrapped.bodyPart(part.asScala()));
  }

  /**
   * Set multiple multipart body parts
   *
   * @param parts the parts
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T bodyParts(@NonNull BodyPart... parts) {
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
  @NonNull
  public T bodyParts(@NonNull List<BodyPart> parts) {
    return make(
        wrapped ->
            wrapped.bodyParts(
                toScalaSeq(parts.stream().map(BodyPart::asScala).collect(Collectors.toList()))));
  }

  /**
   * Set the content-type header for multipart body.
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T asMultipartForm() {
    return make(io.gatling.http.request.builder.RequestWithBodyBuilder::asMultipartForm);
  }

  /**
   * Set the content-type header for form-urlencoding body.
   *
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T asFormUrlEncoded() {
    return make(io.gatling.http.request.builder.RequestWithBodyBuilder::asFormUrlEncoded);
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a Gatling Expression Language String
   * @param value the parameter value, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T formParam(@NonNull String key, @NonNull String value) {
    return make(wrapped -> wrapped.formParam(toStringExpression(key), toAnyExpression(value)));
  }

  /**
   * Set an HTML form parameter
   *
   * @param key the parameter key, expressed as a function
   * @param value the parameter value, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T formParam(@NonNull Function<Session, String> key, @NonNull String value) {
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
  @NonNull
  public T formParam(@NonNull String key, @NonNull Object value) {
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
  @NonNull
  public T formParam(@NonNull Function<Session, String> key, @NonNull Object value) {
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
  @NonNull
  public T formParam(@NonNull String key, @NonNull Function<Session, Object> value) {
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
  @NonNull
  public T formParam(
      @NonNull Function<Session, String> key, @NonNull Function<Session, Object> value) {
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
  @NonNull
  public T multivaluedFormParam(@NonNull String key, @NonNull List<Object> values) {
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
  @NonNull
  public T multivaluedFormParam(@NonNull String key, @NonNull String values) {
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
  @NonNull
  public T multivaluedFormParam(
      @NonNull Function<Session, String> key, @NonNull List<Object> values) {
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
  @NonNull
  public T multivaluedFormParam(
      @NonNull String key, @NonNull Function<Session, List<Object>> values) {
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
  @NonNull
  public T multivaluedFormParam(
      @NonNull Function<Session, String> key, @NonNull Function<Session, List<Object>> values) {
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
  @NonNull
  public T formParamSeq(@NonNull List<Map.Entry<String, Object>> seq) {
    return make(wrapped -> wrapped.formParamSeq(toScalaTuple2Seq(seq)));
  }

  /**
   * Set multiple form parameters
   *
   * @param seq the parameters, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T formParamSeq(@NonNull Function<Session, List<Map.Entry<String, Object>>> seq) {
    return make(wrapped -> wrapped.formParamSeq(javaPairListFunctionToTuple2SeqExpression(seq)));
  }

  /**
   * Set multiple form parameters
   *
   * @param map the static parameters
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T formParamMap(@NonNull Map<String, Object> map) {
    return make(wrapped -> wrapped.formParamMap(toScalaMap(map)));
  }

  /**
   * Set multiple form parameters
   *
   * @param map the parameters, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T formParamMap(@NonNull Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.formParamMap(javaMapFunctionToExpression(map)));
  }

  /**
   * Set a form, typically captured from a form check
   *
   * @param form the form inputs, expressed as a Gatling Expression Language String
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T form(@NonNull String form) {
    return make(wrapped -> wrapped.formParamMap(toMapExpression(form)));
  }

  /**
   * Set a form, typically captured from a form check
   *
   * @param map the form inputs, expressed as a function
   * @return a new HttpRequestActionBuilder instance
   */
  @NonNull
  public T form(@NonNull Function<Session, Map<String, Object>> map) {
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
  @NonNull
  public T formUpload(@NonNull String name, @NonNull String filePath) {
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
  @NonNull
  public T formUpload(@NonNull Function<Session, String> name, @NonNull String filePath) {
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
  @NonNull
  public T formUpload(@NonNull String name, Function<Session, String> filePath) {
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
  @NonNull
  public T formUpload(
      @NonNull Function<Session, String> name, @NonNull Function<Session, String> filePath) {
    return make(
        wrapped ->
            wrapped.formUpload(
                javaFunctionToExpression(name),
                javaFunctionToExpression(filePath),
                io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Set the content-type header for JSON
   *
   * @return a new DSL instance
   */
  @NonNull
  public T asJson() {
    return make(io.gatling.http.request.builder.RequestWithBodyBuilder::asJson);
  }

  /**
   * Set the content-type header for XML
   *
   * @return a new DSL instance
   */
  @NonNull
  public T asXml() {
    return make(io.gatling.http.request.builder.RequestWithBodyBuilder::asXml);
  }
}
