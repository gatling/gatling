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
package io.gatling.http.resolver

import scala.io.Source

import io.gatling.BaseSpec

class HostsFileParserSpec extends BaseSpec {

  val sample1 =
    """##
      | # Host Database
      |#
      |  # localhost is used to configure the loopback interface
      | # when the system is booting.  Do not change this entry.
      |##
      |127.0.0.1	      localhost me
      |::1              localhost stillme
      |
      |192.168.0.100    pi0
      | 192.168.0.101    pi1
      |  192.168.0.102    pi2 # end of line comment
      |
      |192.168.0.200    pi0
      | 192.168.0.201    pi1 # end of line comment
      |  192.168.0.202    pi2
    """.stripMargin

  val sample2 =
    """127.0.0.1 # not valid
      |192.168.0.100    pi0 # valid
    """.stripMargin

  val sample3 =
    """127.0.0.1 # not valid
      |192.168.0.100    pi0 # valid
      |notvalid         stilltrying
    """.stripMargin

  import HostsFileParser._

  "Hosts file parser" should "parse valid hosts file and kept the first entries only" in {
    val source = Source.fromString(sample1)

    parse(source) shouldBe Map(
      "localhost" -> "127.0.0.1",
      "me" -> "127.0.0.1",
      "stillme" -> "::1",
      "pi0" -> "192.168.0.100",
      "pi1" -> "192.168.0.101",
      "pi2" -> "192.168.0.102"
    )
  }

  it should "only keep valid entries" in {
    val source = Source.fromString(sample2)

    parse(source) shouldBe Map(
      "pi0" -> "192.168.0.100"
    )
  }

  it should "throw away invalid addresses after conversion to arrays of bytes" in {
    val source = Source.fromString(sample3)

    asByteArray(parse(source)).keys shouldBe Set("pi0")
  }
}
