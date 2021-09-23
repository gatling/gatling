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

public interface During<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /////////////// long duration
  default Loop<T> during(long duration) {
    return during(Duration.ofSeconds(duration));
  }

  default Loop<T> during(long duration, boolean exitASAP) {
    return during(Duration.ofSeconds(duration), exitASAP);
  }

  default Loop<T> during(long duration, String counterName) {
    return during(Duration.ofSeconds(duration), counterName);
  }

  default Loop<T> during(long duration, String counterName, boolean exitASAP) {
    return during(Duration.ofSeconds(duration), counterName, exitASAP);
  }

  /////////////// Duration duration
  default Loop<T> during(Duration duration) {
    return during(unused -> duration);
  }

  default Loop<T> during(Duration duration, boolean exitASAP) {
    return during(unused -> duration, exitASAP);
  }

  default Loop<T> during(Duration duration, String counterName) {
    return during(unused -> duration, counterName, true);
  }

  default Loop<T> during(Duration duration, String counterName, boolean exitASAP) {
    return during(unused -> duration, counterName, exitASAP);
  }

  /////////////// Gatling EL duration
  default Loop<T> during(String duration) {
    return during(duration, null);
  }

  default Loop<T> during(String duration, boolean exitASAP) {
    return during(duration, null, exitASAP);
  }

  default Loop<T> during(String duration, String counterName) {
    return during(duration, counterName, true);
  }

  default Loop<T> during(String duration, String counterName, boolean exitASAP) {
    return new Loop<>(this, toDurationExpression(duration), counterName, exitASAP);
  }

  /////////////// Function duration
  default Loop<T> during(Function<Session, Duration> duration) {
    return during(duration, null);
  }

  default Loop<T> during(Function<Session, Duration> duration, boolean exitASAP) {
    return during(duration, null, exitASAP);
  }

  default Loop<T> during(Function<Session, Duration> duration, String counterName) {
    return during(duration, counterName, true);
  }

  default Loop<T> during(Function<Session, Duration> duration, String counterName, boolean exitASAP) {
    return new Loop<>(this, toGatlingSessionFunctionDuration(duration), counterName, exitASAP);
  }

  final class Loop<T extends StructureBuilder<T, ?>> {
    private final During<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> duration;
    private final String counterName;
    private final boolean exitASAP;

    Loop(During<T, ?> context, Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> duration, String counterName, boolean exitASAP) {
      this.context = context;
      this.duration = duration;
      this.counterName = counterName == null ? UUID.randomUUID().toString() : counterName;
      this.exitASAP = exitASAP;
    }

    public T loop(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.during(duration, counterName, exitASAP, chain.wrapped));
    }
  }
}
