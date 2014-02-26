package io.gatling.core.action.builder

import akka.actor.ActorDSL.actor
import io.gatling.core.session.Expression
import akka.actor.ActorRef
import io.gatling.core.config.Protocols
import io.gatling.core.action.Pace
import scala.concurrent.duration.Duration

/**
 * Builder for the Pace action
 *
 * @param interval the interval between executions
 * @param counter the name of the counter to use to track whether the action can run. Typically random,
 *                but can be specified manually, if multiple paces are to co-operate
 */
class PaceBuilder(interval: Expression[Duration], counter: String) extends ActionBuilder {
	override private[gatling] def build(next: ActorRef, protocols: Protocols): ActorRef = {
		actor(new Pace(interval, counter, next))
	}
}
