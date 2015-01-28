/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.metrics.sender

import java.net.InetSocketAddress

import scala.concurrent.duration._

import akka.actor.{ PoisonPill, ActorSystem }
import akka.testkit._

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.test.ActorSupport
import io.gatling.metrics.message.SendMetric
import io.gatling.metrics.server.TcpServer

class TcpSenderSpec extends FlatSpec with Matchers {

  val serverUrl = new InetSocketAddress(2003)

  def newTcpServer(implicit actorSystem: ActorSystem) = TestActorRef[TcpServer]

  "TcpSender" should "fail if server is unreachable" in ActorSupport { implicit testKit =>
    import testKit._

    val tcpSender = TestFSMRef(new TcpSender(serverUrl, 1, 1.second))

    Thread.sleep(2000) // Wait for failure

    tcpSender.stateName shouldBe RetriesExhausted
    tcpSender.stateData shouldBe NoData
  }

  it should "go to the Running state and send metrics if it could connect without issues" in ActorSupport { implicit testKit =>
    import testKit._

    val tcpServer = newTcpServer
    val tcpSender = TestFSMRef(new TcpSender(serverUrl, 1, 1.second))

    Thread.sleep(2000) // Give the sender some time to initialize

    tcpServer.underlyingActor.receivedCount shouldBe 0

    tcpSender.stateName shouldBe Running

    tcpSender ! SendMetric("foo", 1, 1)
    Thread.sleep(500) // Give some time to the server to receive the message
    tcpServer.underlyingActor.receivedCount shouldBe 1
  }

  it should "retry to connected until the retry limit has been exceeded to finally stop" in ActorSupport { implicit testKit =>
    import testKit._

    var tcpServer = newTcpServer
    val tcpSender = TestFSMRef(new TcpSender(serverUrl, 5, 10.seconds))

    Thread.sleep(2000) // Give the sender some time to initialize

    tcpSender.stateName shouldBe Running

    tcpServer ! PoisonPill
    Thread.sleep(6000) // Wait before restarting
    tcpServer = newTcpServer
    tcpSender.stateName shouldBe RetriesExhausted

  }
}
