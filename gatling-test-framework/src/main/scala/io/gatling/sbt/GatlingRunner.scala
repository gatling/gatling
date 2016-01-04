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
package io.gatling.sbt

import sbt.testing.{ Runner, TaskDef }

/**
 * As there is no further special handling needed or simulations to reject,
 * [[GatlingRunner]] simply creates a [[GatlingTask]] for each discovered simulation.
 *
 *  @param args the arguments for the new run.
 * @param remoteArgs the arguments for the run in a forked JVM.
 * @param testClassLoader the test ClassLoader, provided by SBT.
 */
class GatlingRunner(val args: Array[String], val remoteArgs: Array[String], testClassLoader: ClassLoader) extends Runner {

  def tasks(taskDefs: Array[TaskDef]) = taskDefs.map(new GatlingTask(_, testClassLoader, args, remoteArgs))

  def done = "Simulation(s) execution ended."

}
