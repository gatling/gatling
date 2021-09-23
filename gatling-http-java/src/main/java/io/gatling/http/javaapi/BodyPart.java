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

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public final class BodyPart {

  private final io.gatling.http.request.BodyPart wrapped;

  public BodyPart(io.gatling.http.request.BodyPart wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.http.request.BodyPart asScala() {
    return wrapped;
  }

  public BodyPart contentType(String contentType) {
    return new BodyPart(wrapped.contentType(toStringExpression(contentType)));
  }

  public BodyPart contentType(Function<Session, String> contentType) {
    return new BodyPart(wrapped.contentType(toTypedGatlingSessionFunction(contentType)));
  }

  public BodyPart charset(String charset) {
    return new BodyPart(wrapped.charset((charset)));
  }

  public BodyPart dispositionType(String dispositionType) {
    return new BodyPart(wrapped.dispositionType(toStringExpression(dispositionType)));
  }

  public BodyPart dispositionType(Function<Session, String> dispositionType) {
    return new BodyPart(wrapped.dispositionType(toTypedGatlingSessionFunction(dispositionType)));
  }

  public BodyPart fileName(String fileName) {
    return new BodyPart(wrapped.fileName(toStringExpression(fileName)));
  }

  public BodyPart fileName(Function<Session, String> fileName) {
    return new BodyPart(wrapped.fileName(toTypedGatlingSessionFunction(fileName)));
  }

  public BodyPart contentId(String contentId) {
    return new BodyPart(wrapped.contentId(toStringExpression(contentId)));
  }

  public BodyPart contentId(Function<Session, String> contentId) {
    return new BodyPart(wrapped.contentId(toTypedGatlingSessionFunction(contentId)));
  }

  public BodyPart transferEncoding(String transferEncoding) {
    return new BodyPart(wrapped.transferEncoding(transferEncoding));
  }

  public BodyPart header(String name, String value) {
    return new BodyPart(wrapped.header(toStringExpression(name), toStringExpression(value)));
  }

  public BodyPart header(String name, Function<Session, String> value) {
    return new BodyPart(wrapped.header(toStringExpression(name), toTypedGatlingSessionFunction(value)));
  }

  public BodyPart header(Function<Session, String> name, String value) {
    return new BodyPart(wrapped.header(toTypedGatlingSessionFunction(name), toStringExpression(value)));
  }

  public BodyPart header(Function<Session, String> name, Function<Session, String> value) {
    return new BodyPart(wrapped.header(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(value)));
  }
}
