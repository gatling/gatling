package io.gatling.jms.action

import org.specs2.mutable.Specification
import io.gatling.jms._
import io.gatling.core.test.ActorSupport
import io.gatling.core.session.Session
import org.specs2.time.NoTimeConversions
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.Predef._
import io.gatling.jms.Predef._
import akka.testkit.TestActorRef
import io.gatling.jms.MessageReceived
import io.gatling.core.result.writer.RequestMessage
import io.gatling.jms.check.JmsSimpleCheck
import io.gatling.jms.MessageSent

class JmsRequestTrackerActorWithMockWriter extends JmsRequestTrackerActor with MockDataWriterClient

class JmsRequestTrackerActorSpec extends Specification with MockMessage with NoTimeConversions {

  def ignoreDrift(actual: Session) = {
    actual.drift must be_>(0L)
    actual.setDrift(0)
  }

  "JmsRequestTrackerActor" should {
    val session = Session("mockSession", "mockUserName")
    "pass to next to next actor when matching message is received" in ActorSupport {
      testKit =>
        import testKit._

        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageSent("1", 15, 20, List(), session, testActor, "success")
        tracker ! MessageReceived("1", 30, textMessage("test"))

        val nextSession = expectMsgType[Session]

        ignoreDrift(nextSession) must_== session
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "success", 15, 20, 20, 30, OK, None, List()))
    }

    "pass to next to next actor even if messages are out of sync" in ActorSupport {
      testKit =>
        import testKit._

        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageReceived("1", 30, textMessage("test"))
        tracker ! MessageSent("1", 15, 20, List(), session, testActor, "outofsync")

        val nextSession = expectMsgType[Session]

        ignoreDrift(nextSession) must_== session
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

        val nextSession = expectMsgType[Session]

        ignoreDrift(nextSession) must_== session.markAsFailed
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "failure", 15, 20, 20, 30, KO, Some("Jms check failed"), List()))
    }

    "pass updated session to next actor if modified by checks" in ActorSupport {
      testKit =>
        import testKit._

        val check: JmsCheck = xpath("/id").saveAs("id")
        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        tracker ! MessageSent("1", 15, 20, List(check), session, testActor, "updated")
        tracker ! MessageReceived("1", 30, textMessage("<id>5</id>"))

        val nextSession = expectMsgType[Session]

        ignoreDrift(nextSession) must_== session.set("id", "5")
        tracker.underlyingActor.dataWriterMsg must contain(
          RequestMessage("mockSession", "mockUserName", List(), "updated", 15, 20, 20, 30, OK, None, List()))
    }

    "pass information to session about response time in case group are used" in ActorSupport {
      testKit =>
        import testKit._

        val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

        val groupSession = session.enterGroup("group")
        tracker ! MessageSent("1", 15, 20, List(), groupSession, testActor, "logGroupResponse")
        tracker ! MessageReceived("1", 30, textMessage("group"))

        val newSession = groupSession.logGroupRequest(15, OK)
        val nextSession1 = expectMsgType[Session]

        val failedCheck = JmsSimpleCheck(_ => false)
        tracker ! MessageSent("2", 25, 30, List(failedCheck), newSession, testActor, "logGroupResponse")
        tracker ! MessageReceived("2", 50, textMessage("group"))

        val nextSession2 = expectMsgType[Session]

        ignoreDrift(nextSession1) must_== newSession
        ignoreDrift(nextSession2) must_== newSession.logGroupRequest(25, KO).markAsFailed
    }
  }
}
