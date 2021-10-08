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

package io.gatling.javaapi.core;

import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Java wrapper of a Scala ChainBuilder. Builder of a detached chain of Actions that can be attached
 * to a Gatling scenario.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class ChainBuilder
    extends StructureBuilder<ChainBuilder, io.gatling.core.structure.ChainBuilder> {

  public static final ChainBuilder EMPTY =
      new ChainBuilder(io.gatling.core.structure.ChainBuilder.Empty());

  ChainBuilder(io.gatling.core.structure.ChainBuilder wrapped) {
    super(wrapped);
  }

  @Override
  @Nonnull
  public ChainBuilder make(
      @Nonnull
          Function<io.gatling.core.structure.ChainBuilder, io.gatling.core.structure.ChainBuilder>
              f) {
    return new ChainBuilder(f.apply(wrapped));
  }
}
