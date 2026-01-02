/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import io.gatling.javaapi.core.internal.Executables;
import io.gatling.javaapi.core.internal.exec.ScalaExecs;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

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
  default @NonNull T exec(@NonNull Function<Session, Session> f) {
    return ScalaExecs.apply(this, f);
  }

  /**
   * Attach some {@link ChainBuilder}s. Chains will be attached sequentially.
   *
   * <pre>{@code
   * ChainBuilder chain1 = ???
   * ChainBuilder chain2 = ???
   * ChainBuilder chain1ThenChain2 = exec(chain1, chain2);
   * }</pre>
   *
   * @param executable some {@link ChainBuilder} or {@link ActionBuilder}
   * @param executables other {@link ChainBuilder}s or {@link ActionBuilder}s
   * @return a new StructureBuilder
   */
  default @NonNull T exec(@NonNull Executable executable, @NonNull Executable... executables) {
    return exec(Executables.toChainBuilders(executable, executables));
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
   * @param chainBuilders some {@link ChainBuilder}s
   * @return a new StructureBuilder
   */
  default @NonNull T exec(@NonNull List<ChainBuilder> chainBuilders) {
    return ScalaExecs.apply(this, chainBuilders);
  }
}
