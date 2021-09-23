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
import io.gatling.core.Predef$;
import io.gatling.core.action.Action;
import io.gatling.core.javaapi.Body;
import io.gatling.core.javaapi.CheckBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.http.request.builder.HttpRequestBuilder;
import io.gatling.http.response.Response;
import scala.Function1;
import scala.Function2;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;
import static io.gatling.http.javaapi.internal.HttpChecks.toScalaChecks;

public final class HttpRequestActionBuilder extends RequestActionBuilder<HttpRequestActionBuilder, io.gatling.http.request.builder.HttpRequestBuilder> {

  public HttpRequestActionBuilder(io.gatling.http.request.builder.HttpRequestBuilder wrapped) {
    super(wrapped);
  }

  @Override
  protected HttpRequestActionBuilder make(Function<HttpRequestBuilder, HttpRequestBuilder> f) {
    return new HttpRequestActionBuilder(f.apply(wrapped));
  }

  @Override
  public Action build(ScenarioContext ctx, Action next) {
    return wrapped.build(ctx, next);
  }

  public HttpRequestActionBuilder check(CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  public HttpRequestActionBuilder check(List<CheckBuilder> checks) {
    return new HttpRequestActionBuilder(wrapped.check(toScalaChecks(checks)));
  }

  public UntypedCondition checkIf(Function<Session, Boolean> condition) {
    return new UntypedCondition(toUntypedGatlingSessionFunction(condition));
  }

  public UntypedCondition checkIf(String condition) {
    return new UntypedCondition(toBooleanExpression(condition));
  }

  public final class UntypedCondition {
    private final Function1<io.gatling.core.session.Session, Validation<Object>> condition;

    public UntypedCondition(Function1<io.gatling.core.session.Session, Validation<Object>> condition) {
      this.condition = condition;
    }

    public HttpRequestActionBuilder then(CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    public HttpRequestActionBuilder then(List<CheckBuilder> thenChecks) {
      return new HttpRequestActionBuilder(wrapped.checkIf(condition, toScalaChecks(thenChecks)));
    }
  }

  public TypedCondition checkIf(BiFunction<Response, Session, Boolean> condition) {
    return new TypedCondition(toUntypedGatlingSessionFunction(condition));
  }

  public final class TypedCondition {
    private final Function2<Response, io.gatling.core.session.Session, Validation<Object>> condition;

    public TypedCondition(Function2<Response, io.gatling.core.session.Session, Validation<Object>> condition) {
      this.condition = condition;
    }

    public HttpRequestActionBuilder then(CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    public HttpRequestActionBuilder then(List<CheckBuilder> thenChecks) {
      return new HttpRequestActionBuilder(wrapped.checkIf(condition, toScalaChecks(thenChecks)));
    }
  }

  public HttpRequestActionBuilder ignoreProtocolChecks() {
    return make(wrapped -> wrapped.ignoreProtocolChecks());
  }

  public HttpRequestActionBuilder silent() {
    return make(wrapped -> wrapped.silent());
  }

  public HttpRequestActionBuilder notSilent() {
    return make(wrapped -> wrapped.notSilent());
  }

  public HttpRequestActionBuilder disableFollowRedirect() {
    return make(wrapped -> wrapped.disableFollowRedirect());
  }

  public HttpRequestActionBuilder transformResponse(BiFunction<Response, Session, Response> f) {
    return make(wrapped -> wrapped.transformResponse(toTypedGatlingSessionFunction(f)));
  }

  public HttpRequestActionBuilder body(Body body) {
    return make(wrapped -> wrapped.body(body.asScala()));
  }

  public HttpRequestActionBuilder processRequestBody(Function<Body, ? extends Body> processor) {
    return make(wrapped -> wrapped.processRequestBody(scalaBody -> processor.apply(toJavaBody(scalaBody)).asScala()));
  }

  public HttpRequestActionBuilder bodyPart(BodyPart part) {
    return make(wrapped -> wrapped.bodyPart(part.asScala()));
  }

  public HttpRequestActionBuilder bodyParts(BodyPart... parts) {
    return make(wrapped -> wrapped.bodyParts(toScalaSeq(Arrays.stream(parts).map(BodyPart::asScala).collect(Collectors.toList()))));
  }

  public HttpRequestActionBuilder bodyParts(List<BodyPart> parts) {
    return make(wrapped -> wrapped.bodyParts(toScalaSeq(parts.stream().map(BodyPart::asScala).collect(Collectors.toList()))));
  }

  public HttpRequestActionBuilder resources(HttpRequestActionBuilder... res) {
    return make(wrapped -> wrapped.resources(toScalaSeq(Arrays.stream(res).map(r -> r.wrapped).collect(Collectors.toList()))));
  }

  public HttpRequestActionBuilder resources(List<HttpRequestActionBuilder> res) {
    return make(wrapped -> wrapped.resources(toScalaSeq(res.stream().map(r -> r.wrapped).collect(Collectors.toList()))));
  }

  public HttpRequestActionBuilder asMultipartForm() {
    return make(wrapped -> wrapped.asMultipartForm());
  }

  public HttpRequestActionBuilder asFormUrlEncoded() {
    return make(wrapped -> wrapped.asFormUrlEncoded());
  }

  public HttpRequestActionBuilder formParam(String key, String value) {
    return make(wrapped -> wrapped.formParam(toStringExpression(key), toAnyExpression(value)));
  }
  public HttpRequestActionBuilder formParam(Function<Session, String> key, String value) {
    return make(wrapped -> wrapped.formParam(toTypedGatlingSessionFunction(key), toAnyExpression(value)));
  }
  public HttpRequestActionBuilder formParam(String key, Object value) {
    return make(wrapped -> wrapped.formParam(toStringExpression(key), toStaticValueExpression(value)));
  }
  public HttpRequestActionBuilder formParam(Function<Session, String> key, Object value) {
    return make(wrapped -> wrapped.formParam(toTypedGatlingSessionFunction(key), toStaticValueExpression(value)));
  }
  public HttpRequestActionBuilder formParam(String key, Function<Session, Object> value) {
    return make(wrapped -> wrapped.formParam(toStringExpression(key), toTypedGatlingSessionFunction(value)));
  }
  public HttpRequestActionBuilder formParam(Function<Session, String> key, Function<Session, Object> value) {
    return make(wrapped -> wrapped.formParam(toTypedGatlingSessionFunction(key), toTypedGatlingSessionFunction(value)));
  }
  
  public HttpRequestActionBuilder multivaluedFormParam(String key, List<Object> values) {
    return make(wrapped -> wrapped.multivaluedFormParam(toStringExpression(key), toStaticValueExpression(toScalaSeq(values))));
  }
  public HttpRequestActionBuilder multivaluedFormParam(Function<Session, String> key, List<Object> values) {
    return make(wrapped -> wrapped.multivaluedFormParam(toTypedGatlingSessionFunction(key), toStaticValueExpression(toScalaSeq(values))));
  }
  public HttpRequestActionBuilder multivaluedFormParam(String key, Function<Session, List<Object>> values) {
    return make(wrapped -> wrapped.multivaluedFormParam(toStringExpression(key), toGatlingSessionFunctionImmutableSeq(values)));
  }
  public HttpRequestActionBuilder multivaluedFormParam(Function<Session, String> key, Function<Session, List<Object>> values) {
    return make(wrapped -> wrapped.multivaluedFormParam(toTypedGatlingSessionFunction(key), toGatlingSessionFunctionImmutableSeq(values)));
  }
  public HttpRequestActionBuilder formParamSeq(List<Map.Entry<String, Object>> seq) {
    return make(wrapped -> wrapped.formParamSeq(toScalaTuple2Seq(seq)));
  }
  public HttpRequestActionBuilder formParamSeq(Function<Session, List<Map.Entry<String, Object>>> seq) {
    return make(wrapped -> wrapped.formParamSeq(toGatlingSessionFunctionTuple2Seq(seq)));
  }

  public HttpRequestActionBuilder formParamMap(Map<String, Object> map) {
    return make(wrapped -> wrapped.formParamMap(toScalaMap(map)));
  }

  public HttpRequestActionBuilder formParamMap(Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.formParamMap(toGatlingSessionFunctionImmutableMap(map)));
  }

  public HttpRequestActionBuilder form(String form) {
    return make(wrapped -> wrapped.formParamMap(toMapExpression(form)));
  }

  public HttpRequestActionBuilder form(Function<Session, Map<String, Object>> map) {
    return make(wrapped -> wrapped.form(toGatlingSessionFunctionImmutableMap(map)));
  }

  public HttpRequestActionBuilder formUpload(String name, String filePath) {
    return make(wrapped -> wrapped.formUpload(toStringExpression(name), toStringExpression(filePath), Predef$.MODULE$.rawFileBodies()));
  }
  public HttpRequestActionBuilder formUpload(Function<Session, String> name, String filePath) {
    return make(wrapped -> wrapped.formUpload(toTypedGatlingSessionFunction(name), toStringExpression(filePath), Predef$.MODULE$.rawFileBodies()));
  }
  public HttpRequestActionBuilder formUpload(String name, Function<Session, String> filePath) {
    return make(wrapped -> wrapped.formUpload(toStringExpression(name), toTypedGatlingSessionFunction(filePath), Predef$.MODULE$.rawFileBodies()));
  }
  public HttpRequestActionBuilder formUpload(Function<Session, String> name, Function<Session, String> filePath) {
    return make(wrapped -> wrapped.formUpload(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(filePath), Predef$.MODULE$.rawFileBodies()));
  }

  public HttpRequestActionBuilder requestTimeout(int timeout) {
    return requestTimeout(Duration.ofSeconds(timeout));
  }

  public HttpRequestActionBuilder requestTimeout(Duration timeout) {
    return make(wrapped -> wrapped.requestTimeout(toScalaDuration(timeout)));
  }
}
