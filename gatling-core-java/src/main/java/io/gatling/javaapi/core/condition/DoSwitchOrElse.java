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

package io.gatling.javaapi.core.condition;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Choice;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.condition.ScalaDoSwitchOrElse;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Methods for defining "doSwitchOrElse" conditional blocks.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface DoSwitchOrElse<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Execute one of the "choices" when the actual value is equal to the possibility's one, otherwise
   * execute the "else" block.
   *
   * @param actual the actual value expressed as a Gatling Expression Language String
   * @return a DSL component for defining the "choices"
   */
  @Nonnull
  default Choices<T> doSwitchOrElse(@Nonnull String actual) {
    return new Choices<>(ScalaDoSwitchOrElse.apply(this, actual));
  }

  /**
   * Execute one of the "choices" when the actual value is equal to the possibility's one, otherwise
   * execute the "else" block.
   *
   * @param actual the actual value expressed as a function
   * @return a DSL component for defining the "choices"
   */
  @Nonnull
  default Choices<T> doSwitchOrElse(@Nonnull Function<Session, Object> actual) {
    return new Choices<>(ScalaDoSwitchOrElse.apply(this, actual));
  }

  /**
   * The DSL component for defining the "choices"
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class Choices<T extends StructureBuilder<T, ?>> {
    private final ScalaDoSwitchOrElse.Then<T, ?> wrapped;

    Choices(ScalaDoSwitchOrElse.Then<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the "choices"
     *
     * @param choices the choices
     * @return a DSL component for defining the "else" block
     */
    @Nonnull
    public OrElse<T> choices(@Nonnull Choice.WithValue... choices) {
      return choices(Arrays.asList(choices));
    }

    /**
     * Define the "choices"
     *
     * @param choices the choices
     * @return a DSL component for defining the "else" block
     */
    @Nonnull
    public OrElse<T> choices(@Nonnull List<Choice.WithValue> choices) {
      return new OrElse<>(wrapped.choices(choices));
    }
  }

  /**
   * The DSL component for defining the "else" block
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class OrElse<T extends StructureBuilder<T, ?>> {
    private final ScalaDoSwitchOrElse.OrElse<T, ?> wrapped;

    OrElse(ScalaDoSwitchOrElse.OrElse<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the "else" block
     *
     * @param orElseChain the chain to execute if the actual value doesn't match any of the choices
     * @return a new {@link StructureBuilder}
     */
    @Nonnull
    public T orElse(@Nonnull ChainBuilder orElseChain) {
      return wrapped.orElse(orElseChain);
    }
  }
}
