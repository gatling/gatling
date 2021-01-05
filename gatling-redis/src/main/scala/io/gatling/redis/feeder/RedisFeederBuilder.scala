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

package io.gatling.redis.feeder

import io.gatling.core.feeder.{ Feeder, FeederBuilder }

import com.redis.{ RedisClient, RedisClientPool }

/**
 * Class for feeding data from Redis DB, using LPOP, SPOP or
 * SRANDMEMBER commands.
 *
 * Originally contributed by Krishnen Chedambarum.
 */
object RedisFeederBuilder {

  // Function for executing Redis command
  private type RedisCommand = (RedisClient, String) => Option[String]

  private val LPOP: RedisCommand = (redisClient, key) => redisClient.lpop(key)

  private val SPOP: RedisCommand = (redisClient, key) => redisClient.spop(key)

  private val SRANDMEMBER: RedisCommand = (redisClient, key) => redisClient.srandmember(key)

  def apply(clientPool: RedisClientPool, key: String): RedisFeederBuilder =
    new RedisFeederBuilder(clientPool, key, RedisFeederBuilder.LPOP)
}

final case class RedisFeederBuilder(clientPool: RedisClientPool, key: String, command: RedisFeederBuilder.RedisCommand) extends FeederBuilder {
  def LPOP: RedisFeederBuilder = copy(command = RedisFeederBuilder.LPOP)
  def SPOP: RedisFeederBuilder = copy(command = RedisFeederBuilder.SPOP)
  def SRANDMEMBER: RedisFeederBuilder = copy(command = RedisFeederBuilder.SRANDMEMBER)

  override def apply(): Feeder[Any] = {
    def next: Option[Map[String, String]] = clientPool.withClient { client =>
      val value = command(client, key)
      value.map(value => Map(key -> value))
    }

    Iterator.continually(next).takeWhile(_.isDefined).map(_.get)
  }
}
