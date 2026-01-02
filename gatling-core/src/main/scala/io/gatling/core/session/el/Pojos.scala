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

package io.gatling.core.session.el

import scala.util.control.NonFatal

import io.gatling.commons.validation._

import jodd.introspector.ClassIntrospector

object Pojos {
  private val UnknownPropertyFailure = "Unknown property".failure
  private val NoGetterForPropertyFailure = "No getter for property".failure
  private val GetterInvocationFailure = "Getter invocationFailure".failure

  def getProperty(bean: Any, name: String): Validation[Any] =
    if (bean.getClass.getName.startsWith("java.")) {
      UnknownPropertyFailure
    } else {
      val propertyDescriptor = ClassIntrospector.get.lookup(bean.getClass).getPropertyDescriptor(name, true)
      if (propertyDescriptor == null) {
        UnknownPropertyFailure
      } else {
        val getter = propertyDescriptor.getGetter(true)
        if (getter == null) {
          NoGetterForPropertyFailure
        } else {
          try {
            getter.invokeGetter(bean).success
          } catch {
            case NonFatal(_) => GetterInvocationFailure
          }
        }
      }
    }
}
