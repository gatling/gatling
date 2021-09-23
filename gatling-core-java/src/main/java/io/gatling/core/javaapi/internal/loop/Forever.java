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

import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.internal.StructureBuilder;

import java.util.UUID;
import java.util.function.Function;

public interface Forever<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default Loop<T> forever() {
    return forever(null);
  }

  default Loop<T> forever(String counterName) {
    return forever(counterName, false);
  }

  default Loop<T> forever(boolean exitASAP) {
    return forever(null, exitASAP);
  }

  default Loop<T> forever(String counterName, boolean exitASAP) {
    return new Loop<>(this, counterName, exitASAP);
  }

  final class Loop<T extends StructureBuilder<T, ?>> {
    private final Forever<T, ?> context;
    private final String counterName;
    private final boolean exitASAP;

    Loop(Forever<T, ?> context, String counterName, boolean exitASAP) {
      this.context = context;
      this.counterName = counterName == null ? UUID.randomUUID().toString() : counterName;
      this.exitASAP = exitASAP;
    }

    public T loop(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.forever(counterName, exitASAP, chain.wrapped));
    }
  }
}
