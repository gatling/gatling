/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.render.template

import io.gatling.commons.util.Java
import io.gatling.recorder.util.Labelled

private[recorder] sealed abstract class RenderingFormat(
    val label: String,
    val configValue: String,
    val fileExtension: String,
    val lineTermination: String,
    val parameterlessMethodCall: String,
    val hasPackages: Boolean
) extends Labelled
    with Product
    with Serializable {
  override def toString: String = configValue
}

private[recorder] object RenderingFormat {
  val AllFormats: List[RenderingFormat] = List(Java11, Java17, Kotlin, Scala, JavaScript, TypeScript)

  def fromString(configValue: String): RenderingFormat = configValue match {
    case Java11.configValue     => Java11
    case Java17.configValue     => Java17
    case Kotlin.configValue     => Kotlin
    case Scala.configValue      => Scala
    case JavaScript.configValue => JavaScript
    case TypeScript.configValue => TypeScript
    case _                      => throw new IllegalArgumentException(s"Unknown Format $configValue")
  }

  private[recorder] case object Java11 extends RenderingFormat("Java 11", "java11", "java", ";", "()", true)
  private[recorder] case object Java17 extends RenderingFormat("Java 17", "java17", "java", ";", "()", true)
  private[recorder] case object Kotlin extends RenderingFormat("Kotlin", "kotlin", "kt", "", "()", true)
  private[recorder] case object Scala extends RenderingFormat("Scala", "scala", "scala", "", "", true)
  private[recorder] case object JavaScript extends RenderingFormat("JavaScript", "javascript", "gatling.js", ";", "()", false)
  private[recorder] case object TypeScript extends RenderingFormat("TypeScript", "typescript", "gatling.ts", ";", "()", false)

  def defaultFromJvm: RenderingFormat = if (Java.MajorVersion >= 17) RenderingFormat.Java17 else RenderingFormat.Java11
}
