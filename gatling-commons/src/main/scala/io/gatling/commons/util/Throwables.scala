/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import java.io.PrintWriter

import io.gatling.commons.util.ClassHelper._

object Throwables {

  implicit class PimpedException[T <: Throwable](val e: T) extends AnyVal {

    def unknownStackTrace(clazz: Class[_], method: String): T = {
      e.setStackTrace(Array[StackTraceElement](new StackTraceElement(clazz.getName, method, null, -1)))
      e
    }

    def rootCause: Throwable = {
      var t: Throwable = e
      while (t.getCause != null) {
        t = t.getCause
      }
      t
    }

    def rootMessage: String =
      rootCause.detailedMessage

    def detailedMessage: String = {
      val nonAnon = e.getClass.nonAnonSuperclass
      if (e.getMessage == null) {
        nonAnon.getShortName
      } else {
        s"${nonAnon.getShortName}: ${e.getMessage}"
      }
    }

    def stackTraceString: String = {
      val sw = new FastStringWriter()
      e.printStackTrace(new PrintWriter(sw))
      sw.toString
    }
  }
}
