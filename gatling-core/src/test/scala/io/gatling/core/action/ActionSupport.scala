package io.gatling.core.action

import akka.testkit.{ ImplicitSender, TestKit }
import akka.actor.ActorSystem
import org.specs2.mutable._
import io.gatling.core.akka.GatlingActorSystem
import java.util.UUID
import org.specs2.specification.Scope

object ActorSupport {
	def gatlingActorSystem = GatlingActorSystem.synchronized {
		GatlingActorSystem.instanceOpt match {
			case None =>
				GatlingActorSystem.start()
				GatlingActorSystem.instance
			case Some(system) => system
		}
	}
}

class ActorSupport extends TestKit(ActorSupport.gatlingActorSystem) with ImplicitSender with Scope
