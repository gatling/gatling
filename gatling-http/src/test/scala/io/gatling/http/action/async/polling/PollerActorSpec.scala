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
package io.gatling.http.action.async.polling

import scala.concurrent.duration._
import scala.reflect.ClassTag

import io.gatling.AkkaSpec
import io.gatling.commons.validation.Failure
import io.gatling.core.session._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.DataWritersStatsEngine
import io.gatling.core.stats.writer.ErrorMessage
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.{ HttpRequestConfig, HttpRequestDef }
import io.gatling.http.response.ResponseBuilderFactory

import akka.testkit._
import org.mockito.Mockito._
import org.mockito.Matchers._

// TODO : test resourceFetched, stopPolling
class PollerActorSpec extends AkkaSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()

  val requestName = "foo".expressionSuccess

  def newHttpRequestDef = HttpRequestDef(requestName, failedExpr, mock[HttpRequestConfig])

  "PollerActor" should "start in Uninitalized state with NoData" in {
    val dataWriterProbe = TestProbe()
    val poller = createPollerActor(1.second, newHttpRequestDef, mock[HttpEngine], dataWriterProbe)

    poller.stateName shouldBe Uninitialized
    poller.stateData shouldBe NoData
  }

  it should "after receiving a StartPolling, move to the Polling state with the initial session" in {
    val dataWriterProbe = TestProbe()
    val poller = createPollerActor(1.second, newHttpRequestDef, mock[HttpEngine], dataWriterProbe)

    val session = Session("scenario", 0)

    poller ! StartPolling(session)

    poller.isTimerActive(PollerActor.PollTimerName) shouldBe true
    poller.stateName shouldBe Polling
    poller.stateData shouldBe a[PollingData]
    poller.stateData.asInstanceOf[PollingData] shouldBe PollingData(session, Session.Identity)
  }

  it should "do nothing if the request name could not be resolved and fail the session" in {
    val dataWriterProbe = TestProbe()
    val mockHttpEngine = mock[HttpEngine]
    val poller = createPollerActor(1.second, newHttpRequestDef, mockHttpEngine, dataWriterProbe)
    val session = Session("scenario", 0)

    poller ! StartPolling(session)
    Thread.sleep(2.seconds.toMillis)

    verify(mockHttpEngine, never).httpClient(any[Session], any[HttpProtocol])
    poller.stateName shouldBe Polling
    poller.stateData shouldBe a[PollingData]
    val pollingData = poller.stateData.asInstanceOf[PollingData]
    pollingData.session.isFailed shouldBe true
  }

  it should "do nothing if the request could not be resolved, fail the session and report to the DataWriters" in {
    val dataWriterProbe = TestProbe()
    val mockHttpEngine = mock[HttpEngine]
    val poller = createPollerActor(1.second, newHttpRequestDef, mockHttpEngine, dataWriterProbe)
    val session = Session("scenario", 0)

    poller ! StartPolling(session)
    Thread.sleep(2.seconds.toMillis)

    verify(mockHttpEngine, never).httpClient(any[Session], any[HttpProtocol])
    poller.stateName shouldBe Polling
    poller.stateData shouldBe a[PollingData]
    val pollingData = poller.stateData.asInstanceOf[PollingData]
    pollingData.session.isFailed shouldBe true

    dataWriterProbe.expectMsgType[ErrorMessage]
  }

  def createPollerActor(
    period:          FiniteDuration,
    requestDef:      HttpRequestDef,
    httpEngine:      HttpEngine,
    dataWriterProbe: TestProbe
  ) =
    TestFSMRef(
      new PollerActor(
        pollerName = "testPoller",
        period = period,
        requestDef = requestDef,
        responseBuilderFactory = mock[ResponseBuilderFactory],
        statsEngine = new DataWritersStatsEngine(system, List(dataWriterProbe.ref))
      )
    )

  def failedExpr[T: ClassTag]: Expression[T] =
    session => Failure("Failed expression")
}
