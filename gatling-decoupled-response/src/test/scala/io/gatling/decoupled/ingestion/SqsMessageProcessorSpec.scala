/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.decoupled.ingestion

import java.time.Instant

import akka.Done
import io.gatling.BaseSpec
import io.gatling.decoupled.models.ExecutionId.ExecutionId
import io.gatling.decoupled.models.{ ExecutionId, ExecutionPhase, NormalExecutionPhase }
import io.gatling.decoupled.state.PendingRequestsState
import org.mockito.Mockito.when
import software.amazon.awssdk.services.sqs.model.Message
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class SqsMessageProcessorSpec extends BaseSpec with ScalaFutures {

  behavior of "SqsMessageProcessor"

  it should "parse and forward the SQS message" in new Fixtures {

    val message = givenProperMessage
    givenRegistrationCallsWorks

    val result = processor.apply(message)

    result.futureValue shouldBe Done
    expectResponseRegistration

  }

  it should "fail if message is invalid" in new Fixtures {
    val message = givenInvalidMessage
    givenRegistrationCallsWorks

    val result = processor.apply(message)

    result.failed.futureValue shouldBe a[Exception]
    expectNoResponseRegistration
  }

  it should "fail if state change fails" in new Fixtures {
    val message = givenProperMessage
    givenRegistrationCallsFail

    val result = processor.apply(message)

    result.failed.futureValue shouldBe a[Exception]
  }

  trait Fixtures {

    val state = mock[PendingRequestsState]

    val processor = new SqsMessageProcessor(state)

    val id = ExecutionId("17d754d8-6c15-4c0c-b13b-b7d6318095b6")
    val phases = Seq(
      NormalExecutionPhase("phase-1", Instant.ofEpochMilli(1585921485350L)),
      NormalExecutionPhase("phase-2", Instant.ofEpochMilli(1585921485370L))
    )

    def givenProperMessage: Message =
      message(
        """
          |{
          |  "id": "17d754d8-6c15-4c0c-b13b-b7d6318095b6",
          |  "phases": [
          |    {
          |      "name": "phase-1",
          |      "time": 1585921485350
          |    },
          |    {
          |      "name": "phase-2",
          |      "time": 1585921485370
          |    }
          |  ]
          |}""".stripMargin
      )

    def givenInvalidMessage: Message =
      message(
        """[{
          | "name": "phase-1"
          | "time": "yesterday" 
          |}]""".stripMargin
      )

    private def message(body: String) = {
      val builder = Message.builder()
      builder.body(body).build()
    }

    def givenRegistrationCallsWorks: Unit =
      when(state.registerResponse(any[ExecutionId], any[Seq[NormalExecutionPhase]])) thenReturn Future.successful(Done)

    def givenRegistrationCallsFail: Unit =
      when(state.registerResponse(any[ExecutionId], any[Seq[NormalExecutionPhase]])) thenReturn Future.failed(new Exception("error"))

    def expectResponseRegistration: Unit =
      verify(state, times(1)).registerResponse(id, phases)

    def expectNoResponseRegistration: Unit =
      verify(state, never).registerResponse(any[ExecutionId], any[Seq[NormalExecutionPhase]])

  }

}
