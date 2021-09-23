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

import io.gatling.core.javaapi.RandomSwitchPossibility;
import io.gatling.core.javaapi.internal.StructureBuilder;
import io.gatling.core.structure.ChainBuilder;
import scala.Tuple2;
import scala.collection.immutable.Seq;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public interface RandomSwitchOrElse<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default OrElse<T> randomSwitchOrElse(RandomSwitchPossibility... possibilities) {
    return new OrElse<>(this, RandomSwitchPossibility.asScala(Arrays.stream(possibilities)));
  }

  default OrElse<T> randomSwitchOrElse(List<RandomSwitchPossibility> possibilities) {
    return new OrElse<>(this, RandomSwitchPossibility.asScala(possibilities.stream()));
  }

  final class OrElse<T extends StructureBuilder<T, ?>> {
    private final RandomSwitchOrElse<T, ?> context;
    private final Seq<Tuple2<Object, ChainBuilder>> possibilities;

    OrElse(RandomSwitchOrElse<T, ?> context, Seq<Tuple2<Object, io.gatling.core.structure.ChainBuilder>> possibilities) {
      this.context = context;
      this.possibilities = possibilities;
    }

    public T orElse(io.gatling.core.javaapi.ChainBuilder orElseChain) {
      return context.make(wrapped -> wrapped.randomSwitchOrElse(possibilities, orElseChain.wrapped));
    }
  }
}
