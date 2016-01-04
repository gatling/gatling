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
package io.gatling.redis.util

import java.nio.charset.StandardCharsets._

import io.gatling.commons.util.StringHelper.Crlf

object RedisHelper {

  /**
   * Generate Redis protocol required for mass insert
   * i.e  generateRedisProtocol("LPUSH", "SIM", "SOMETHING COOL!")
   */
  def generateRedisProtocol(d: String*): String = {
    val protocol = new StringBuilder().append("*").append(d.length).append(Crlf)
    d.foreach { x =>
      val length = x.getBytes(UTF_8).length
      protocol.append("$").append(length).append(Crlf).append(x).append(Crlf)
    }
    protocol.toString()
  }
}
