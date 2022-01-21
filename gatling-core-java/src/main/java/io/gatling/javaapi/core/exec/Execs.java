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

package io.gatling.javaapi.core.exec;

import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.exec.ScalaExecs;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Execs methods for defining actions.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one, eg
 *
 * <pre>{@code
 * ChainBuilder chain1 = ???
 * // chain2 is different from chain1
 * // chain1 isn't modified and doesn't have this new "exec"
 * ChainBuilder chain2 = chain1.exec(???);
 * }</pre>
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Execs<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Attach a new action that will execute a function. Important: the function must only perform
   * fast in-memory operations. In particular, it mustn't perform any long block I/O operation, or
   * it will hurt Gatling performance badly.
   *
   * <pre>{@code
   * exec(session -> session.set("foo", "bar"))
   * }</pre>
   *
   * @param f the function
   * @return a new StructureBuilder
   */
  @Nonnull
  default T exec(@Nonnull Function<Session, Session> f) {
    return ScalaExecs.apply(this, f);
  }

  /**
   * Attach a new action.
   *
   * <pre>{@code
   * exec(http("name").get("url"))
   * }</pre>
   *
   * @param actionBuilder the Action builder
   * @return a new StructureBuilder
   */
  @Nonnull
  default T exec(@Nonnull ActionBuilder actionBuilder) {
    return make(wrapped -> wrapped.exec(actionBuilder.asScala()));
  }

  /**
   * Attach some {@link StructureBuilder}s, Those can be {@link ChainBuilder}s or a {@link
   * ScenarioBuilder}s. In the case of a {@link ScenarioBuilder}, only the chain of actions is
   * considered, without the scenario name. Chains will be attached sequentially.
   *
   * <pre>{@code
   * ChainBuilder chain1 = ???
   * ChainBuilder chain2 = ???
   * ChainBuilder chain1ThenChain2 = exec(chain1, chain2);
   * }</pre>
   *
   * @param structureBuilders some {@link ChainBuilder}
   * @return a new StructureBuilder
   */
  @Nonnull
  default <WB extends io.gatling.core.structure.StructureBuilder<WB>> T exec(
      @Nonnull StructureBuilder<?, WB>... structureBuilders) {
    return exec(Arrays.asList(structureBuilders));
  }

  /**
   * Attach some {@link StructureBuilder}s. Those can be {@link ChainBuilder}s or a {@link
   * ScenarioBuilder}s. In the case of a {@link ScenarioBuilder}, only the chain of actions is
   * considered, without the scenario name. Chains will be attached sequentially.
   *
   * <pre>{@code
   * ChainBuilder chain1 = ???
   * ChainBuilder chain2 = ???
   * List<ChainBuilder> chains = Arrays.asList(chain1, chain2);
   * ChainBuilder chain1ThenChain2 = exec(chains);
   * }</pre>
   *
   * @param structureBuilders some {@link ChainBuilder}
   * @return a new StructureBuilder
   */
  @Nonnull
  default <WB extends io.gatling.core.structure.StructureBuilder<WB>> T exec(
      @Nonnull List<StructureBuilder<?, WB>> structureBuilders) {
    return ScalaExecs.apply(this, structureBuilders);
  }
}
