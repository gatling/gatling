/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms.check

import javax.jms.Message
import scala.collection.mutable
import io.gatling.core.validation.Validation
import io.gatling.core.check.Check
import io.gatling.jms._
import io.gatling.core.session.Session
import io.gatling.core.validation.Failure

case class JmsSimpleCheck(func: Message => Boolean) extends JmsCheck {
  override def check(response: Message, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[(Session) => Session] = {
    func(response) match {
      case true => Check.noopUpdateSuccess
      case _    => Failure("Jms check failed")
    }
  }
}
