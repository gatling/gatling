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

import io.gatling.core.result.message.OK

class RequestMessageSpec extends FlatSpec with Matchers {

  "constructor" should "have sensible defaults for optional parameters" in {
    val record: RequestMessage = RequestMessage("scenarioName", "1", Nil, "requestName", 0L, 0L, 0L, 0L, OK, Some("requestMessage"), Nil)

    record.extraInfo shouldBe empty
  }
}
