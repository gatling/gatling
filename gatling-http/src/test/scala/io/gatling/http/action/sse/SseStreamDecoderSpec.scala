/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.sse

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.BaseSpec
import io.gatling.http.action.sse.fsm.{ ServerSentEvent, SseStreamDecoder }

import io.netty.buffer.Unpooled

class SseStreamDecoderSpec extends BaseSpec {

  private val longString = "x" * 920

  private val data =
    s""": test stream
       |
       |data: first event
       |id: 1
       |
       |data:second event 加特林岩石
       |id
       |
       |data:  third event $longString
       |foo: bar
       |
    """.stripMargin

  private val bytes = data.getBytes(UTF_8)

  private val expected = Seq(
    ServerSentEvent(
      name = None,
      data = Some("first event"),
      id = Some("1"),
      retry = None
    ),
    ServerSentEvent(
      name = None,
      data = Some("second event 加特林岩石"),
      id = None,
      retry = None
    ),
    ServerSentEvent(
      name = None,
      data = Some(s" third event $longString"),
      id = None,
      retry = None
    )
  )

  private def decodeChunks(splitPos: Int) = {
    val (chunk1, chunk2) = bytes.splitAt(splitPos)
    val chunks = Seq(Unpooled.wrappedBuffer(chunk1), Unpooled.wrappedBuffer(chunk2))
    try {
      val decoder = new SseStreamDecoder
      chunks.flatMap(decoder.decodeStream)
    } finally {
      chunks.foreach(_.release())
    }
  }

  "SseStreamDecoder" should "be able to decode split UTF-8 chars" in {
    (0 until bytes.length).foreach { splitPos =>
      (splitPos, decodeChunks(splitPos)) shouldBe (splitPos, expected)
    }
  }
}
