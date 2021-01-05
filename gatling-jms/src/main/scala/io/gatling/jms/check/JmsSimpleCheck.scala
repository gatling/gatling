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

package io.gatling.jms.check

import java.util.{ Map => JMap }
import javax.jms.Message

import io.gatling.commons.validation._
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session
import io.gatling.jms._

object JmsSimpleCheck {

  private val JmsSimpleCheckFailure = "JMS check failed".failure
}

final class JmsSimpleCheck(func: Message => Boolean) extends JmsCheck {
  override def check(response: Message, session: Session, preparedCache: JMap[Any, Any]): Validation[CheckResult] =
    if (func(response)) {
      CheckResult.NoopCheckResultSuccess
    } else {
      JmsSimpleCheck.JmsSimpleCheckFailure
    }
}
