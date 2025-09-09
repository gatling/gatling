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

package io.gatling.javaapi.core;

import io.gatling.commons.validation.Validation;
import io.gatling.javaapi.core.internal.Expressions;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import scala.Function1;

public final class DummyBuilder implements ActionBuilder {

  private final io.gatling.core.action.builder.DummyBuilder wrapped;

  public DummyBuilder(
      @NonNull Function1<io.gatling.core.session.Session, Validation<String>> requestName,
      @NonNull
          Function1<io.gatling.core.session.Session, Validation<Object>> responseTimeInMillis) {
    this(
        io.gatling.core.action.builder.DummyBuilder$.MODULE$.apply(
            requestName, responseTimeInMillis));
  }

  private DummyBuilder(io.gatling.core.action.builder.DummyBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Set the successful outcome of the dummy action. If undefined, the outcome is a success.
   *
   * @param newSuccess if the outcome of the dummy action must be a success
   * @return a new DummyBuilder with the success outcome defined
   */
  public DummyBuilder withSuccess(boolean newSuccess) {
    return new DummyBuilder(wrapped.withSuccess(Expressions.toStaticValueExpression(newSuccess)));
  }

  /**
   * Set the successful outcome of the dummy action. If undefined, the outcome is a success.
   *
   * @param newSuccess if the outcome of the dummy action must be a success, as a Gatling EL String
   * @return a new DummyBuilder with the success outcome defined
   */
  public DummyBuilder withSuccess(String newSuccess) {
    return new DummyBuilder(wrapped.withSuccess(Expressions.toBooleanExpression(newSuccess)));
  }

  /**
   * Set the successful outcome of the dummy action. If undefined, the outcome is a success.
   *
   * @param newSuccess if the outcome of the dummy action must be a success, as a function
   * @return a new DummyBuilder with the success outcome defined
   */
  public DummyBuilder withSuccess(Function<Session, Boolean> newSuccess) {
    return new DummyBuilder(
        wrapped.withSuccess(Expressions.javaBooleanFunctionToExpression(newSuccess)));
  }

  /**
   * Modify the Session like an exec(f) block would, as part of this dummy action
   *
   * @param f a function to return an updated Session
   * @return a new DummyBuilder with the Session function defined
   */
  public DummyBuilder withSessionUpdate(Function<Session, Session> f) {
    return new DummyBuilder(
        wrapped.withSessionUpdate(Expressions.javaSessionFunctionToExpression(f)));
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
