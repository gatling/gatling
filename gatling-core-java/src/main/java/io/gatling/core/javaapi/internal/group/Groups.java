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

package io.gatling.core.javaapi.internal.group;

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;
import scala.Function1;

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toTypedGatlingSessionFunction;

public interface Groups<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default Grouping<T> group(String name) {
    return group(unused -> name);
  }

  default Grouping<T> group(Function<Session, String> name) {
    return new Grouping<>(this, toTypedGatlingSessionFunction(name));
  }

  final class Grouping<T extends StructureBuilder<T, ?>> {
    private final Groups<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<String>> name;

    Grouping(Groups<T, ?> context, Function1<io.gatling.core.session.Session, Validation<String>> name) {
      this.context = context;
      this.name = name;
    }

    public T grouping(ChainBuilder chain) {
      return context.make(wrapped -> wrapped.group(name, chain.wrapped));
    }
  }
}
