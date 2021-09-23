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

package io.gatling.core.javaapi.internal.error;

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;
import scala.Function1;

import java.util.UUID;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface Errors<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default T exitBlockOnFail(ChainBuilder chain) {
    return make(wrapped -> wrapped.exitBlockOnFail(chain.wrapped));
  }

  default TryMax<T> tryMax(int times) {
    return tryMax(unused -> times);
  }

  default TryMax<T> tryMax(int times, String counterName) {
    return tryMax(unused -> times, counterName);
  }

  default TryMax<T> tryMax(String times) {
    return tryMax(times, null);
  }

  default TryMax<T> tryMax(String times, String counterName) {
    return new TryMax<>(this, toIntExpression(times), counterName);
  }

  default TryMax<T> tryMax(Function<Session, Integer> times) {
    return tryMax(times, null);
  }

  default TryMax<T> tryMax(Function<Session, Integer> times, String counterName) {
    return new TryMax<>(this, toUntypedGatlingSessionFunction(times), counterName);
  }


  final class TryMax<T extends StructureBuilder<T, ?>> {
    private final Errors<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<Object>> times;
    private final String counterName;

    TryMax(Errors<T, ?> context, Function1<io.gatling.core.session.Session, Validation<Object>> times, String counterName) {
      this.context = context;
      this.times = times;
      this.counterName = counterName == null ? UUID.randomUUID().toString() : counterName;
    }

    public T trying(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.tryMax(times, counterName, chain.wrapped));
    }
  }

  default T exitHereIf(String condition) {
    return make(wrapped -> wrapped.exitHereIf(toBooleanExpression(condition)));
  }

  default T exitHereIf(Function<Session, Boolean> condition) {
    return make(wrapped -> wrapped.exitHereIf(toUntypedGatlingSessionFunction(condition)));
  }

  default T exitHere() {
    return make(wrapped -> wrapped.exitHere());
  }

  default T exitHereIfFailed() {
    return make(wrapped -> wrapped.exitHereIfFailed());
  }
}
