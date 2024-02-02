/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.exec.Executable;
import io.gatling.javaapi.core.internal.Converters;
import java.util.List;

/**
 * Java wrapper of a Scala ActionBuilder. Builder of an Action in a Gatling scenario.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public interface ActionBuilder extends Executable {

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  io.gatling.core.action.builder.ActionBuilder asScala();

  /**
   * For internal use only
   *
   * @return a ChainBuilder
   */
  @Override
  default ChainBuilder toChainBuilder() {
    return new ChainBuilder(
        io.gatling.core.structure.ChainBuilder.Empty()
            .chain(Converters.toScalaSeq(List.of(asScala()))));
  }
}
