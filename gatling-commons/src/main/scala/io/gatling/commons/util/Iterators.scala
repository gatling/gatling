/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.collection.AbstractIterator

object Iterators {

  /**
   * On contrary to Iterator.continually that takes a by-name parameter that gets evaluated on each iteration,
   * this one takes a static value.
   *
   * @param value the value to be eternally returned
   * @tparam T the type of value
   * @return the value
   */
  def infinitely[T](value: T): Iterator[T] = new AbstractIterator[T] {

    override def hasNext: Boolean = true

    override def next(): T = value
  }
}
