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
import io.gatling.javaapi.core.exec.Executable;
import io.gatling.javaapi.core.internal.Executables;
import io.gatling.javaapi.core.internal.condition.ScalaRandomSwitchOrElse;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Methods for defining "randomSwitchOrElse" conditional blocks.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface RandomSwitchOrElse<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Execute one of the "choices" randomly based on their respective weight. Weights are expressed
   * in percents so their sum must be <= 100%.
   *
   * @return the DSL component for defining the "else" block
   */
  @NonNull
  default On<T> randomSwitchOrElse() {
    return new On<>(new ScalaRandomSwitchOrElse.Choices<>(this));
  }

  /**
   * The DSL component for defining the "choices"
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaRandomSwitchOrElse.Choices<T, ?> wrapped;

    On(ScalaRandomSwitchOrElse.Choices<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the "choices"
     *
     * @param choices the choices
     * @return the DSL component for defining the "else" block
     */
    @NonNull
    public OrElse<T> on(@NonNull Choice.WithWeight... choices) {
      return on(Arrays.asList(choices));
    }

    /**
     * Define the "choices"
     *
     * @param choices the choices
     * @return the DSL component for defining the "else" block
     */
    @NonNull
    public OrElse<T> on(@NonNull List<Choice.WithWeight> choices) {
      return new OrElse<>(wrapped.choices(choices));
    }
  }

  /**
   * The DSL component for defining the "else" block
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class OrElse<T extends StructureBuilder<T, ?>> {
    private final ScalaRandomSwitchOrElse.OrElse<T, ?> wrapped;

    OrElse(ScalaRandomSwitchOrElse.OrElse<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the chain to be executed when the random number falls into the gap between 100% and
     * the sum of the weights of the choices.
     *
     * @param executable the "then "chain
     * @param executables other chains
     * @return a new {@link StructureBuilder}
     */
    @NonNull
    public T orElse(@NonNull Executable executable, @NonNull Executable... executables) {
      return wrapped.orElse(Executables.toChainBuilder(executable, executables));
    }
  }
}
