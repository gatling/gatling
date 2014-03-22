package io.gatling.core.structure

import io.gatling.core.action.ActorSupport
import org.specs2.mutable.Specification
import io.gatling.core.Predef._
import io.gatling.core.config.Protocols
import io.gatling.core.session.Session

/**
 * Created by hisg085 on 22/03/2014.
 */
class ExecsSpec extends Specification {
	"Execs" should {
		"wrap Scenarios in chains, using exec" in new ActorSupport {
			val testScenario = scenario("Test Scenario").exec { session =>
				self ! "Success"
				session
			}
			val chainBuilder = exec(testScenario)
			val chain = chainBuilder.build(self, Protocols())
			val session = Session("TestScenario", "testUser")
			chain ! session
			expectMsgAllOf("Success", session)
		}
	}
}
