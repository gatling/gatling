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

package io.gatling.core.javaapi;

import javax.annotation.Nonnull;

/** A holder for possibility types */
public final class Possibility {
  private Possibility() {}

  /** A possibility with an expected value */
  public static final class WithValue {
    public final Object value;
    public final ChainBuilder chain;

    /**
     * @param value the expected value
     * @param chain the chain that will be triggered if the switch matches the expected value
     */
    public WithValue(@Nonnull Object value, @Nonnull ChainBuilder chain) {
      this.value = value;
      this.chain = chain;
    }
  }

  /** A possibility with a weight */
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
