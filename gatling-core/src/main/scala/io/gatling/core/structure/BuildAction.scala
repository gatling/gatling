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

package io.gatling.core.structure

import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.pause.PauseType
import io.gatling.core.protocol.ProtocolComponentsRegistry

trait BuildAction { this: Execs[_] =>

  private[gatling] def build(ctx: ScenarioContext, chainNext: Action): Action =
    actionBuilders.foldLeft(chainNext) { (next, actionBuilder) =>
      actionBuilder.build(ctx, next)
    }
}

final class ScenarioContext(
    val coreComponents: CoreComponents,
    val protocolComponentsRegistry: ProtocolComponentsRegistry,
    val pauseType: PauseType,
    val throttled: Boolean
)
