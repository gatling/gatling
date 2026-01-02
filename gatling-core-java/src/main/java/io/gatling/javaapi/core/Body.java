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

package io.gatling.javaapi.core;

import io.gatling.commons.validation.Failure;
import io.gatling.commons.validation.Success;
import io.gatling.commons.validation.Validation;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * Java wrapper of a Scala request body. Sealed, must not be extended. Implementations are for
 * internal use only.
 */
public abstract class Body {

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  public abstract io.gatling.core.body.Body asScala();

  private Body() {
    // sealed
  }

  /** Default Body */
  public static final class Default extends Body {
    private final io.gatling.core.body.Body wrapped;

    public Default(io.gatling.core.body.@NonNull Body wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public io.gatling.core.body.Body asScala() {
      return wrapped;
    }
  }

  /** Body that is a Function<Session, String> */
  public static final class WithString extends Body implements Function<Session, String> {

    private final io.gatling.core.body.BodyWithStringExpression wrapped;

    public WithString(io.gatling.core.body.@NonNull BodyWithStringExpression wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public io.gatling.core.body.Body asScala() {
      return wrapped;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String apply(@NonNull Session session) {
      Validation<?> validation = wrapped.apply(session.asScala());
      if (validation instanceof Success) {
        return ((Success<String>) validation).value();
      } else {
        throw new RuntimeException(((Failure) validation).message());
      }
    }
  }

  /** Body that is a Function<Session, byte[]> */
  public static final class WithBytes extends Body implements Function<Session, byte[]> {

    private final io.gatling.core.body.BodyWithBytesExpression wrapped;

    public WithBytes(io.gatling.core.body.@NonNull BodyWithBytesExpression wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public io.gatling.core.body.Body asScala() {
      return wrapped;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] apply(@NonNull Session session) {
      Validation<?> validation = wrapped.apply(session.asScala());
      if (validation instanceof Success) {
        return ((Success<byte[]>) validation).value();
      } else {
        throw new RuntimeException(((Failure) validation).message());
      }
    }
  }
}
