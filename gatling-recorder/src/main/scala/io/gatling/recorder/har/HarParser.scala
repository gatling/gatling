/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.recorder.har

import java.io.InputStream

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._

object HarParser {

  private implicit val formats = DefaultFormats

  case class HarHttpArchive(log: HarLog)

  case class HarLog(entries: Seq[HarEntry])

  case class HarEntry(startedDateTime: String, time: Option[Double], timings: Option[HarTimings], request: HarRequest, response: HarResponse)

  case class HarRequest(httpVersion: String, method: String, url: String, headers: Seq[HarHeader], postData: Option[HarRequestPostData])

  case class HarHeader(name: String, value: String)

  case class HarRequestPostData(mimeType: String, text: Option[String], params: Seq[HarRequestPostParam])

  case class HarRequestPostParam(name: String, value: String)

  case class HarResponse(status: Int, headers: Seq[HarHeader], statusText: String, content: HarResponseContent)

  case class HarResponseContent(mimeType: String, encoding: Option[String], text: Option[String], comment: Option[String])

  case class HarTimings(blocked: Double, dns: Double, connect: Double, ssl: Double, send: Double, waitTiming: Double, receive: Double) {
    val time: Double = blocked + dns + connect + ssl + send + waitTiming + receive
  }

  def parseHarEntries(is: InputStream): Seq[HarEntry] = {
    val json = parse(is).transformField {
      case ("wait", x) => ("waitTiming", x)
    }
    json.extract[HarHttpArchive].log.entries
  }
}
