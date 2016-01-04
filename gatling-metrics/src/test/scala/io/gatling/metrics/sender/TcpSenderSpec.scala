/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import akka.testkit._
import akka.io.Tcp._
import akka.util.ByteString

import io.gatling.AkkaSpec
import io.gatling.metrics.message.GraphiteMetrics

class TcpSenderSpec extends AkkaSpec {

  val dummySocketAddress = new InetSocketAddress(9999)

  class TcpSenderNoIo extends TcpSender(dummySocketAddress, 2, 1.second) {
    override def askForConnection(): Unit = ()
  }

  "TcpSender" should "fail if server is unreachable" in {
    val tcpSender = TestFSMRef(new TcpSenderNoIo)

    // Fail 2 times in a row, retry limit is exhausted
    tcpSender ! CommandFailed(Connect(dummySocketAddress))
    tcpSender ! CommandFailed(Connect(dummySocketAddress))

    tcpSender.stateName shouldBe RetriesExhausted
    tcpSender.stateData shouldBe NoData
  }

  it should "go to the Running state and send metrics if it could connect without issues" in {
    val tcpSender = TestFSMRef(new TcpSenderNoIo)

    tcpSender ! Connected(dummySocketAddress, dummySocketAddress)

    expectMsg(Register(tcpSender))

    tcpSender.stateName shouldBe Running

    val metrics = GraphiteMetrics(Iterator.single("foo" -> 1), 1)

    tcpSender ! metrics

    expectMsg(Write(metrics.byteString))
  }

  it should "retry to connected until the retry limit has been exceeded to finally stop" in {
    val tcpSender = TestFSMRef(new TcpSenderNoIo)

    // Connect
    tcpSender ! Connected(dummySocketAddress, dummySocketAddress)
    expectMsg(Register(tcpSender))

    tcpSender.stateName shouldBe Running

    // Fail one time, retries limit is not exhausted
    tcpSender ! PeerClosed
    tcpSender ! Connected(dummySocketAddress, dummySocketAddress)

    tcpSender.stateName shouldBe Running

    // Make sure one second has passed to reset the retry window
    Thread.sleep(1.second.toMillis)

    // Fail 2 times in a row, retry limit is exhausted
    tcpSender ! CommandFailed(Write(ByteString.empty))
    tcpSender ! CommandFailed(Write(ByteString.empty))

    tcpSender.stateName shouldBe RetriesExhausted
    tcpSender.stateData shouldBe NoData
  }
}
