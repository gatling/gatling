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

import io.gatling.core.action.builder.{ ActionBuilder, SessionHookBuilder }
import io.gatling.core.session.{ Expression, Session }

private[structure] trait Execs[B] {

  protected def actionBuilders: List[ActionBuilder]
  protected def chain(newActionBuilders: Seq[ActionBuilder]): B

  def exec(sessionFunction: Expression[Session]): B = exec(new SessionHookBuilder(sessionFunction, exitable = true))
  def exec(actionBuilder: ActionBuilder): B = chain(List(actionBuilder))
  def exec(execs: Execs[_]*): B = exec(execs.toIterable)
  def exec(execs: Iterable[Execs[_]]): B = chain(execs.toList.reverse.flatMap(_.actionBuilders))
}
