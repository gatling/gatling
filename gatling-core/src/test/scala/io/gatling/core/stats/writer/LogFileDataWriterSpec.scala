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
package io.gatling.core.stats.writer

import io.gatling.BaseSpec
import io.gatling.commons.stats.OK
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.ResponseTimings

class LogFileDataWriterSpec extends BaseSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()
  import LogFileDataWriter._

  def logMessage(record: ResponseMessage)(implicit serializer: DataWriterMessageSerializer[ResponseMessage]): String = serializer.serialize(record).toString

  "file data writer" should "log a standard request record" in {
    val record = new ResponseMessage("scenario", 0, Nil, "requestName", ResponseTimings(2L, 5L), OK, Some("200"), Some("message"), Nil)

    logMessage(record) shouldBe s"REQUEST${Separator}scenario${Separator}0${Separator}${Separator}requestName${Separator}2${Separator}5${Separator}OK${Separator}message" + Eol
  }

  it should "append extra info to request records" in {
    val extraInfo: List[String] = List("some", "extra info", "for the log")
    val record = new ResponseMessage("scenario", 0, Nil, "requestName", ResponseTimings(2L, 5L), OK, Some("200"), Some("message"), extraInfo)

    logMessage(record) shouldBe s"REQUEST${Separator}scenario${Separator}0${Separator}${Separator}requestName${Separator}2${Separator}5${Separator}OK${Separator}message${Separator}some${Separator}extra info${Separator}for the log" + Eol
  }

  "sanitize" should "sanitize extra info so that simulation log format is preserved" in {
    "\nnewlines \n are\nnot \n\n allowed\n".sanitize shouldBe " newlines   are not    allowed "
    "\rcarriage returns \r are\rnot \r\r allowed\r".sanitize shouldBe " carriage returns   are not    allowed "
    s"${Separator}tabs ${Separator} are${Separator}not ${Separator}${Separator} allowed${Separator}".sanitize shouldBe " tabs   are not    allowed "
  }
}
