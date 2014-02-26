package io.gatling.core.action

import io.gatling.core.session.{ Expression, Session }
import akka.actor.ActorRef
import io.gatling.core.util.TimeHelper.nowMillis
import scala.concurrent.duration._

/**
 * Pace provides a means to limit the frequency with which an action is run, by specifying a minimum wait time between
 * iterations.
 *
 * @param intervalExpr a function that decides how long to wait before the next iteration
 * @param counter the name of the counter used to keep track of the run state. Typically this would be random, but
 *                can be set explicitly if needed
 * @param next the next actor in the chain
 */
class Pace(intervalExpr: Expression[Duration], counter: String, val next: ActorRef) extends Interruptable with Failable {
	/**
	 * Pace keeps track of when it can next run using a counter in the session. If this counter does not exist, it will
   * run immediately. On each run, it increments the counter by intervalExpr.
	 *
	 * @param session the session of the virtual user
	 * @return Nothing
	 */
	override def executeOrFail(session: Session) = {
		intervalExpr(session) map { interval =>
			val startTimeOpt = session(counter).asOption[Long]
			val startTime = startTimeOpt.getOrElse(nowMillis)
			val nextStartTime = startTime + interval.toMillis
			val waitTime = startTime - nowMillis
			def doNext = next ! session.set(counter, nextStartTime)
			if (waitTime > 0) {
				scheduler.scheduleOnce(waitTime milliseconds)(doNext)
			} else {
				if (startTimeOpt.isDefined) logger.info(s"Previous run overran by ${-waitTime}ms. Running immediately")
				doNext
			}
		}
	}
}
