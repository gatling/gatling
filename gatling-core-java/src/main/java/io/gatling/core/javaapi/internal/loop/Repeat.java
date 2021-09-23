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

import java.util.UUID;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface Repeat<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  // int
  default Loop<T> repeat(int times) {
    return repeat(unused -> times, null);
  }

  default Loop<T> repeat(int times, String counterName) {
    return repeat(unused -> times, counterName);
  }

  // Gatling EL
  default Loop<T> repeat(String times) {
    return repeat(times, null);
  }

  default Loop<T> repeat(String times, String counterName) {
    return new Loop<>(this, toIntExpression(times), counterName);
  }

  // Function
  default Loop<T> repeat(Function<Session, Integer> times) {
    return repeat(times, null);
  }

  default Loop<T> repeat(Function<Session, Integer> times, String counterName) {
    return new Loop<>(this, toUntypedGatlingSessionFunction(times), counterName);
  }

  final class Loop<T extends StructureBuilder<T, ?>> {
    private final Repeat<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<Object>> times;
    private final String counterName;

    Loop(Repeat<T, ?> context, Function1<io.gatling.core.session.Session, Validation<Object>> times, String counterName) {
      this.context = context;
      this.times = times;
      this.counterName = counterName == null ? UUID.randomUUID().toString() : counterName;
    }

    public T loop(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.repeat(times, counterName, chain.wrapped));
    }
  }
}
