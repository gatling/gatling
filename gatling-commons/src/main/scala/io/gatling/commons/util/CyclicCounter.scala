/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import java.util.concurrent.atomic.AtomicInteger

sealed trait CyclicCounter {
  def nextVal(): Int
}

object CyclicCounter {
  final class ThreadSafe(max: Int) extends CyclicCounter {
    require(max >= 2, "max must be >= 2")
    private val counter = new AtomicInteger
    private val maxIndex = max - 1
    def nextVal(): Int = counter.getAndUpdate(i => if (i < maxIndex) i + 1 else 0)
  }

  final class NonThreadSafe(max: Int) extends CyclicCounter {
    require(max >= 2, "max must be >= 2")
    private var counter = 0
    private val maxIndex = max - 1
    def nextVal(): Int = {
      val current = counter
      counter = if (counter < maxIndex) counter + 1 else 0
      current
    }
  }
}
