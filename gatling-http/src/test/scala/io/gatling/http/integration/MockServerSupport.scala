package io.gatling.http.integration

import java.io.File

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.IO
import akka.pattern.ask
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import spray.can.Http
import spray.http._

import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.Protocols
import io.gatling.core.controller.DataWritersInitialized
import io.gatling.core.result.writer.{ DataWriter, RunMessage }
import io.gatling.core.session.Session
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.core.test.ActorSupport
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.Predef._
import io.gatling.http.ahc.{ AsyncHandlerActor, HttpEngine }

object MockServerSupport extends StrictLogging {

  val mockHttpPort = Option(Integer.getInteger("gatling.mockHttp.port")).map(_.intValue).getOrElse(8702)
  def httpProtocol = http.baseURL(s"http://localhost:$mockHttpPort")

  var serverActor: ActorRef = _

  var requests: List[HttpRequest] = Nil

  class HttpActor(process: PartialFunction[HttpRequest, HttpResponse]) extends Actor {

    var bindSender: ActorRef = _
    var httpListener: ActorRef = _
    var unbindSender: ActorRef = _

    override def receive: Receive = {

      case bind: Http.Bind =>
        bindSender = sender()
        IO(Http)(GatlingActorSystem.instance) ! bind

      case bound: Http.Bound =>
        httpListener = sender()
        bindSender ! bound

      // when a new connection comes in we register ourselves as the connection handler
      case _: Http.Connected => sender ! Http.Register(self)

      case unbind: Http.Unbind =>
        unbindSender = sender()
        httpListener ! unbind

      case Http.Unbound =>
        unbindSender ! Http.Unbound

      case r: HttpRequest if process.isDefinedAt(r) =>
        record(r)
        sender ! process(r)

      case r: HttpRequest =>
        record(r)
        logger.warn(s"Unhandled request: $r")
        sender ! HttpResponse(404)
    }

    def record(request: HttpRequest) = {
      requests = request :: requests
    }
  }

  def serverMock(f: PartialFunction[Any, HttpResponse])(implicit testKit: TestKit with ImplicitSender) = {
    serverActor = GatlingActorSystem.instance.actorOf(Props(new HttpActor(f)), "mockServerActor")
    implicit val timout = Timeout(4 seconds)
    val future = serverActor ? Http.Bind(serverActor, interface = "localhost", port = mockHttpPort)
    Await.result(future, Duration.Inf)
  }

  def apply(f: TestKit with ImplicitSender => Any): Unit = {
    ActorSupport { implicit testKit =>
      import testKit._

      try {
        HttpEngine.start()
        AsyncHandlerActor.start()

        //Initialise DataWriter with fake data.
        DataWriter.init(Nil, RunMessage("FakeSimulation", "fakesimulation1", nowMillis, "A fake run"), Nil, self)
        expectMsgClass(classOf[DataWritersInitialized])

        f(testKit)
      } finally {
        /*
         * DataWriter, AsyncHandlerActor and HttpEngine don't need explicit shutdown - they have callbacks registered
         * with GatlingActorSystem, so they will be shutdown with the ActorSystem
         */
        if (serverActor != null) {
          implicit val timeout = Timeout(8 seconds)
          val future = serverActor ? Http.Unbind(4 second)
          Await.result(future, Duration.Inf)
        }

        serverActor = null
        requests = Nil
      }
    }
  }

  def runScenario(sb: ScenarioBuilder, timeout: FiniteDuration = 10.seconds, protocols: Protocols = Protocols(httpProtocol))(implicit testKit: TestKit with ImplicitSender) = {
    import testKit._

    val actor = sb.build(testKit.self, protocols)
    actor ! Session("TestSession", "testUser")
    expectMsgClass(timeout, classOf[Session])
  }

  def verifyRequestTo(path: String): Unit = verifyRequestTo(path, 1)

  def verifyRequestTo(path: String, count: Int, checks: (HttpRequest => Unit)*): Unit = {
    val filteredRequests = requests.filter(_.uri.path.toString == path)
    val actualCount = filteredRequests.size
    if (actualCount != count) {
      throw new AssertionError(s"Expected to access $path $count times, but actually accessed it $actualCount times.")
    }

    checks.foreach(check =>
      filteredRequests.foreach(check))
  }

  def file(name: String, contentType: ContentType = ContentTypes.`application/octet-stream`): HttpEntity = {
    val resource = getClass.getClassLoader.getResource(name)
    val file = new File(resource.getFile)
    HttpEntity(contentType, HttpData(file))
  }

}

object Checks {
  def checkCookie(cookie: String, value: String)(request: HttpRequest) = {
    val cookies = request.cookies.filter(_.name == cookie)

    if (cookies.isEmpty) {
      throw new AssertionError(s"In request $request there were no header '$header'")
    }

    for (cookie <- cookies) {
      if (cookie.content != value) {
        throw new AssertionError(s"In request $request cookie '${cookie.name}' expected to be '$value' but actually was '${cookie.content}'")
      }
    }
  }
}

