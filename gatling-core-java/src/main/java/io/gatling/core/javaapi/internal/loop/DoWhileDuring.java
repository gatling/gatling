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

package io.gatling.core.javaapi.internal.loop;

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;
import scala.Function1;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface DoWhileDuring<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  // Gatling EL condition
  default Loop<T> doWhileDuring(String condition, String duration) {
    return doWhileDuring(condition, duration, null);
  }

  default Loop<T> doWhileDuring(String condition, String duration, String counterName) {
    return doWhileDuring(condition, duration, counterName, false);
  }

  default Loop<T> doWhileDuring(String condition, String duration, boolean exitASAP) {
    return doWhileDuring(condition, duration, null, exitASAP);
  }

  default Loop<T> doWhileDuring(String condition, String duration, String counterName, boolean exitASAP) {
    return new Loop<>(this, toBooleanExpression(condition), toDurationExpression(duration), counterName, exitASAP);
  }

  // Function condition
  default Loop<T> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration) {
    return doWhileDuring(condition, duration, null);
  }

  default Loop<T> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, String counterName) {
    return doWhileDuring(condition, duration, counterName, false);
  }

  default Loop<T> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, boolean exitASAP) {
    return doWhileDuring(condition, duration, null, exitASAP);
  }

  default Loop<T> doWhileDuring(Function<Session, Boolean> condition, Function<Session, Duration> duration, String counterName, boolean exitASAP) {
    return new Loop<>(this, toUntypedGatlingSessionFunction(condition), toGatlingSessionFunctionDuration(duration), counterName, exitASAP);
  }

  final class Loop<T extends StructureBuilder<T, ?>> {
    private final DoWhileDuring<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<Object>> condition;
    private final Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> duration;
    private final String counterName;
    private final boolean exitASAP;

    Loop(DoWhileDuring<T, ?> context, Function1<io.gatling.core.session.Session, Validation<Object>> condition, Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> duration, String counterName, boolean exitASAP) {
      this.context = context;
      this.condition = condition;
      this.duration = duration;
      this.counterName = counterName == null ? UUID.randomUUID().toString() : counterName;
      this.exitASAP = exitASAP;
    }

    public T loop(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.doWhileDuring(condition, duration, counterName, exitASAP, chain.wrapped));
    }
  }
}
