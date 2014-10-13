package io.gatling.compiler

import java.nio.file._
import scala.util.Properties.{ envOrElse, propOrElse }

import com.typesafe.config.ConfigFactory

object CompilerConfiguration {

  private val encodingKey = "gatling.core.encoding"
  private val sourceDirectoryKey = "gatling.core.directory.simulations"
  private val binariesDirectoryKey = "gatling.core.directory.binaries"

  private def resolvePath(path: Path): Path = {
    if (path.isAbsolute || Files.exists(path)) path else GatlingHome.resolve(path)
  }

  def string2option(string: String) = string.trim match {
    case "" => None
    case s  => Some(s)
  }

  private val configuration = {
    val customConfig = ConfigFactory.load("gatling.conf")
    val defaultConfig = ConfigFactory.load("gatling-defaults.conf")
    customConfig.withFallback(defaultConfig)
  }

  val GatlingHome = Paths.get(envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", ".")))
  val encoding = configuration.getString(encodingKey)
  val sourceDirectory = resolvePath(Paths.get(configuration.getString(sourceDirectoryKey)))
  val binariesDirectory = string2option(configuration.getString(binariesDirectoryKey)).map(Paths.get(_)).getOrElse(GatlingHome.resolve("target"))
  val classesDirectory = binariesDirectory.resolve("test-classes")
}
