/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core;

import javax.annotation.Nonnull;

/** A holder for choice types */
public final class Choice {
  private Choice() {}

  public static WithKey withKey(@Nonnull Object key, @Nonnull ChainBuilder chain) {
    return new WithKey(key, chain);
  }

  public static WithWeight withWeight(double weight, @Nonnull ChainBuilder chain) {
    return new WithWeight(weight, chain);
  }

  /** A choice with an expected key */
  public static final class WithKey {
    public final Object key;
    public final ChainBuilder chain;

    /**
     * @param key the expected key
     * @param chain the chain that will be triggered if the switch matches the expected key
     */
    public WithKey(@Nonnull Object key, @Nonnull ChainBuilder chain) {
      this.key = key;
      this.chain = chain;
    }
  }

  /** A choice with a weight */
  public static final class WithWeight {
    public final double weight;
    public final ChainBuilder chain;

    /**
     * @param weight the associated weight
     * @param chain the chain that will be triggered if the switch randomly falls into the range
     *     computed from the weights
     */
    public WithWeight(double weight, @Nonnull ChainBuilder chain) {
      this.weight = weight;
      this.chain = chain;
    }
  }
}
