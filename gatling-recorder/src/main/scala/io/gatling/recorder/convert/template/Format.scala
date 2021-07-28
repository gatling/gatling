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

package io.gatling.recorder.convert.template

import io.gatling.recorder.util.Labelled

private[recorder] sealed abstract class Format(
    val label: String,
    val configValue: String,
    val fileExtension: String,
    val lineTermination: String,
    val parameterlessMethodCall: String
) extends Labelled
    with Product
    with Serializable {
  override def toString: String = configValue
}

private[recorder] object Format {

  val AllFormats: List[Format] = List(Scala, Java8, Java11, Java17, Kotlin)

  def fromString(configValue: String): Format = configValue match {
    case Scala.configValue  => Scala
    case Java8.configValue  => Java8
    case Java11.configValue => Java11
    case Java17.configValue => Java17
    case Kotlin.configValue => Kotlin
    case _                  => throw new IllegalArgumentException(s"Unknown Format $configValue")
  }

  private[recorder] case object Scala extends Format("Scala", "scala", "scala", "", "")
  private[recorder] case object Kotlin extends Format("Kotlin", "kotlin", "kt", "", "()")
  private[recorder] case object Java8 extends Format("Java 8", "java8", "java", ";", "()")
  private[recorder] case object Java11 extends Format("Java 11", "java11", "java", ";", "()")
  private[recorder] case object Java17 extends Format("Java 17", "java17", "java", ";", "()")
}
