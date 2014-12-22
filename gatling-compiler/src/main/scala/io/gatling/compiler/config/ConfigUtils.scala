package io.gatling.compiler.config

import java.nio.file.{ Files, Path, Paths }

import scala.util.Properties._

object ConfigUtils {

  val GatlingHome = Paths.get(envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", ".")))

  def resolvePath(path: Path): Path = {
    if (path.isAbsolute || Files.exists(path)) path else GatlingHome.resolve(path)
  }

  def string2option(string: String) = string.trim match {
    case "" => None
    case s  => Some(s)
  }
}
