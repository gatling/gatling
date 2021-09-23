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

import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.http.client.SignatureCalculator;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public abstract class RequestActionBuilder<T extends RequestActionBuilder<T, W>, W extends io.gatling.http.request.builder.RequestBuilder<W>> implements ActionBuilder {
  final W wrapped;

  public RequestActionBuilder(W wrapped) {
    this.wrapped = wrapped;
  }

  protected abstract T make(Function<W, W> f);

  public T queryParam(String key, String value) {
     return make(wrapped -> wrapped.queryParam(toStringExpression(key), toAnyExpression(value)));
  }
  public T queryParam(Function<Session, String> key, String value) {
    return make(wrapped -> wrapped.queryParam(toTypedGatlingSessionFunction(key), toAnyExpression(value)));
  }
  public T queryParam(String key, Object value) {
    return make(wrapped -> wrapped.queryParam(toStringExpression(key), toStaticValueExpression(value)));
  }
  public T queryParam(Function<Session, String> key, Object value) {
    return make(wrapped -> wrapped.queryParam(toTypedGatlingSessionFunction(key), toStaticValueExpression(value)));
  }
  public T queryParam(String key, Function<Session, Object> value) {
    return make(wrapped -> wrapped.queryParam(toStringExpression(key), toTypedGatlingSessionFunction(value)));
  }
  public T queryParam(Function<Session, String> key, Function<Session, Object> value) {
    return make(wrapped -> wrapped.queryParam(toTypedGatlingSessionFunction(key), toTypedGatlingSessionFunction(value)));
  }

  public T multivaluedQueryParam(String key, List<Object> values) {
    return make(wrapped -> wrapped.multivaluedQueryParam(toStringExpression(key), toStaticValueExpression(toScalaSeq(values))));
  }
  public T multivaluedQueryParam(Function<Session, String> key, List<Object> values) {
    return make(wrapped -> wrapped.multivaluedQueryParam(toTypedGatlingSessionFunction(key), toStaticValueExpression(toScalaSeq(values))));
  }
  public T multivaluedQueryParam(String key, Function<Session, List<Object>> values) {
    return make(wrapped -> wrapped.multivaluedQueryParam(toStringExpression(key), toGatlingSessionFunctionImmutableSeq(values)));
  }
  public T multivaluedQueryParam(Function<Session, String> key, Function<Session, List<Object>> values) {
    return make(wrapped -> wrapped.multivaluedQueryParam(toTypedGatlingSessionFunction(key), toGatlingSessionFunctionImmutableSeq(values)));
  }

  public T queryParamSeq(List<Map.Entry<String, Object>> seq) {
    return make(wrapped -> wrapped.queryParamSeq(toScalaTuple2Seq(seq)));
  }

  public T queryParamSeq(Function<Session, List<Map.Entry<String, Object>>> seq) {
    return make(wrapped -> wrapped.queryParamSeq(toGatlingSessionFunctionTuple2Seq(seq)));
  }

  public T queryParamMap(Map<String, Object> map) {
    return make(wrapped -> wrapped.queryParamMap(toScalaMap(map)));
  }

  public T queryParamMap(Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.queryParamMap(toGatlingSessionFunctionImmutableMap(map)));
  }

  public T header(CharSequence name, String value) {
    return make(wrapped -> wrapped.header(name, toStringExpression(value)));
  }

  public T header(CharSequence name, Function<Session, String> value) {
    return make(wrapped -> wrapped.header(name, toTypedGatlingSessionFunction(value)));
  }

  public T headers(Map<? extends CharSequence, String> newHeaders) {
    return make(wrapped -> wrapped.headers(toScalaMap(newHeaders)));
  }

  public T ignoreProtocolHeaders() {
    return make(wrapped -> wrapped.ignoreProtocolHeaders());
  }

  public T asJson() {
    return make(wrapped -> wrapped.asJson());
  }

  public T asXml() {
    return make(wrapped -> wrapped.asXml());
  }

  public T basicAuth(String username, String password) {
    return make(wrapped -> wrapped.basicAuth(toStringExpression(username), toStringExpression(password)));
  }

  public T basicAuth(String username, Function<Session, String> password) {
    return make(wrapped -> wrapped.basicAuth(toStringExpression(username), toTypedGatlingSessionFunction(password)));
  }

  public T basicAuth(Function<Session, String> username, String password) {
    return make(wrapped -> wrapped.basicAuth(toTypedGatlingSessionFunction(username), toStringExpression(password)));
  }

  public T basicAuth(Function<Session, String> username, Function<Session, String> password) {
    return make(wrapped -> wrapped.basicAuth(toTypedGatlingSessionFunction(username), toTypedGatlingSessionFunction(password)));
  }

  public T digestAuth(String username, String password) {
    return make(wrapped -> wrapped.digestAuth(toStringExpression(username), toStringExpression(password)));
  }

  public T digestAuth(String username, Function<Session, String> password) {
    return make(wrapped -> wrapped.digestAuth(toStringExpression(username), toTypedGatlingSessionFunction(password)));
  }

  public T digestAuth(Function<Session, String> username, String password) {
    return make(wrapped -> wrapped.digestAuth(toTypedGatlingSessionFunction(username), toStringExpression(password)));
  }

  public T digestAuth(Function<Session, String> username, Function<Session, String> password) {
    return make(wrapped -> wrapped.digestAuth(toTypedGatlingSessionFunction(username), toTypedGatlingSessionFunction(password)));
  }

  public T virtualHost(String virtualHost) {
    return make(wrapped -> (wrapped.virtualHost(toStringExpression(virtualHost))));
  }

  public T virtualHost(Function<Session, String> virtualHost) {
    return make(wrapped -> wrapped.virtualHost(toTypedGatlingSessionFunction(virtualHost)));
  }

  public T disableUrlEncoding() {
    return make(wrapped -> wrapped.disableUrlEncoding());
  }

  public T proxy(Proxy proxy) {
    return make(wrapped -> wrapped.proxy(proxy.asScala()));
  }

  public T sign(SignatureCalculator calculator) {
    return make(wrapped -> wrapped.sign(toStaticValueExpression(calculator)));
  }

  public T sign(Function<Session, SignatureCalculator> calculator) {
    return make(wrapped -> wrapped.sign(toTypedGatlingSessionFunction(calculator)));
  }

  public T signWithOAuth1(String consumerKey, String clientSharedSecret, String token, String tokenSecret) {
    return make(wrapped -> wrapped.signWithOAuth1(toStringExpression(consumerKey), toStringExpression(clientSharedSecret), toStringExpression(token), toStringExpression(tokenSecret)));
  }

  public T signWithOAuth1(Function<Session, String> consumerKey, Function<Session, String> clientSharedSecret, Function<Session, String> token, Function<Session, String> tokenSecret) {
    return make(wrapped -> wrapped.signWithOAuth1(toTypedGatlingSessionFunction(consumerKey), toTypedGatlingSessionFunction(clientSharedSecret), toTypedGatlingSessionFunction(token), toTypedGatlingSessionFunction(tokenSecret)));
  }
}
