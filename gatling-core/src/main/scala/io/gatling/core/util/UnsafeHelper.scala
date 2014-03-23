/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.util

import scala.util.Try

import sun.misc.Unsafe

object UnsafeHelper {

  val unsafe: Option[Unsafe] = Try {
    val unsafeField = classOf[Unsafe].getDeclaredField("theUnsafe")
    unsafeField.setAccessible(true)
    unsafeField.get(null).asInstanceOf[Unsafe]
  }.toOption

  val stringValueFieldOffset: Option[Long] = unsafe.flatMap(u => Try(u.objectFieldOffset(classOf[String].getDeclaredField("value"))).toOption)
  val stringOffsetFieldOffset: Option[Long] = unsafe.flatMap(u => Try(u.objectFieldOffset(classOf[String].getDeclaredField("offset"))).toOption)
  val stringCountFieldOffset: Option[Long] = unsafe.flatMap(u => Try(u.objectFieldOffset(classOf[String].getDeclaredField("count"))).toOption)
}
