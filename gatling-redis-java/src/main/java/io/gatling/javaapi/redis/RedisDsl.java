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

package io.gatling.javaapi.redis;

import com.redis.RedisClientPool;
import javax.annotation.Nonnull;

/** The entrypoint of the Redis DSL */
public final class RedisDsl {
  private RedisDsl() {}

  /**
   * Bootstrap a Redis feeder
   *
   * @param clients the clients pool
   * @param key the key to look up in the redis command
   * @return a new RedisFeederBuilder instance
   */
  @Nonnull
  public static RedisFeederBuilder redisFeeder(
      @Nonnull RedisClientPool clients, @Nonnull String key) {
    return new RedisFeederBuilder(io.gatling.redis.Predef.redisFeeder(clients, key));
  }
}
