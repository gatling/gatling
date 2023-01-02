/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import io.gatling.http.client.Request;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.SignatureCalculators;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;

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
  @Nonnull
  public T queryParam(@Nonnull String name, @Nonnull String value) {
    return make(wrapped -> wrapped.queryParam(toStringExpression(name), toAnyExpression(value)));
  }

  /**
   * Set some query parameter
   *
   * @param name the name of the parameter, expressed as a function
   * @param value the value of the parameter, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @Nonnull
  public T queryParam(@Nonnull Function<Session, String> name, @Nonnull String value) {
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
  @Nonnull
  public T queryParam(@Nonnull String name, @Nonnull Object value) {
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
  @Nonnull
  public T queryParam(@Nonnull Function<Session, String> name, @Nonnull Object value) {
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
  @Nonnull
  public T queryParam(@Nonnull String name, @Nonnull Function<Session, Object> value) {
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
  @Nonnull
  public T queryParam(
      @Nonnull Function<Session, String> name, @Nonnull Function<Session, Object> value) {
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
  @Nonnull
  public T multivaluedQueryParam(@Nonnull String name, @Nonnull List<Object> values) {
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
  @Nonnull
  public T multivaluedQueryParam(
      @Nonnull Function<Session, String> name, @Nonnull List<Object> values) {
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
  @Nonnull
  public T multivaluedQueryParam(@Nonnull String name, @Nonnull String values) {
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
  @Nonnull
  public T multivaluedQueryParam(@Nonnull Function<Session, String> name, @Nonnull String values) {
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
  @Nonnull
  public T multivaluedQueryParam(
      @Nonnull String name, @Nonnull Function<Session, List<Object>> values) {
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
  @Nonnull
  public T multivaluedQueryParam(
      @Nonnull Function<Session, String> name, @Nonnull Function<Session, List<Object>> values) {
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
  @Nonnull
  public T queryParamSeq(@Nonnull List<Map.Entry<String, Object>> seq) {
    return make(wrapped -> wrapped.queryParamSeq(toScalaTuple2Seq(seq)));
  }

  /**
   * Set multiple query params
   *
   * @param seq a List of query params, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @Nonnull
  public T queryParamSeq(@Nonnull String seq) {
    return make(wrapped -> wrapped.queryParamSeq(toSeqExpression(seq)));
  }

  /**
   * Set multiple query params
   *
   * @param seq a List of query params, expressed as a function
   * @return a new DSL instance
   */
  @Nonnull
  public T queryParamSeq(@Nonnull Function<Session, List<Map.Entry<String, Object>>> seq) {
    return make(wrapped -> wrapped.queryParamSeq(javaPairListFunctionToTuple2SeqExpression(seq)));
  }

  /**
   * Set multiple query params
   *
   * @param map a static Map of query params
   * @return a new DSL instance
   */
  @Nonnull
  public T queryParamMap(@Nonnull Map<String, Object> map) {
    return make(wrapped -> wrapped.queryParamMap(toScalaMap(map)));
  }

  /**
   * Set multiple query params
   *
   * @param map a Map of query params, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @Nonnull
  public T queryParamMap(@Nonnull String map) {
    return make(wrapped -> wrapped.queryParamMap(toMapExpression(map)));
  }

  /**
   * Set multiple query params
   *
   * @param map a Map of query params, expressed as a function
   * @return a new DSL instance
   */
  @Nonnull
  public T queryParamMap(@Nonnull Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.queryParamMap(javaMapFunctionToExpression(map)));
  }

  /**
   * Set a header
   *
   * @param name the static header name
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new DSL instance
   */
  @Nonnull
  public T header(@Nonnull CharSequence name, @Nonnull String value) {
    return make(wrapped -> wrapped.header(name, toStringExpression(value)));
  }

  /**
   * Set a header
   *
   * @param name the static header name
   * @param value the header value, expressed as a function
   * @return a new DSL instance
   */
  @Nonnull
  public T header(@Nonnull CharSequence name, @Nonnull Function<Session, String> value) {
    return make(wrapped -> wrapped.header(name, javaFunctionToExpression(value)));
  }

  /**
   * Set multiple headers
   *
   * @param headers the headers, names are static but values are expressed as a Gatling Expression
   *     Language String
   * @return a new DSL instance
   */
  @Nonnull
  public T headers(@Nonnull Map<? extends CharSequence, String> headers) {
    return make(wrapped -> wrapped.headers(toScalaMap(headers)));
  }

  /**
   * Ignore common headers set in the Http protocol configuration
   *
   * @return a new DSL instance
   */
  @Nonnull
  public T ignoreProtocolHeaders() {
    return make(io.gatling.http.request.builder.RequestBuilder::ignoreProtocolHeaders);
  }

  /**
   * Set the content-type header for JSON
   *
   * @return a new DSL instance
   */
  @Nonnull
  public T asJson() {
    return make(io.gatling.http.request.builder.RequestBuilder::asJson);
  }

  /**
   * Set the content-type header for XML
   *
   * @return a new DSL instance
   */
  @Nonnull
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
  @Nonnull
  public T basicAuth(@Nonnull String username, @Nonnull String password) {
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
  @Nonnull
  public T basicAuth(@Nonnull String username, @Nonnull Function<Session, String> password) {
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
  @Nonnull
  public T basicAuth(@Nonnull Function<Session, String> username, @Nonnull String password) {
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
  @Nonnull
  public T basicAuth(
      @Nonnull Function<Session, String> username, @Nonnull Function<Session, String> password) {
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
  @Nonnull
  public T digestAuth(@Nonnull String username, @Nonnull String password) {
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
  @Nonnull
  public T digestAuth(@Nonnull String username, @Nonnull Function<Session, String> password) {
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
  @Nonnull
  public T digestAuth(@Nonnull Function<Session, String> username, @Nonnull String password) {
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
  @Nonnull
  public T digestAuth(
      @Nonnull Function<Session, String> username, @Nonnull Function<Session, String> password) {
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
  @Nonnull
  public T virtualHost(@Nonnull String virtualHost) {
    return make(wrapped -> (wrapped.virtualHost(toStringExpression(virtualHost))));
  }

  /**
   * Define a virtual host
   *
   * @param virtualHost the virtual host, expressed as a function
   * @return a new DSL instance
   */
  @Nonnull
  public T virtualHost(@Nonnull Function<Session, String> virtualHost) {
    return make(wrapped -> wrapped.virtualHost(javaFunctionToExpression(virtualHost)));
  }

  /**
   * Disable the automatic url encoding that tries to detect unescaped reserved chars
   *
   * @return a new DSL instance
   */
  @Nonnull
  public T disableUrlEncoding() {
    return make(io.gatling.http.request.builder.RequestBuilder::disableUrlEncoding);
  }

  /**
   * Define a Proxy to be used for this request
   *
   * @param proxy the proxy
   * @return a new DSL instance
   */
  @Nonnull
  public T proxy(@Nonnull Proxy proxy) {
    return make(wrapped -> wrapped.proxy(proxy.asScala()));
  }

  /**
   * Provide a function to sign the requests before writing them on the wire
   *
   * @param calculator the signing function
   * @return a new DSL instance
   */
  @Nonnull
  public T sign(@Nonnull Consumer<Request> calculator) {
    return sign((request, session) -> calculator.accept(request));
  }

  /**
   * Provide a function to sign the requests before writing them on the wire. This version provides
   * access to the session.
   *
   * @param calculator the signing function
   * @return a new DSL instance
   */
  @Nonnull
  public T sign(@Nonnull BiConsumer<Request, Session> calculator) {
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
  @Nonnull
  public T signWithOAuth1(
      @Nonnull String consumerKey,
      @Nonnull String clientSharedSecret,
      @Nonnull String token,
      @Nonnull String tokenSecret) {
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
  @Nonnull
  public T signWithOAuth1(
      @Nonnull Function<Session, String> consumerKey,
      @Nonnull Function<Session, String> clientSharedSecret,
      @Nonnull Function<Session, String> token,
      @Nonnull Function<Session, String> tokenSecret) {
    return make(
        wrapped ->
            wrapped.signWithOAuth1(
                javaFunctionToExpression(consumerKey),
                javaFunctionToExpression(clientSharedSecret),
                javaFunctionToExpression(token),
                javaFunctionToExpression(tokenSecret)));
  }
}
