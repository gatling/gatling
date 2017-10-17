/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

object Har {

  private implicit val formats = DefaultFormats

  case class RawHttpArchive(log: RawLog)

  case class RawLog(entries: Seq[RawEntry])

  case class RawEntry(startedDateTime: String, time: Float, request: RawRequest, response: RawResponse)

  case class RawRequest(method: String, url: String, headers: Seq[RawHeader], postData: Option[RawPostData])

  case class RawHeader(name: String, value: String)

  case class RawPostData(mimeType: String, text: String, params: Seq[RawPostParam])

  case class RawPostParam(name: String, value: String)

  case class RawResponse(status: Int, content: RawContent)

  case class RawContent(mimeType: Option[String], encoding: Option[String], text: Option[String])

  def parseStream(is: InputStream): RawHttpArchive = {
    val json = parse(is)
    json.extract[RawHttpArchive]
  }
}
