/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.redis.feeder

import io.gatling.core.feeder.{ Feeder, FeederBuilder }

import com.redis.{ RedisClient, RedisClientPool }

/**
 * Class for feeding data from Redis DB, using LPOP, SPOP or
 * SRANDMEMBER commands.
 *
 * Originally contributed by Krishnen Chedambarum.
 */
object RedisFeeder {

  // Function for executing Redis command
  @deprecated(message = "Will be removed in 3.3.0. Use RedisPredef#redisFeeder instead.", since = "3.2.0")
  type RedisCommand = (RedisClient, String) => Option[String]

  // LPOP Redis command
  @deprecated(message = "Will be removed in 3.3.0. Use RedisPredef#redisFeeder instead.", since = "3.2.0")
  def LPOP(redisClient: RedisClient, key: String): Option[String] = redisClient.lpop(key)

  // SPOP Redis command
  @deprecated(message = "Will be removed in 3.3.0. Use RedisPredef#redisFeeder instead.", since = "3.2.0")
  def SPOP(redisClient: RedisClient, key: String): Option[String] = redisClient.spop(key)

  // SRANDMEMBER Redis command
  @deprecated(message = "Will be removed in 3.3.0. Use RedisPredef#redisFeeder instead.", since = "3.2.0")
  def SRANDMEMBER(redisClient: RedisClient, key: String): Option[String] = redisClient.srandmember(key)

  @deprecated(message = "Will be removed in 3.3.0. Use RedisPredef#redisFeeder instead.", since = "3.2.0")
  def apply(clientPool: RedisClientPool, key: String, redisCommand: RedisCommand = LPOP): FeederBuilder =
    RedisFeederBuilder(clientPool, key, redisCommand)
}

final case class RedisFeederBuilder(clientPool: RedisClientPool, key: String, command: RedisFeeder.RedisCommand = RedisFeeder.LPOP) extends FeederBuilder {
  def LPOP: RedisFeederBuilder = copy(command = RedisFeeder.LPOP)
  def SPOP: RedisFeederBuilder = copy(command = RedisFeeder.SPOP)
  def SRANDMEMBER: RedisFeederBuilder = copy(command = RedisFeeder.SRANDMEMBER)

  override def apply(): Feeder[Any] = {
    def next: Option[Map[String, String]] = clientPool.withClient { client =>
      val value = command(client, key)
      value.map(value => Map(key -> value))
    }

    Iterator.continually(next).takeWhile(_.isDefined).map(_.get)
  }
}
