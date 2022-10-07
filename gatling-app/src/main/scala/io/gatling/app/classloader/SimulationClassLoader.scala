/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import java.nio.file.Path

import scala.util.Properties

import io.gatling.app.{ JavaSimulation, SimulationClass }
import io.gatling.commons.shared.unstable.util.PathHelper
import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.core.scenario.Simulation

private[gatling] object SimulationClassLoader {
  def apply(binariesDirectory: Path): SimulationClassLoader =
    new SimulationClassLoader(selectClassLoaderImplementation(binariesDirectory), binariesDirectory)

  private def selectClassLoaderImplementation(binariesDirectory: Path): ClassLoader =
    if (isInClasspath(binariesDirectory)) getClass.getClassLoader
    else new FileSystemBackedClassLoader(binariesDirectory, getClass.getClassLoader)

  private def isInClasspath(binariesDirectory: Path): Boolean = {
    val classpathElements = Properties.javaClassPath split File.pathSeparator
    classpathElements.contains(binariesDirectory.toString)
  }
}

private[gatling] class SimulationClassLoader(classLoader: ClassLoader, binaryDir: Path) {
  def simulationClasses: List[SimulationClass] =
    PathHelper
      .deepFiles(binaryDir, _.path.hasExtension("class"))
      .map(file => classLoader.loadClass(pathToClassName(file.path, binaryDir)))
      .collect {
        case clazz if isScalaSimulationClass(clazz) => SimulationClass.Scala(clazz.asInstanceOf[Class[Simulation]])
        case clazz if isJavaSimulationClass(clazz)  => SimulationClass.Java(clazz.asInstanceOf[Class[JavaSimulation]])
      }
      .toList

  private def isConcreteClass(clazz: Class[_]): Boolean =
    !(clazz.isInterface || Modifier.isAbstract(clazz.getModifiers))

  private def isScalaSimulationClass(clazz: Class[_]): Boolean =
    classOf[Simulation].isAssignableFrom(clazz) && isConcreteClass(clazz)

  private def isJavaSimulationClass(clazz: Class[_]): Boolean =
    classOf[JavaSimulation].isAssignableFrom(clazz) && isConcreteClass(clazz)

  private def pathToClassName(path: Path, root: Path): String =
    path.getParent
      .resolve(path.stripExtension)
      .toString
      .stripPrefix(root.toString + File.separator)
      .replace(File.separator, ".")
}
