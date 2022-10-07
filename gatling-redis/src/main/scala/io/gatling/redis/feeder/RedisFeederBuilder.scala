/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.core.feeder.{ Feeder, NamedFeederBuilder }

import com.redis.{ RedisClient, RedisClientPool }

/**
 * Class for feeding data from Redis DB, using LPOP, SPOP or SRANDMEMBER, RPOPLPUSH commands.
 *
 * Originally contributed by Krishnen Chedambarum. RPOPLPUSH added by Shoaib Khan
 */
object RedisFeederBuilder {
  // Function for executing Redis command
  private type RedisCommand = (RedisClient, String, String) => Option[String]

  private val LPOP: RedisCommand = (redisClient, keySrc, _) => redisClient.lpop(keySrc)

  private val SPOP: RedisCommand = (redisClient, keySrc, _) => redisClient.spop(keySrc)

  private val SRANDMEMBER: RedisCommand = (redisClient, keySrc, _) => redisClient.srandmember(keySrc)

  private val RPOPLPUSH: RedisCommand = (redisClient, keySrc, keyDest) => redisClient.rpoplpush(keySrc, keyDest)

  def apply(clientPool: RedisClientPool, keySrc: String, keyDest: String): RedisFeederBuilder =
    new RedisFeederBuilder(clientPool, RedisFeederBuilder.LPOP, keySrc, keyDest)
}

final case class RedisFeederBuilder(clientPool: RedisClientPool, command: RedisFeederBuilder.RedisCommand, keySrc: String, keyDest: String)
    extends NamedFeederBuilder {
  def LPOP: RedisFeederBuilder = copy(command = RedisFeederBuilder.LPOP)
  def SPOP: RedisFeederBuilder = copy(command = RedisFeederBuilder.SPOP)
  def SRANDMEMBER: RedisFeederBuilder = copy(command = RedisFeederBuilder.SRANDMEMBER)
  def RPOPLPUSH: RedisFeederBuilder = copy(command = RedisFeederBuilder.RPOPLPUSH)

  override def apply(): Feeder[Any] = {
    def next: Option[Map[String, String]] = clientPool.withClient { client =>
      val value = command(client, keySrc, keyDest)
      value.map(value => Map(keySrc -> value))
    }

    Iterator.continually(next).takeWhile(_.isDefined).map(_.get)
  }

  override val name: String = "redis"
}
