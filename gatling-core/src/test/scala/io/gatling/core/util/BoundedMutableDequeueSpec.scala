/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.core.util

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

final class BoundedMutableDequeueSpec extends AnyFlatSpecLike with Matchers {

  "BoundedMutableDequeue" should "have a max size and removeAll return elements in insert order" in {
    val q = new BoundedMutableDequeue[Int](2)
    q.addOne(1)
    q.addOne(2)
    q.addOne(3)
    q.removeAll() shouldBe List(2, 3)
  }

  it should "have removeAllReverse empty it" in {
    val q = new BoundedMutableDequeue[Int](1)
    q.addOne(1)
    q.removeAll()
    q.addOne(2)
    q.removeAll() shouldBe 2 :: Nil
  }
}
