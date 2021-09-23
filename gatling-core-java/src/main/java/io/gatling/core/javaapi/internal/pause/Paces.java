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

package io.gatling.core.javaapi.internal.pause;

import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;

import java.time.Duration;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface Paces<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /////////////// long duration
  default T pace(long duration) {
    return pace(Duration.ofSeconds(duration));
  }

  default T pace(long duration, String counterName) {
    return pace(Duration.ofSeconds(duration), counterName);
  }

  /////////////// Duration duration
  default T pace(Duration duration) {
    return make(wrapped -> wrapped.pace(toScalaDuration(duration)));
  }

  default T pace(Duration duration, String counterName) {
    return make(wrapped -> wrapped.pace(toScalaDuration(duration), counterName));
  }

  /////////////// Gatling EL duration
  default T pace(String duration) {
    return make(wrapped -> wrapped.pace(toDurationExpression(duration)));
  }

  default T pace(String duration, String counterName) {
    return make(wrapped -> wrapped.pace(toDurationExpression(duration), counterName));
  }

  /////////////// Function duration
  default T pace(Function<Session, Duration> duration) {
    return make(wrapped -> wrapped.pace(toGatlingSessionFunctionDuration(duration)));
  }

  default T pace(Function<Session, Duration> duration, String counterName) {
    return make(wrapped -> wrapped.pace(toGatlingSessionFunctionDuration(duration), counterName));
  }

  /////////////// long min max
  default T pace(long min, long max) {
    return make(wrapped -> wrapped.pace(toScalaDuration(Duration.ofSeconds(min)), toScalaDuration(Duration.ofSeconds(max))));
  }

  default T pace(long min, long max, String counterName) {
    return make(wrapped -> wrapped.pace(toScalaDuration(Duration.ofSeconds(min)), toScalaDuration(Duration.ofSeconds(max)), counterName));
  }

  /////////////// Duration min max
  default T pace(Duration min, Duration max) {
    return make(wrapped -> wrapped.pace(toScalaDuration(min), toScalaDuration(max)));
  }

  default T pace(Duration min, Duration max, String counterName) {
    return make(wrapped -> wrapped.pace(toScalaDuration(min), toScalaDuration(max), counterName));
  }

  /////////////// Gatling EL min max
  default T pace(String min, String max, String counterName) {
    return make(wrapped -> wrapped.pace(toDurationExpression(min), toDurationExpression(max), counterName));
  }

  /////////////// Function min max
  default T pace(Function<Session, Duration> min, Function<Session, Duration> max) {
    return make(wrapped -> wrapped.pace(toGatlingSessionFunctionDuration(min), toGatlingSessionFunctionDuration(max)));
  }

  default T pace(Function<Session, Duration> min, Function<Session, Duration> max, String counterName) {
    return make(wrapped -> wrapped.pace(toGatlingSessionFunctionDuration(min), toGatlingSessionFunctionDuration(max), counterName));
  }
}
