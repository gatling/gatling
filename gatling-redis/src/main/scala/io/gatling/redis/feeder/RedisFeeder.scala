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
package io.gatling.redis.feeder

import com.redis.{ RedisClient, RedisClientPool }

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.feeder.Feeder

/**
 * Class for feeding data from Redis DB, using LPOP, SPOP or
 * SRANDMEMBER commands.
 *
 * Originally contributed by Krishnen Chedambarum.
 */
object RedisFeeder extends AkkaDefaults {

  // Function for executing Redis command
  type RedisCommand = (RedisClient, String) => Option[String]

  // LPOP Redis command
  def LPOP(redisClient: RedisClient, key: String) = redisClient.lpop(key)

  // SPOP Redis command
  def SPOP(redisClient: RedisClient, key: String) = redisClient.spop(key)

  // SRANDMEMBER Redis command
  def SRANDMEMBER(redisClient: RedisClient, key: String) = redisClient.srandmember(key)

  def apply(clientPool: RedisClientPool, key: String, redisCommand: RedisCommand = LPOP): Feeder[String] = {
    system.registerOnTermination(clientPool.close)

      def next = clientPool.withClient { client =>
        val value = redisCommand(client, key)
        value.map(value => Map(key -> value))
      }

    Iterator.continually(next).takeWhile(_.isDefined).map(_.get)
  }
}
