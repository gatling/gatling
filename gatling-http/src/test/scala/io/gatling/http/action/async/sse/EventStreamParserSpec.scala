/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.async.sse

import io.gatling.BaseSpec

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._

class EventStreamParserSpec extends BaseSpec {

  private def parseFullSse(sse: String): ServerSentEvent = {
    val sseDispatcher = mock[EventStreamDispatcher]
    val sseParser = new EventStreamDispatcher with EventStreamParser {
      def dispatchEventStream(sse: ServerSentEvent): Unit = sseDispatcher.dispatchEventStream(sse)
    }

    sseParser.parse(sse)

    val argumentCapture = ArgumentCaptor.forClass(classOf[ServerSentEvent])
    verify(sseDispatcher, times(1)).dispatchEventStream(argumentCapture.capture())
    argumentCapture.getValue
  }

  val name = "snapshot"
  val id = "4d80cbbb-d456-4268-816a-0f8e411eec42"
  val data = """[{"title":"Value 1","price":91,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 2","price":52,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 3","price":64,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 4","price":10,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 5","price":67,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 6","price":86,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 7","price":40,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 8","price":91,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 9","price":1,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 10","price":95,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 11","price":91,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 12","price":13,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 13","price":52,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 14","price":24,"param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"},{"title":"Value 15","price":30","param1":"value1","param2":"value2","param3":"value3","param4":"value4","param5":"value5","param6":"value6","param7":"value7","param8":"value8"}]"""
  val retry = 1200

  "completeInARowSse" should "return a server sent event with a snapshot type" in {

    val completeSse = s"""event: $name
id: $id
data: $data
retry: $retry

"""

    val sse = parseFullSse(completeSse)

    sse.name shouldBe Some(name)
    sse.id shouldBe Some(id)
    sse.data shouldBe Some(data)
    sse.retry shouldBe Some(retry)
  }

  "completeSse" should "return a server sent event with a snapshot type" in {

    val sseDispatcher = mock[EventStreamDispatcher]
    val sseParser = new EventStreamDispatcher with EventStreamParser {
      def dispatchEventStream(sse: ServerSentEvent): Unit = sseDispatcher.dispatchEventStream(sse)
    }

    sseParser.parse(s"event: $name\n")
    sseParser.parse(s"id: $id\n")
    sseParser.parse(s"data: $data\n")
    sseParser.parse(s"retry: $retry\n\n")

    val argumentCapture = ArgumentCaptor.forClass(classOf[ServerSentEvent])
    verify(sseDispatcher, times(1)).dispatchEventStream(argumentCapture.capture())
    val sse = argumentCapture.getValue

    sse.name shouldBe Some(name)
    sse.id shouldBe Some(id)
    sse.data shouldBe Some(data)
    sse.retry shouldBe Some(retry)
  }

  "sseNoRetry" should "return a server sent event with a snapshot type with no retry" in {

    val sseNoRetry = s"""event: $name
id: $id
data: $data

"""

    val sse = parseFullSse(sseNoRetry)
    sse.name shouldBe Some(name)
    sse.id shouldBe Some(id)
    sse.data shouldBe Some(data)
    sse.retry shouldBe None
  }

  "sseNoRetryNoId" should "return a server sent event with a snapshot type with no retry and no id" in {

    val sseNoRetryNoId = s"""event: $name
data: $data

"""

    val sse = parseFullSse(sseNoRetryNoId)
    sse.name shouldBe Some(name)
    sse.data shouldBe Some(data)
    sse.id shouldBe None
    sse.retry shouldBe None
  }

  "sseOnlyData" should "return a server sent event with only data" in {

    val sseOnlyData = s"""data: $data

"""

    val sse = parseFullSse(sseOnlyData)

    sse.data shouldBe Some(data)
    sse.name shouldBe None
    sse.id shouldBe None
    sse.retry shouldBe None
  }

  "sseWithExtraFields" should "return a server sent event with legacy fields" in {

    val sseWithExtraFields = s"""event: $name
id: $id
data: $data
retry: $retry
foo: bar
fooz

"""

    val sse = parseFullSse(sseWithExtraFields)
    sse.name shouldBe Some(name)
    sse.id shouldBe Some(id)
    sse.data shouldBe Some(data)
    sse.retry shouldBe Some(retry)
  }

  "sseWithComments" should "return a server sent event with comments" in {
    val sseWithComments = s""": This is a begin comment
event: $name
id: $id
: This is a comment
data: $data
retry: $retry
: This is an end comment

"""

    val sse = parseFullSse(sseWithComments)
    sse.name shouldBe Some(name)
    sse.id shouldBe Some(id)
    sse.data shouldBe Some(data)
    sse.retry shouldBe Some(retry)
  }
}
