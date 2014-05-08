package io.gatling.http.integration

import akka.actor.ActorRef
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.typesafe.scalalogging.slf4j.Logging
import io.gatling.core.config.Protocols
import io.gatling.core.controller.DataWritersInitialized
import io.gatling.core.result.writer.{ RunMessage, DataWriter }
import io.gatling.core.session.Session
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.core.test.ActorSupport
import io.gatling.http.Predef._
import io.gatling.http.ahc.{ AsyncHandlerActor, HttpEngine }
import org.joda.time.DateTime
import org.specs2.execute.{ Result, AsResult }
import org.specs2.specification.Fixture
import scala.concurrent.duration._
import com.github.tomakehurst.wiremock.common.SingleRootFileSource

object WireMockSupport extends Fixture[TestKit with ImplicitSender] with Logging {
  val wireMockPort = Option(Integer.getInteger("gatling.wireMock.port")).map(_.intValue).getOrElse(8702)
  def httpProtocol = http.baseURL(s"http://localhost:$wireMockPort")

  def runScenario(sb: ScenarioBuilder, timeout: FiniteDuration = 10 seconds, protocols: Protocols = Protocols(httpProtocol))(implicit testKit: TestKit with ImplicitSender) = {
    import testKit._
    val buildMethod = classOf[ScenarioBuilder].getMethod("build", classOf[ActorRef], classOf[Protocols])
    buildMethod.setAccessible(true)
    val actor = buildMethod.invoke(sb, testKit.self, protocols).asInstanceOf[ActorRef]
    actor ! Session("TestSession", "testUser")
    expectMsgClass(timeout, classOf[Session])
  }

  def apply[R: AsResult](f: TestKit with ImplicitSender => R): Result = apply(ActorSupport.consoleOnlyConfig)(f)

  def apply[R: AsResult](config: Map[String, _])(f: TestKit with ImplicitSender => R): Result = {
    ActorSupport(config) { implicit testKit =>
      import testKit._
      val wireMockServer = new WireMockServer(wireMockPort, new SingleRootFileSource("gatling-http/src/test/resources"), false)

      try {
        WireMock.configureFor("localhost", wireMockPort)
        wireMockServer.start()
        // Do we need to run Controller.start()???

        HttpEngine.start()
        AsyncHandlerActor.start()

        //Initialise DataWriter with fake data.
        DataWriter.init(RunMessage("FakeSimulation", "fakesimulation1", DateTime.now, "A fake run"), Nil, self)
        expectMsgClass(classOf[DataWritersInitialized])

        f(testKit)
      } finally {
        /*
         * DataWriter, AsyncHandlerActor and HttpEngine don't need explicit shutdown - they have callbacks registered
         * with GatlingActorSystem, so they will be shutdown with the ActorSystem
         */
        WireMock.reset()
        wireMockServer.stop()
      }
    }
  }
}
