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

import sbt.testing.{ Event, Fingerprint, OptionalThrowable, Selector, Status }

/**
 * The event fired by the test framework if the simulation was successful,
 * eg. Gatling doesn't prematurely stop and assertions didn't fail.
 *
 * @param fullyQualifiedName The simulation class' fully qualified name.
 * @param fingerprint The [[GatlingFingerprint]] for this simulation.
 * @param selector The Selector used for this simulation.
 * @param throwable The exception that may have been thrown by Gatling.
 * @param duration The simulation's execution's duration.
 */
final case class SimulationSuccessful(
    fullyQualifiedName: String,
    fingerprint: Fingerprint,
    selector: Selector,
    throwable: OptionalThrowable,
    duration: Long
) extends Event {

  override val status: Status = Status.Success
}

/**
 * The event fired by the test framework if the simulation failed,
 * eg. Gatling exits prematurely or at least one assertion failed.
 *
 * @param fullyQualifiedName The simulation class' fully qualified name.
 * @param fingerprint The [[GatlingFingerprint]] for this simulation.
 * @param selector The Selector used for this simulation.
 * @param throwable The exception that may have been thrown by Gatling.
 * @param duration The simulation's execution's duration.
 */
final case class SimulationFailed(
    fullyQualifiedName: String,
    fingerprint: Fingerprint,
    selector: Selector,
    throwable: OptionalThrowable,
    duration: Long
) extends Event {

  override val status: Status = Status.Failure
}

/**
 * The event fired by the test framework if Gatling couldn't start
 * due to an invalid argument provided to it.
 *
 * @param fullyQualifiedName The simulation class' fully qualified name.
 * @param fingerprint The [[GatlingFingerprint]] for this simulation.
 * @param selector The Selector used for this simulation.
 * @param throwable The exception that may have been thrown by Gatling.
 * @param duration The simulation's execution's duration.
 */
final case class InvalidArguments(
    fullyQualifiedName: String,
    fingerprint: Fingerprint,
    selector: Selector,
    throwable: OptionalThrowable,
    duration: Long
) extends Event {

  override val status: Status = Status.Error
}
