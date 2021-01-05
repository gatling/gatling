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

trait Clock {
  def nowMillis: Long
  def nowSeconds: Long = nowMillis / 1000
}

class DefaultClock extends Clock {

  private val currentTimeMillisReference = System.currentTimeMillis
  private val nanoTimeReference = System.nanoTime

  override def nowMillis: Long = (System.nanoTime - nanoTimeReference) / 1000000 + currentTimeMillisReference
}
