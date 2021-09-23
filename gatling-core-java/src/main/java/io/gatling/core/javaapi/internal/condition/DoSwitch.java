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

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.DoSwitchPossibility;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.internal.StructureBuilder;
import scala.Function1;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface DoSwitch<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default Possibilities<T> doSwitch(String value) {
    return new Possibilities(this, toAnyExpression(value));
  }

  default Possibilities<T> doSwitch(Function<Session, Object> value) {
    return new Possibilities(this, toUntypedGatlingSessionFunction(value));
  }

  final class Possibilities<T extends StructureBuilder<T, ?>> {
    private final io.gatling.core.javaapi.internal.condition.DoSwitch<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<Object>> value;

    Possibilities(io.gatling.core.javaapi.internal.condition.DoSwitch<T, ?> context, Function1<io.gatling.core.session.Session, Validation<Object>> value) {
      this.context = context;
      this.value = value;
    }

    public T possibilities(DoSwitchPossibility... possibilities) {
      return context.make(wrapped -> wrapped.doSwitch(value, DoSwitchPossibility.asScala(Arrays.stream(possibilities))));
    }

    public T possibilities(List<DoSwitchPossibility> possibilities) {
      return context.make(wrapped -> wrapped.doSwitch(value, DoSwitchPossibility.asScala(possibilities.stream())));
    }
  }
}
