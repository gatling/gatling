/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.result.writer

import com.excilys.ebi.gatling.core.result.message.{RequestStatus, RequestRecord}
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

import org.junit.runner.RunWith

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import java.io.StringWriter

@RunWith(classOf[JUnitRunner])
class FileDataWriterSpec extends Specification {

  "file data writer" should {

    "log a standard request record" in {
      val record = new RequestRecord("scenario", 1, "requestName", 2L, 3L, 4L, 5L, RequestStatus.OK, Some("message"))

      val stringWriter = new StringWriter()

      FileDataWriter.append(stringWriter, record)

      val loggedRequestRecord:String = stringWriter.getBuffer.toString

      loggedRequestRecord must beEqualTo("ACTION\tscenario\t1\trequestName\t2\t3\t4\t5\tOK\tmessage" + END_OF_LINE)
    }

    "append extra info to request records" in {
      val extraInfo:List[String] = List("some", "extra info", "for the log")
      val record = new RequestRecord("scenario", 1, "requestName", 2L, 3L, 4L, 5L, RequestStatus.OK, Some("message"), extraInfo)

      val stringWriter = new StringWriter()

      FileDataWriter.append(stringWriter, record)

      val loggedRequestRecord:String = stringWriter.getBuffer.toString

      loggedRequestRecord must beEqualTo("ACTION\tscenario\t1\trequestName\t2\t3\t4\t5\tOK\tmessage\tsome\textra info\tfor the log" + END_OF_LINE)
    }

    "sanitize extra info so that simulation log format is preserved" in {
      FileDataWriter.sanitize("\nnewlines \n are\nnot \n\n allowed\n") must beEqualTo(" newlines   are not    allowed ")
      FileDataWriter.sanitize("\rcarriage returns \r are\rnot \r\r allowed\r") must beEqualTo(" carriage returns   are not    allowed ")
      FileDataWriter.sanitize("\ttabs \t are\tnot \t\t allowed\t") must beEqualTo(" tabs   are not    allowed ")
    }

  }
}
