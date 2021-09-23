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

package io.gatling.core.javaapi.internal.condition;

import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.internal.StructureBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toScalaSeq;

public interface RoundRobinSwitch<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default T roundRobinSwitch(ChainBuilder... possibilities) {
    return make(wrapped -> wrapped.roundRobinSwitch(toScalaSeq(Arrays.stream(possibilities).map(possibility -> possibility.wrapped).collect(Collectors.toList()))));
  }

  default T roundRobinSwitch(List<ChainBuilder> possibilities) {
    return make(wrapped -> wrapped.roundRobinSwitch(toScalaSeq(possibilities.stream().map(possibility -> possibility.wrapped).collect(Collectors.toList()))));
  }
}
