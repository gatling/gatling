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

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.http.client.Request;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.SignatureCalculators;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Base DSL for HTTP, WebSocket and SSE requests
 *
 * @param <T> the type of Java request builder
 * @param <W> the type of wrapped Scala request builder
 */
public abstract class RequestActionBuilder<
        T extends RequestActionBuilder<T, W>,
        W extends io.gatling.http.request.builder.RequestBuilder<W>>
    implements ActionBuilder {
  final W wrapped;

  RequestActionBuilder(W wrapped) {
    this.wrapped = wrapped;
  }

  protected abstract T make(Function<W, W> f);

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a Gatling Expression Language String
   * @param value the value of the parameter, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T queryParam(@NonNull String name, @NonNull String value) {
    return make(wrapped -> wrapped.queryParam(toStringExpression(name), toAnyExpression(value)));
  }

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param value the value of the parameter, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T queryParam(@NonNull Function<Session, String> name, @NonNull String value) {
    return make(
        wrapped -> wrapped.queryParam(javaFunctionToExpression(name), toAnyExpression(value)));
  }

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a Gatling Expression Language String
   * @param value the static value of the parameter
   * @return a new DSL instance
   */
  @NonNull
  public T queryParam(@NonNull String name, @NonNull Object value) {
    return make(
        wrapped -> wrapped.queryParam(toStringExpression(name), toStaticValueExpression(value)));
  }

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param value the static value of the parameter
   * @return a new DSL instance
   */
  @NonNull
  public T queryParam(@NonNull Function<Session, String> name, @NonNull Object value) {
    return make(
        wrapped ->
            wrapped.queryParam(javaFunctionToExpression(name), toStaticValueExpression(value)));
  }

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a Gatling Expression Language String
   * @param value the value of the parameter, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T queryParam(@NonNull String name, @NonNull Function<Session, Object> value) {
    return make(
        wrapped -> wrapped.queryParam(toStringExpression(name), javaFunctionToExpression(value)));
  }

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param value the value of the parameter, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T queryParam(
      @NonNull Function<Session, String> name, @NonNull Function<Session, Object> value) {
    return make(
        wrapped ->
            wrapped.queryParam(javaFunctionToExpression(name), javaFunctionToExpression(value)));
  }

  /**
   * Set a multivalued query parameter
   *
   * @param name the name of the parameter, expressed as a Gatling Expression Language String
   * @param values the static list of values of the parameter
   * @return a new DSL instance
   */
  @NonNull
  public T multivaluedQueryParam(@NonNull String name, @NonNull List<Object> values) {
    return make(
        wrapped ->
            wrapped.multivaluedQueryParam(
                toStringExpression(name), toStaticValueExpression(toScalaSeq(values))));
  }

  /**
   * Set a multivalued query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param values the static list of values of the parameter
   * @return a new DSL instance
   */
  @NonNull
  public T multivaluedQueryParam(
      @NonNull Function<Session, String> name, @NonNull List<Object> values) {
    return make(
        wrapped ->
            wrapped.multivaluedQueryParam(
                javaFunctionToExpression(name), toStaticValueExpression(toScalaSeq(values))));
  }

  /**
   * Set a multivalued query parameter
   *
   * @param name the name of the parameter, expressed as a Gatling Expression Language String
   * @param values the list of values of the parameter, expressed as a Gatling Expression Language
   *     String
   * @return a new DSL instance
   */
  @NonNull
  public T multivaluedQueryParam(@NonNull String name, @NonNull String values) {
    return make(
        wrapped ->
            wrapped.multivaluedQueryParam(toStringExpression(name), toSeqExpression(values)));
  }

  /**
   * Set a multivalued query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param values the list of values of the parameter, expressed as a Gatling Expression Language
   *     String
   * @return a new DSL instance
   */
  @NonNull
  public T multivaluedQueryParam(@NonNull Function<Session, String> name, @NonNull String values) {
    return make(
        wrapped ->
            wrapped.multivaluedQueryParam(javaFunctionToExpression(name), toSeqExpression(values)));
  }

  /**
   * Set a multivalued query parameter
   *
   * @param name the name of the parameter, expressed as a Gatling Expression Language String
   * @param values the list of values of the parameter, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T multivaluedQueryParam(
      @NonNull String name, @NonNull Function<Session, List<Object>> values) {
    return make(
        wrapped ->
            wrapped.multivaluedQueryParam(
                toStringExpression(name), javaListFunctionToExpression(values)));
  }

  /**
   * Set a multivalued query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param values the list of values of the parameter, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T multivaluedQueryParam(
      @NonNull Function<Session, String> name, @NonNull Function<Session, List<Object>> values) {
    return make(
        wrapped ->
            wrapped.multivaluedQueryParam(
                javaFunctionToExpression(name), javaListFunctionToExpression(values)));
  }

  /**
   * Set multiple query params
   *
   * @param seq a static List of query params
   * @return a new DSL instance
   */
  @NonNull
  public T queryParamSeq(@NonNull List<Map.Entry<String, Object>> seq) {
    return make(wrapped -> wrapped.queryParamSeq(toScalaTuple2Seq(seq)));
  }

  /**
   * Set multiple query params
   *
   * @param seq a List of query params, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T queryParamSeq(@NonNull String seq) {
    return make(wrapped -> wrapped.queryParamSeq(toSeqExpression(seq)));
  }

  /**
   * Set multiple query params
   *
   * @param seq a List of query params, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T queryParamSeq(@NonNull Function<Session, List<Map.Entry<String, Object>>> seq) {
    return make(wrapped -> wrapped.queryParamSeq(javaPairListFunctionToTuple2SeqExpression(seq)));
  }

  /**
   * Set multiple query params
   *
   * @param map a static Map of query params
   * @return a new DSL instance
   */
  @NonNull
  public T queryParamMap(@NonNull Map<String, Object> map) {
    return make(wrapped -> wrapped.queryParamMap(toScalaMap(map)));
  }

  /**
   * Set multiple query params
   *
   * @param map a Map of query params, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T queryParamMap(@NonNull String map) {
    return make(wrapped -> wrapped.queryParamMap(toMapExpression(map)));
  }

  /**
   * Set multiple query params
   *
   * @param map a Map of query params, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T queryParamMap(@NonNull Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.queryParamMap(javaMapFunctionToExpression(map)));
  }

  /**
   * Set a header
   *
   * @param name the static header name
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T header(@NonNull CharSequence name, @NonNull String value) {
    return make(wrapped -> wrapped.header(name, toStringExpression(value)));
  }

  /**
   * Set a header
   *
   * @param name the static header name
   * @param value the header value, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T header(@NonNull CharSequence name, @NonNull Function<Session, String> value) {
    return make(wrapped -> wrapped.header(name, javaFunctionToExpression(value)));
  }

  /**
   * Set multiple headers
   *
   * @param headers the headers, names are static but values are expressed as a Gatling Expression
   *     Language String
   * @return a new DSL instance
   */
  @NonNull
  public T headers(@NonNull Map<? extends CharSequence, String> headers) {
    return make(wrapped -> wrapped.headers(toScalaMap(headers)));
  }

  /**
   * Ignore common headers set in the Http protocol configuration
   *
   * @return a new DSL instance
   */
  @NonNull
  public T ignoreProtocolHeaders() {
    return make(io.gatling.http.request.builder.RequestBuilder::ignoreProtocolHeaders);
  }

  /**
   * Set the content-type header for JSON
   *
   * @return a new DSL instance
   */
  @NonNull
  public T asJson() {
    return make(io.gatling.http.request.builder.RequestBuilder::asJson);
  }

  /**
   * Set the content-type header for XML
   *
   * @return a new DSL instance
   */
  @NonNull
  public T asXml() {
    return make(io.gatling.http.request.builder.RequestBuilder::asXml);
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T basicAuth(@NonNull String username, @NonNull String password) {
    return make(
        wrapped -> wrapped.basicAuth(toStringExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T basicAuth(@NonNull String username, @NonNull Function<Session, String> password) {
    return make(
        wrapped ->
            wrapped.basicAuth(toStringExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T basicAuth(@NonNull Function<Session, String> username, @NonNull String password) {
    return make(
        wrapped ->
            wrapped.basicAuth(javaFunctionToExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T basicAuth(
      @NonNull Function<Session, String> username, @NonNull Function<Session, String> password) {
    return make(
        wrapped ->
            wrapped.basicAuth(
                javaFunctionToExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T digestAuth(@NonNull String username, @NonNull String password) {
    return make(
        wrapped -> wrapped.digestAuth(toStringExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T digestAuth(@NonNull String username, @NonNull Function<Session, String> password) {
    return make(
        wrapped ->
            wrapped.digestAuth(toStringExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T digestAuth(@NonNull Function<Session, String> username, @NonNull String password) {
    return make(
        wrapped ->
            wrapped.digestAuth(javaFunctionToExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T digestAuth(
      @NonNull Function<Session, String> username, @NonNull Function<Session, String> password) {
    return make(
        wrapped ->
            wrapped.digestAuth(
                javaFunctionToExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Define a virtual host
   *
   * @param virtualHost the virtual host, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T virtualHost(@NonNull String virtualHost) {
    return make(wrapped -> (wrapped.virtualHost(toStringExpression(virtualHost))));
  }

  /**
   * Define a virtual host
   *
   * @param virtualHost the virtual host, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T virtualHost(@NonNull Function<Session, String> virtualHost) {
    return make(wrapped -> wrapped.virtualHost(javaFunctionToExpression(virtualHost)));
  }

  /**
   * Disable the automatic url encoding that tries to detect unescaped reserved chars
   *
   * @return a new DSL instance
   */
  @NonNull
  public T disableUrlEncoding() {
    return make(io.gatling.http.request.builder.RequestBuilder::disableUrlEncoding);
  }

  /**
   * Define a Proxy to be used for this request
   *
   * @param proxy the proxy
   * @return a new DSL instance
   */
  @NonNull
  public T proxy(@NonNull Proxy proxy) {
    return make(wrapped -> wrapped.proxy(proxy.asScala()));
  }

  /**
   * Provide a function to sign the requests before writing them on the wire
   *
   * @param calculator the signing function
   * @return a new DSL instance
   */
  @NonNull
  public T sign(@NonNull Function<Request, Request> calculator) {
    return sign((request, session) -> calculator.apply(request));
  }

  /**
   * Provide a function to sign the requests before writing them on the wire. This version provides
   * access to the session.
   *
   * @param calculator the signing function
   * @return a new DSL instance
   */
  @NonNull
  public T sign(@NonNull BiFunction<Request, Session, Request> calculator) {
    return make(wrapped -> wrapped.sign(SignatureCalculators.toScala(calculator)));
  }

  /**
   * Instruct sign the request with OAuth1 before writing it on the wire
   *
   * @param consumerKey the consumerKey, expressed as a Gatling Expression Language String
   * @param clientSharedSecret the clientSharedSecret, expressed as a Gatling Expression Language
   *     String
   * @param token the token, expressed as a Gatling Expression Language String
   * @param tokenSecret the tokenSecret, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @NonNull
  public T signWithOAuth1(
      @NonNull String consumerKey,
      @NonNull String clientSharedSecret,
      @NonNull String token,
      @NonNull String tokenSecret) {
    return make(
        wrapped ->
            wrapped.signWithOAuth1(
                toStringExpression(consumerKey),
                toStringExpression(clientSharedSecret),
                toStringExpression(token),
                toStringExpression(tokenSecret)));
  }

  /**
   * Instruct sign the request with OAuth1 before writing it on the wire
   *
   * @param consumerKey the consumerKey, expressed as a function
   * @param clientSharedSecret the clientSharedSecret, expressed as a function
   * @param token the token, expressed as a function
   * @param tokenSecret the tokenSecret, expressed as a function
   * @return a new DSL instance
   */
  @NonNull
  public T signWithOAuth1(
      @NonNull Function<Session, String> consumerKey,
      @NonNull Function<Session, String> clientSharedSecret,
      @NonNull Function<Session, String> token,
      @NonNull Function<Session, String> tokenSecret) {
    return make(
        wrapped ->
            wrapped.signWithOAuth1(
                javaFunctionToExpression(consumerKey),
                javaFunctionToExpression(clientSharedSecret),
                javaFunctionToExpression(token),
                javaFunctionToExpression(tokenSecret)));
  }
}
