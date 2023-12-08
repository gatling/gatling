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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.exec.Executable;
import io.gatling.javaapi.core.internal.Executables;

/** A holder for choice types */
public final class Choice {
  private Choice() {}

  /** @deprecated Use {@link CoreDsl#onCase(Object)} instead. */
  @Deprecated
  public static WithKey withKey(
      @NonNull Object key, @NonNull Executable executable, @NonNull Executable... executables) {
    return new WithKey(key, Executables.toChainBuilder(executable, executables));
  }

  /** @deprecated Use {@link CoreDsl#percent(double)} instead. */
  @Deprecated
  public static WithWeight withWeight(
      double weight, @NonNull Executable executable, @NonNull Executable... executables) {
    return new WithWeight(weight, Executables.toChainBuilder(executable, executables));
  }

  /** A choice with an expected key */
  public static final class WithKey {
    public final Object key;
    public final ChainBuilder chain;

    /**
     * @param key the expected key
     * @param chain the chain that will be triggered if the switch matches the expected key
     */
    private WithKey(@NonNull Object key, @NonNull ChainBuilder chain) {
      this.key = key;
      this.chain = chain;
    }

    public static final class Then {
      private final Object key;

      public Then(Object key) {
        this.key = key;
      }

      public WithKey then(@NonNull Executable executable, @NonNull Executable... executables) {
        return new WithKey(key, Executables.toChainBuilder(executable, executables));
      }
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
    public WithWeight(double weight, @NonNull ChainBuilder chain) {
      this.weight = weight;
      this.chain = chain;
    }

    public static final class Then {
      private final double weight;

      public Then(double weight) {
        this.weight = weight;
      }

      public WithWeight then(@NonNull Executable executable, @NonNull Executable... executables) {
        return new WithWeight(weight, Executables.toChainBuilder(executable, executables));
      }
    }
  }
}
