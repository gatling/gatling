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

package io.gatling.javaapi.core.pause;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.StructureBuilder;
import java.util.function.Function;

/**
 * Rendez-vous methods for defining points that a given number of virtual users before proceeding.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface RendezVous<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Make virtual users wait until enough of them reach this point
   *
   * @param users the number of virtual users that must reach this point
   * @return a new StructureBuilder
   */
  @NonNull
  default T rendezVous(int users) {
    return make(wrapped -> wrapped.rendezVous(users));
  }
}
