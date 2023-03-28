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

package io.gatling.javaapi.core.group;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.group.ScalaGroups;
import java.util.function.Function;

/**
 * Methods for defining "groups".
 *
 * <p>Groups provide with "cumulated response times" and start-to-end "group duration metrics". When
 * running with Gatling Enterprise, groups also provides with aggregated response times across
 * requests grouped by group.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Groups<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Define a group
   *
   * @param name the name of the group, expressed as a Gatling Expression Language String
   * @return a DSL component for defining the wrapped block
   */
  @NonNull
  default On<T> group(@NonNull String name) {
    return new On<>(ScalaGroups.apply(this, name));
  }

  /**
   * Define a group
   *
   * @param name the name of the group, expressed as a function
   * @return a DSL component for defining the wrapped block
   */
  @NonNull
  default On<T> group(@NonNull Function<Session, String> name) {
    return new On<>(ScalaGroups.apply(this, name));
  }

  /**
   * The DSL component for defining the wrapped block
   *
   * @param <T> the type of {@link StructureBuilder} to attach to and to return
   */
  final class On<T extends StructureBuilder<T, ?>> {
    private final ScalaGroups.Grouping<T, ?> wrapped;

    On(ScalaGroups.Grouping<T, ?> wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the wrapped block
     *
     * @param chain the wrapped block
     * @return a new {@link StructureBuilder}
     */
    @NonNull
    public T on(@NonNull ChainBuilder chain) {
      return wrapped.grouping(chain);
    }
  }
}
