package io.gatling.core.structure

import io.gatling.core.action.ActorSupport
import org.specs2.mutable.Specification
import io.gatling.core.Predef._
import io.gatling.core.config.Protocols
import io.gatling.core.session.Session

class ExecsSpec extends Specification {
	"Execs" should {
		"wrap Scenarios in chains, using exec" in new ActorSupport {
			val testScenario = scenario("Test Scenario").exec { session =>
				self ! "Message 2"
				session
			}

			val chainBuilder = exec { session =>
				self ! "Message 1"
				session
			}
				.exec(testScenario)
				.exec { session =>
					self ! "Message 3"
					session
				}

			val chain = chainBuilder.build(self, Protocols())
			val session = Session("TestScenario", "testUser")
			chain ! session
			/*
			 * We're cheating slightly by assuming messages will be delivered
			 * in order (technically, Akka doesn't guarantee transitive
			 * ordering, although within the same JVM ordering is generally
			 * transitive) as it gives us more informative error messages.
			 */
			expectMsg("Message 1")
			expectMsg("Message 2")
			expectMsg("Message 3")
			expectMsg(session)
			expectNoMsg
		}
	}
}
