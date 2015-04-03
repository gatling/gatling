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
package io.gatling.core.result.writer

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message._
import io.gatling.core.util.StringHelper._

class FileDataWriterSpec extends BaseSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()
  import FileDataWriter._

  def logMessage(record: ResponseMessage)(implicit serializer: DataWriterMessageSerializer[ResponseMessage]): String = serializer.serialize(record).toString

  "file data writer" should "log a standard request record" in {
    val record = new ResponseMessage("scenario", "1", Nil, "requestName", RequestTimings(2L, 3L, 4L, 5L), OK, Some("message"), Nil)

    logMessage(record) shouldBe s"scenario${Separator}1${Separator}REQUEST${Separator}${Separator}requestName${Separator}2${Separator}3${Separator}4${Separator}5${Separator}OK${Separator}message" + Eol
  }

  it should "append extra info to request records" in {
    val extraInfo: List[String] = List("some", "extra info", "for the log")
    val record = new ResponseMessage("scenario", "1", Nil, "requestName", RequestTimings(2L, 3L, 4L, 5L), OK, Some("message"), extraInfo)

    logMessage(record) shouldBe s"scenario${Separator}1${Separator}REQUEST${Separator}${Separator}requestName${Separator}2${Separator}3${Separator}4${Separator}5${Separator}OK${Separator}message${Separator}some${Separator}extra info${Separator}for the log" + Eol
  }

  it should "sanitize extra info so that simulation log format is preserved" in {
    "\nnewlines \n are\nnot \n\n allowed\n".sanitize shouldBe " newlines   are not    allowed "
    "\rcarriage returns \r are\rnot \r\r allowed\r".sanitize shouldBe " carriage returns   are not    allowed "
    s"${Separator}tabs ${Separator} are${Separator}not ${Separator}${Separator} allowed${Separator}".sanitize shouldBe " tabs   are not    allowed "
  }
}
