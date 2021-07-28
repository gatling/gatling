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

package io.gatling.core.javaapi.loop;

import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.javaapi.StructureBuilder;
import io.gatling.core.javaapi.internal.loop.ScalaForEach;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Methods for defining "foreach" loops that iterate over a list of values.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface ForEach<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Define a loop that will iterate over a list of values.
   *
   * @param seq the static list of values to iterate over
   * @param attributeName the key to store the current element in the {@link Session}
   * @return a DSL component to define the loop content
   */
  @Nonnull
  default Loop<T> foreach(@Nonnull List<?> seq, String attributeName) {
    return foreach(seq, attributeName, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate over a list of values.
   *
   * @param seq the static list of values to iterate over
   * @param attributeName the key to store the current element in the {@link Session}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component to define the loop content
   */
  @Nonnull
  default Loop<T> foreach(@Nonnull List<?> seq, String attributeName, @Nonnull String counterName) {
    return foreach(unused -> seq, attributeName, counterName);
  }

  /**
   * Define a loop that will iterate over a list of values.
   *
   * @param seq the list of values to iterate over, expressed as a Gatling Expression Language
   *     String, must evaluate to a {@link List}
   * @param attributeName the key to store the current element in the {@link Session}
   * @return a DSL component to define the loop content
   */
  @Nonnull
  default Loop<T> foreach(@Nonnull String seq, String attributeName) {
    return foreach(seq, attributeName, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate over a list of values.
   *
   * @param seq the list of values to iterate over, expressed as a Gatling Expression Language
   *     String, must evaluate to a {@link List}
   * @param attributeName the key to store the current element in the {@link Session}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component to define the loop content
   */
  @Nonnull
  default Loop<T> foreach(@Nonnull String seq, String attributeName, @Nonnull String counterName) {
    return new Loop<>(ScalaForEach.apply(this, seq, attributeName, counterName));
  }

  /**
   * Define a loop that will iterate over a list of values.
   *
   * @param seq the list of values to iterate over, expressed as a function
   * @param attributeName the key to store the current element in the {@link Session}
   * @return a DSL component to define the loop content
   */
  @Nonnull
  default Loop<T> foreach(@Nonnull Function<Session, List<?>> seq, @Nonnull String attributeName) {
    return foreach(seq, attributeName, UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate over a list of values.
   *
   * @param seq the list of values to iterate over, expressed as a function
   * @param attributeName the key to store the current element in the {@link Session}
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component to define the loop content
   */
  @Nonnull
  default Loop<T> foreach(
      @Nonnull Function<Session, List<?>> seq,
      @Nonnull String attributeName,
      @Nonnull String counterName) {
    return new Loop<>(ScalaForEach.apply(this, seq, attributeName, counterName));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class Loop<T extends StructureBuilder<T, ?>> {
    private final ScalaForEach.Loop<T, ?> wrapped;

    Loop(ScalaForEach.Loop<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the loop content
     *
     * @param chain the loop content
     * @return a new {@link StructureBuilder}
     */
    @Nonnull
    public T loop(@Nonnull ChainBuilder chain) {
      return wrapped.loop(chain);
    }
  }
}
