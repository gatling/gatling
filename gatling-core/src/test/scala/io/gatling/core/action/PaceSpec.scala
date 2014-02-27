package io.gatling.core.action

import akka.actor.ActorDSL._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import io.gatling.core.Predef._
import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.duration.Duration
import io.gatling.core.config.Protocols
import io.gatling.core.session.Session

@RunWith(classOf[JUnitRunner])
class PaceSpec extends Specification {
	"pace" should {
		"run actions with a minimum wait time" in new ActorSupport {
			val instance = pace(Duration(3, SECONDS), "paceCounter").build(self, Protocols())

			// Send session, expect response near-instantly
			instance ! Session("TestScenario", "testUser")
			val session1 = expectMsgClass(Duration(1, SECONDS), classOf[Session])

			// Send second session, expect nothing for 7 seconds, then a response
			instance ! session1
			expectNoMsg(Duration(2, SECONDS))
			val session2 = expectMsgClass(Duration(2, SECONDS), classOf[Session])

			// counter must have incremented by 3 seconds
			session2("paceCounter").as[Long] must_== session1("paceCounter").as[Long] + 3000L
		}

		"run actions immediately if the minimum time has expired" in new ActorSupport {
			val instance = pace(Duration(3, SECONDS), "paceCounter").build(self, Protocols())

			// Send session, expect response near-instantly
			instance ! Session("TestScenario", "testUser")
			val session1 = expectMsgClass(Duration(1, SECONDS), classOf[Session])

			// Wait 3 seconds - simulate overrunning action
			Thread.sleep(3000L)

			// Send second session, expect response near-instantly
			instance ! session1
			val session2 = expectMsgClass(Duration(1, SECONDS), classOf[Session])

			// counter must have incremented by 3 seconds
			session2("paceCounter").as[Long] must_== session1("paceCounter").as[Long] + 3000L
		}
	}
}
