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

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{ Function => JFunction }

import scala.compat.java8.FunctionConverters._

object ClassHelper {

  object PimpedClass {
    private val ShortClassNamesCache = new ConcurrentHashMap[Class[_], String]
    private val ShortClassNameFunction: Class[_] => String = clazz => {
      val sb = new StringBuilder
      clazz.getPackage.getName.split('.').foreach { p =>
        sb.append(p.head).append('.')
      }
      sb.append(clazz.getSimpleName).toString
    }
    private val ShortClassNameComputer: JFunction[Class[_], String] = ShortClassNameFunction.asJava
  }

  implicit class PimpedClass(val clazz: Class[_]) extends AnyVal {

    import PimpedClass._

    def getShortName: String = ShortClassNamesCache.computeIfAbsent(clazz, ShortClassNameComputer)
  }
}
