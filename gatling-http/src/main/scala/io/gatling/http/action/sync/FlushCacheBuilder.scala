/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.sync

import io.gatling.core.action.{ Action, ExitableAction, SessionHook }
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.action.HttpActionBuilder

class FlushCacheBuilder extends HttpActionBuilder with NameGen {

  def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val expression = lookUpHttpComponents(protocolComponentsRegistry).httpCaches.FlushCache
    new SessionHook(expression, genName("flushCache"), coreComponents.statsEngine, next) with ExitableAction
  }
}
