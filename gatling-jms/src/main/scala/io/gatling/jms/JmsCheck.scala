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

package io.gatling.jms

import javax.jms.Message

import io.gatling.commons.validation.Validation
import io.gatling.core.check.{ Check, CheckResult, ConditionalCheck }
import io.gatling.core.session.{ Expression, Session }

final case class JmsCheck(wrapped: Check[Message], condition: Option[(Message, Session) => Validation[Boolean]]) extends ConditionalCheck[Message] {

  private[jms] def withUntypedCondition(condition: Expression[Boolean]): JmsCheck = {
    val typedCondition = (_: Message, session: Session) => condition(session)
    copy(condition = Some(typedCondition))
  }
}
