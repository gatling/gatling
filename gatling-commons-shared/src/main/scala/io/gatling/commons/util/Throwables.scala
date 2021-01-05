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

import io.gatling.commons.util.Classes._
import io.gatling.netty.util.StringBuilderPool

object Throwables {

  implicit class PimpedException[T <: Throwable](val e: T) extends AnyVal {

    def rootCause: Throwable = {
      var t: Throwable = e
      while (t.getCause != null && t.getCause.ne(t)) {
        t = t.getCause
      }
      t
    }

    def rootMessage: String =
      rootCause.detailedMessage

    def detailedMessage: String = {
      val sb = StringBuilderPool.DEFAULT.get()
      appendClassShortName(nonAnonSuperclass(e.getClass).getName, sb)
      if (e.getMessage != null) {
        sb.append(": ").append(e.getMessage)
      }
      sb.toString
    }
  }
}
