package io.gatling.jms.action

import org.specs2.mutable.Specification
import io.gatling.jms._
import io.gatling.core.test.ActorSupport
import io.gatling.core.session.Session
import org.specs2.time.NoTimeConversions
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.Predef._
import io.gatling.jms.Predef._
import io.gatling.jms.MessageReceived
import io.gatling.core.result.writer.RequestMessage
import io.gatling.jms.check.JmsSimpleCheck
import io.gatling.jms.MessageSent
import akka.testkit.TestActorRef

class JmsRequestTrackerActorWithMockWriter extends JmsRequestTrackerActor with MockDataWriterClient

class JmsRequestTrackerActorSpec extends Specification with MockMessage with NoTimeConversions {

  "JmsRequestTrackerActor" should {
    val session = Session("mockSession", "mockUserName")
    "pass to next to next actor when matching message is received" in ActorSupport {
      testKit =>
        import testKit._

        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageSent("1", 15, 20, List(), session, testActor, "success")
        tracker ! MessageReceived("1", 30, textMessage("test"))

        expectMsg(session)
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "success", 15, 20, 20, 30, OK, None, List()))
    }

    "pass to next to next actor even if messages are out of sync" in ActorSupport {
      testKit =>
        import testKit._

        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageReceived("1", 30, textMessage("test"))
        tracker ! MessageSent("1", 15, 20, List(), session, testActor, "outofsync")

        expectMsg(session)
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "outofsync", 15, 20, 20, 30, OK, None, List()))
    }

    "pass KO to next actor when check fails" in ActorSupport {
      testKit =>
        import testKit._

        val failedCheck = JmsSimpleCheck(_ => false)
        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageSent("1", 15, 20, List(failedCheck), session, testActor, "failure")
        tracker ! MessageReceived("1", 30, textMessage("test"))

        expectMsg(session)
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "failure", 15, 20, 20, 30, KO, None, List()))
    }

    "pass updated session to next actor if modified by checks" in ActorSupport {
      testKit =>
        import testKit._

        val check: JmsCheck = xpath("/id").saveAs("id")
        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageSent("1", 15, 20, List(check), session, testActor, "updated")
        tracker ! MessageReceived("1", 30, textMessage("<id>5</id>"))

        expectMsg(session.set("id", "5"))
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "updated", 15, 20, 20, 30, OK, None, List()))
    }
  }
}
