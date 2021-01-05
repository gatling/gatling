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

package io.gatling.sbt

import io.gatling.core.scenario.Simulation

import sbt.testing.SubclassFingerprint

/**
 * Gatling's specific fingerprint, which defines which classes are to be
 * picked up by the test framework from the test ClassLoader as test classes,
 * in this case Gatling simulations.
 */
class GatlingFingerprint extends SubclassFingerprint {

  /** Matches only Scala classes, as simulation objects are not supported. */
  override val isModule: Boolean = false

  /** All classes that are to be picked up must extend ''Simulation'' */
  override val superclassName: String = classOf[Simulation].getName

  /** Gatling simulations does not take constructor arguments. */
  override val requireNoArgConstructor: Boolean = true
}
