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

package io.gatling.javaapi.core.loop;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.loop.ScalaDoWhile;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Methods for defining "doWhile" loops. Similar to {@link AsLongAs} except the condition is
 * evaluated at the end of the loop.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface DoWhile<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  // Gatling EL condition
  /**
   * Define a loop that will iterate as long as the condition holds true. The condition is evaluated
   * at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> doWhile(@Nonnull String condition) {
    return doWhile(condition, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate as long as the condition holds true. The condition is evaluated
   * at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> doWhile(@Nonnull String condition, @Nonnull String counterName) {
    return new On<>(ScalaDoWhile.apply(this, condition, counterName));
  }

  // Function condition
  /**
   * Define a loop that will iterate as long as the condition holds true. The condition is evaluated
   * at the end of the loop. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> doWhile(@Nonnull Function<Session, Boolean> condition) {
    return doWhile(condition, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate as long as the condition holds true. The condition is evaluated
   * at the end of the loop. The condition is evaluated at the end of the loop.
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> doWhile(
      @Nonnull Function<Session, Boolean> condition, @Nonnull String counterName) {
    return new On<>(ScalaDoWhile.apply(this, condition, counterName));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaDoWhile.Loop<T, ?> wrapped;

    On(ScalaDoWhile.Loop<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the loop content
     *
     * @param chain the loop content
     * @return a new {@link StructureBuilder}
     */
    @Nonnull
    public T on(@Nonnull ChainBuilder chain) {
      return wrapped.loop(chain);
    }
  }
}
