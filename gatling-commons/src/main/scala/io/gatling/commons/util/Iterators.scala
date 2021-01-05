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

package io.gatling.commons.util

import scala.collection.AbstractIterator

final class InfiniteIterator[T](value: T) extends AbstractIterator[T] {
  override val hasNext: Boolean = true
  override val next: T = value
}

object CircularIterator {

  def apply[T](values: IndexedSeq[T], threadSafe: Boolean): Iterator[T] = values.length match {
    case 0 => Iterator.empty
    case 1 => new InfiniteIterator(values(0))
    case _ =>
      val counter = if (threadSafe) new CyclicCounter.ThreadSafe(values.length) else new CyclicCounter.NonThreadSafe(values.length)
      Iterator.continually(values(counter.nextVal))
  }
}
