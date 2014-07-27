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

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.OK
import io.gatling.core.util.StringHelper._

class FileDataWriterSpec extends FlatSpec with Matchers {

  GatlingConfiguration.setUp()

  import FileDataWriter._

  def logMessage(record: RequestMessage): String = new String(record.getBytes)

  "file data writer" should "log a standard request record" in {
    val record = new RequestMessage("scenario", "1", Nil, "requestName", 2L, 3L, 4L, 5L, OK, Some("message"), Nil)

    logMessage(record) shouldBe "scenario\t1\tREQUEST\t\trequestName\t2\t3\t4\t5\tOK\tmessage" + Eol
  }

  it should "append extra info to request records" in {
    val extraInfo: List[String] = List("some", "extra info", "for the log")
    val record = new RequestMessage("scenario", "1", Nil, "requestName", 2L, 3L, 4L, 5L, OK, Some("message"), extraInfo)

    logMessage(record) shouldBe "scenario\t1\tREQUEST\t\trequestName\t2\t3\t4\t5\tOK\tmessage\tsome\textra info\tfor the log" + Eol
  }

  it should "sanitize extra info so that simulation log format is preserved" in {
    "\nnewlines \n are\nnot \n\n allowed\n".sanitize shouldBe " newlines   are not    allowed "
    "\rcarriage returns \r are\rnot \r\r allowed\r".sanitize shouldBe " carriage returns   are not    allowed "
    "\ttabs \t are\tnot \t\t allowed\t".sanitize shouldBe " tabs   are not    allowed "
  }
}
