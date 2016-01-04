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

object ReflectionHelper {

  def newInstance[T](className: String): T =
    Class.forName(className).newInstance.asInstanceOf[T]

  def newInstance[T](className: String, params: Object*): T = {
    val clazz = Class.forName(className)

    val constructors = clazz.getConstructors.filter { constructor =>
      val types = constructor.getParameterTypes

      types.length == params.length && params.zip(types).forall { case (o, t) => t.isAssignableFrom(o.getClass) }
    }

    constructors.toList match {
      case constructor :: Nil => constructor.newInstance(params: _*).asInstanceOf[T]
      case Nil                => throw new IllegalArgumentException("No constructor matching parameter types")
      case _                  => throw new IllegalArgumentException("More than one constructor matching parameter types")
    }
  }
}
