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

import java.{ lang => jl }

import scala.annotation.tailrec

object Classes {

  private[util] def appendClassShortName(className: String, sb: jl.StringBuilder): Unit = {
    var bufferedChar: Char = className.charAt(0)
    var offset = 0
    var next = 0
    while ({
      next = className.indexOf('.', offset)
      next != -1
    }) {
      sb.append(bufferedChar).append('.')
      bufferedChar = className.charAt(next + 1)
      offset = next + 2
    }

    if (offset > 0) {
      sb.append(bufferedChar)
    }

    val endOffset = if (className.charAt(className.length - 1) == '$') 1 else 0

    sb.append(className, offset, className.length - endOffset)
  }

  def toClassShortName(className: String): String = {
    val sb = new jl.StringBuilder
    appendClassShortName(className, sb)
    sb.toString
  }

  private def isAnonymousClass(res: Class[_]): Boolean =
    try {
      res.isAnonymousClass
    } catch {
      case e: InternalError if e.getMessage == "Malformed class name" =>
        // https://github.com/scala/bug/issues/2034, FIXED in Java 9
        false
    }

  @tailrec
  def nonAnonSuperclass(clazz: Class[_]): Class[_] =
    if (isAnonymousClass(clazz) || clazz.getName.contains("$anon$")) {
      nonAnonSuperclass(clazz.getSuperclass)
    } else {
      clazz
    }
}
