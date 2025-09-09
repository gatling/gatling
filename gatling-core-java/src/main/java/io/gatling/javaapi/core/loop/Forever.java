/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.exec.Executable;
import io.gatling.javaapi.core.internal.Executables;
import io.gatling.javaapi.core.internal.loop.ScalaForever;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * Methods for defining "forever" loops.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Forever<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Define a loop that will iterate forever.
   *
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> forever() {
    return forever(UUID.randomUUID().toString());
  }

  /**
   * Define a loop that will iterate forever.
   *
   * @param counterName the name of the loop counter, as stored in the {@link Session}
   * @return a DSL component for defining the loop content
   */
  default @NonNull On<T> forever(@NonNull String counterName) {
    return new On<>(ScalaForever.apply(this, counterName));
  }

  /**
   * A DSL component for defining the loop content
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaForever.Loop<T, ?> wrapped;

    On(ScalaForever.Loop<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the loop content
     *
     * @param executable the loop content
     * @param executables other chains
     * @return a new {@link StructureBuilder}
     */
    public @NonNull T on(@NonNull Executable executable, @NonNull Executable... executables) {
      return wrapped.loop(Executables.toChainBuilder(executable, executables));
    }
  }
}
