/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.shared.unstable.model.stats.assertion

import io.gatling.commons.stats.assertion.Assertion

sealed trait AssertionResult extends Product with Serializable {
  def success: Boolean
  def assertion: Assertion
}

object AssertionResult {
  final case class Resolved(assertion: Assertion, success: Boolean, actualValue: Double) extends AssertionResult

  final case class ResolutionError(assertion: Assertion, error: String) extends AssertionResult {
    override def success: Boolean = false
  }
}
