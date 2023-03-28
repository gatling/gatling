/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core.condition;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.Choice;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.condition.ScalaRandomSwitch;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Methods for defining "randomSwitch" conditional blocks.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface RandomSwitch<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Execute one of the "choices" randomly based on their respective weight. Weights are expressed
   * in percents so their sum must be <= 100%.
   *
   * @return a DSL component for defining the "choices"
   */
  @NonNull
  default On<T> randomSwitch() {
    return new On<>(new ScalaRandomSwitch<>(this));
  }

  /**
   * The DSL component for defining the "choices"
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaRandomSwitch<T, ?> wrapped;

    On(ScalaRandomSwitch<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the "choices"
     *
     * @param choices the choices
     * @return a new {@link StructureBuilder}
     */
    @NonNull
    public T on(@NonNull Choice.WithWeight... choices) {
      return on(Arrays.asList(choices));
    }

    /**
     * Define the "choices"
     *
     * @param choices the choices
     * @return a new {@link StructureBuilder}
     */
    @NonNull
    public T on(@NonNull List<Choice.WithWeight> choices) {
      return wrapped.choices(choices);
    }
  }
}
