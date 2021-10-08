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

import static io.gatling.javaapi.core.internal.Feeders.*;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

public final class RedisFeederBuilder implements Supplier<Iterator<Map<String, Object>>> {

  private final io.gatling.redis.feeder.RedisFeederBuilder wrapped;

  RedisFeederBuilder(io.gatling.redis.feeder.RedisFeederBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Instruct to use a LPOP command (default)
   *
   * @return a new RedisFeederBuilder instance
   */
  @Nonnull
  public RedisFeederBuilder LPOP() {
    return new RedisFeederBuilder(wrapped.LPOP());
  }

  /**
   * Instruct to use a SPOP command (default)
   *
   * @return a new RedisFeederBuilder instance
   */
  @Nonnull
  public RedisFeederBuilder SPOP() {
    return new RedisFeederBuilder(wrapped.SPOP());
  }

  /**
   * Instruct to use a SRANDMEMBER command (default)
   *
   * @return a new RedisFeederBuilder instance
   */
  @Nonnull
  public RedisFeederBuilder SRANDMEMBER() {
    return new RedisFeederBuilder(wrapped.SRANDMEMBER());
  }

  @Override
  public Iterator<Map<String, Object>> get() {
    return toJavaFeeder(wrapped.apply());
  }
}
