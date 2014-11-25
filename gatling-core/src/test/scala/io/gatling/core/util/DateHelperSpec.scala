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
package io.gatling.core.util

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.util.DateHelper._
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeParseException

class DateHelperSpec extends FlatSpec with Matchers {

  def testDate = LocalDateTime.of(2014, 10, 1, 7, 30)

  "parseTimestampString" should "successfully parse a correct timestamp sting" in {
    val dateTime = parseTimestampString("20141231063528")
    dateTime.getYear shouldBe 2014
    dateTime.getMonthValue shouldBe 12
    dateTime.getDayOfMonth shouldBe 31
    dateTime.getHour shouldBe 6
    dateTime.getMinute shouldBe 35
    dateTime.getSecond shouldBe 28
  }

  it should "fail if it's not a timestamp string" in {
    intercept[DateTimeParseException](parseTimestampString("20081228"))
  }

  "RichDateTime" should "correctly print a LocalDateTime with the 'human format' using toHumanDate" in {
    testDate.toHumanDate shouldBe "2014-10-01 07:30:00"
  }

  it should "correctly print a LocalDateTime with the 'timestamp format' using toTimestamp" in {
    testDate.toTimestamp shouldBe "20141001073000"
  }
}
