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

package io.gatling.compiler.config

import java.nio.file.{ Files, Path, Paths }

import scala.util.Properties._

private[compiler] object ConfigUtils {

  // WARN copied from io.gatling.commons.util.PathHelper
  implicit def string2path(pathString: String): Path = Paths.get(pathString)

  implicit class RichPath(val path: Path) extends AnyVal {

    def /(pathString: String): Path = path.resolve(pathString)

    def /(other: Path): Path = path.resolve(other)

    def exists: Boolean = Files.exists(path)
  }

  // WARN copied from io.gatling.core.config.GatlingFiles
  val GatlingHome: Path = Paths.get(envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", ".")))

  def resolvePath(path: Path): Path =
    (if (path.isAbsolute || path.exists) path else GatlingHome / path).normalize().toAbsolutePath

  def string2option(string: String): Option[String] = string.trim match {
    case "" => None
    case s  => Some(s)
  }
}
