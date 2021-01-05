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

import java.util.concurrent.ThreadLocalRandom

import io.gatling.commons.util.Spire._

object Arrays {

  private def swap[T](array: Array[T], i: Int, j: Int): Unit = {
    val tmp = array(i)
    array(i) = array(j)
    array(j) = tmp
  }

  def shuffle[T](array: Array[T]): Array[T] =
    shuffle(array, array.length)

  def shuffle[T](array: Array[T], length: Int): Array[T] = {
    val rnd = ThreadLocalRandom.current()
    cfor(length)(_ > 1, _ - 1) { i =>
      swap(array, i - 1, rnd.nextInt(i))
    }
    array
  }
}
