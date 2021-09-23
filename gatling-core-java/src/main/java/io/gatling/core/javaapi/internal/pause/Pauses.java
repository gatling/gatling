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

import io.gatling.core.javaapi.PauseType;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface Pauses<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default T pause(long duration) {
    return pause(Duration.ofSeconds(duration));
  }

  default T pause(long duration, PauseType pauseType) {
    return pause(Duration.ofSeconds(duration), pauseType);
  }

  default T pause(Duration duration) {
    return make(wrapped -> wrapped.pause(toScalaDuration(duration)));
  }

  default T pause(Duration duration, PauseType pauseType) {
    return make(wrapped -> wrapped.pause(toScalaDuration(duration), pauseType.asScala()));
  }

  default T pause(String duration) {
    return make(wrapped -> wrapped.pause(duration));
  }

  default T pause(String duration, PauseType pauseType) {
    return make(wrapped -> wrapped.pause(duration, pauseType.asScala()));
  }

  default T pause(Function<Session, Duration> f) {
    return make(wrapped -> wrapped.pause(toGatlingSessionFunctionDuration(f)));
  }

  default T pause(Function<Session, Duration> f, PauseType pauseType) {
    return make(wrapped -> wrapped.pause(toGatlingSessionFunctionDuration(f), pauseType.asScala()));
  }

  default T pause(long min, long max) {
    return pause(Duration.ofSeconds(min), Duration.ofSeconds(max));
  }

  default T pause(long min, long max, PauseType pauseType) {
    return pause(Duration.ofSeconds(min), Duration.ofSeconds(max), pauseType);
  }

  default T pause(Duration min, Duration max) {
    return make(wrapped -> wrapped.pause(toScalaDuration(min), toScalaDuration(max)));
  }

  default T pause(Duration min, Duration max, PauseType pauseType) {
    return make(wrapped -> wrapped.pause(toScalaDuration(min), toScalaDuration(max), pauseType.asScala()));
  }

  default T pause(String min, String max) {
    return make(wrapped -> wrapped.pause(min, max, TimeUnit.SECONDS));
  }

  default T pause(String min, String max, PauseType pauseType) {
    return make(wrapped -> wrapped.pause(min, max, pauseType.asScala()));
  }

  default T pause(Function<Session, Duration> min, Function<Session, Duration> max) {
    return make(wrapped -> wrapped.pause(toGatlingSessionFunctionDuration(min), toGatlingSessionFunctionDuration(max)));
  }

  default T pause(Function<Session, Duration> min, Function<Session, Duration> max, PauseType pauseType) {
    return make(wrapped -> wrapped.pause(toGatlingSessionFunctionDuration(min), toGatlingSessionFunctionDuration(max), pauseType.asScala()));
  }
}
