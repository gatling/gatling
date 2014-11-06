package io.gatling.http.check.sse

import io.gatling.core.check.{ Check, CheckResult }
import io.gatling.core.session.Session
import io.gatling.core.validation.Validation
import io.gatling.http.check.ws.Expectation

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
 * @author ctranxuan
 */

case class SseCheck(wrapped: Check[String], blocking: Boolean, timeout: FiniteDuration, expectation: Expectation) extends Check[String] {
  override def check(message: String, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = wrapped.check(message, session)

}
