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

package io.gatling.redis.util

import io.gatling.BaseSpec
import io.gatling.commons.util.StringHelper.Crlf
import io.gatling.redis.util.RedisHelper.generateRedisProtocol

class RedisHelperSpec extends BaseSpec {

  "generateRedisProtocol" should "generate a correct protocol" in {
    val correctProtocol = List("*3", "$3", "SET", "$5", "mykey", "$7", "myvalue").mkString("", Crlf, Crlf)

    generateRedisProtocol("SET", "mykey", "myvalue") shouldBe correctProtocol
  }

  it should "count length by bytes length" in {
    val correctProtocol = List("*3", "$3", "SET", "$5", "mykey", "$16", "もふもふmofu").mkString("", Crlf, Crlf)

    generateRedisProtocol("SET", "mykey", "もふもふmofu") shouldBe correctProtocol
  }
}
