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

package io.gatling.app.classloader

import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.{ Files, Path, Paths }

import scala.util.Properties

import io.gatling.app.{ JavaSimulation, SimulationClass }
import io.gatling.core.scenario.Simulation
import io.gatling.shared.util.PathHelper
import io.gatling.shared.util.PathHelper._

private[gatling] final class SimulationClassLoader(classLoader: ClassLoader) {
  def simulationClasses: List[SimulationClass] =
    Properties.javaClassPath
      .split(File.pathSeparator)
      .map(classpathElement => Paths.get(classpathElement))
      .filter(classpathElement => Files.isDirectory(classpathElement))
      .flatMap { directory =>
        PathHelper
          .deepFiles(directory, _.path.hasExtension("class"))
          .flatMap { file =>
            val clazz = classLoader.loadClass(pathToClassName(file.path, directory))
            SimulationClass.fromClass(clazz).toList
          }
      }
      .toList

  private def pathToClassName(path: Path, root: Path): String =
    path.getParent
      .resolve(path.stripExtension)
      .toString
      .stripPrefix(root.toString + File.separator)
      .replace(File.separator, ".")
}
