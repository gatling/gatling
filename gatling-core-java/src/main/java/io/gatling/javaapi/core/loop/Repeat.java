/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import io.gatling.javaapi.core.internal.loop.ScalaRepeat;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Methods for defining "repeat" loops that iterate over a block for a given number of times.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Repeat<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  // int
  /**
   * Define a loop that will iterate for a given number of times.
   *
   * @param times the number of iteration
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> repeat(int times) {
    return repeat(unused -> times, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate for a given number of times.
   *
   * @param times the number of iteration
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> repeat(int times, @Nonnull String counterName) {
    return new On<>(ScalaRepeat.apply(this, times, counterName));
  }

  // Gatling EL
  /**
   * Define a loop that will iterate for a given number of times.
   *
   * @param times the number of iteration, expressed as a Gatling Expression Language String that
   *     must evaluate to an {@link Integer}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> repeat(@Nonnull String times) {
    return repeat(times, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate for a given number of times.
   *
   * @param times the number of iteration, expressed as a Gatling Expression Language String that
   *     must evaluate to an {@link Integer}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> repeat(@Nonnull String times, @Nonnull String counterName) {
    return new On<>(ScalaRepeat.apply(this, times, counterName));
  }

  // Function
  /**
   * Define a loop that will iterate for a given number of times.
   *
   * @param times the number of iteration, expressed as a function
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> repeat(@Nonnull Function<Session, Integer> times) {
    return repeat(times, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate for a given number of times.
   *
   * @param times the number of iteration, expressed as a function
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  @Nonnull
  default On<T> repeat(@Nonnull Function<Session, Integer> times, String counterName) {
    return new On<>(ScalaRepeat.apply(this, times, counterName));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaRepeat.Loop<T, ?> wrapped;

    On(ScalaRepeat.Loop<T, ?> wrapped) {
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
