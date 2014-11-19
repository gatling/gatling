/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.assertion

object AssertionTags {

  // -- Assertion tags -- //

  val AssertionTag = "ASSERTION"
  val PathTag = "PATH"
  val TargetTag = "TARGET"
  val ConditionTag = "CONDITION"

  // -- Path tags -- //

  val GlobalTag = "GLOBAL"
  val DetailsTag = "DETAILS"
  val ForAllTag = "FORALL"

  // -- Metric tags -- //

  val AllRequestsTag = "ALL_REQUESTS"
  val FailedRequestsTag = "FAILED_REQUESTS"
  val SuccessfulRequestsTag = "SUCCESSFUL_REQUESTS"

  val ResponseTimeTag = "RESPONSE_TIME"

  // -- Selection tags -- //

  val CountTag = "COUNT"
  val PercentTag = "PERCENT"

  val MinTag = "MIN"
  val MaxTag = "MAX"
  val MeanTag = "MEAN"
  val StandardDeviationTag = "STANDARD_DEVIATION"
  val Percentiles1Tag = "PERCENTILES_1"
  val Percentiles2Tag = "PERCENTILES_2"

  // -- Target tags -- //

  val MeanRequestsPerSecondTag = "MEAN_REQUESTS_PER_SECOND"

  // -- Condition tags -- //

  val LessThanTag = "LESS_THAN"
  val GreaterThanTag = "GREATER_THAN"
  val IsTag = "IS"
  val BetweenTag = "BETWEEN"
  val InTag = "IN"

}
