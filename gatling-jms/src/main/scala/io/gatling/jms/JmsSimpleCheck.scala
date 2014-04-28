package io.gatling.jms

import javax.jms.Message
import io.gatling.core.session.Session
import scala.collection.mutable
import io.gatling.core.validation.{ Failure, Validation }
import io.gatling.core.check.Check

case class JmsSimpleCheck(func: Message => Boolean) extends JmsCheck {
  override def check(response: Message, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[(Session) => Session] = {
    func(response) match {
      case true => Check.noopUpdateSuccess
      case _    => Failure("Jms check failed")
    }
  }
}
