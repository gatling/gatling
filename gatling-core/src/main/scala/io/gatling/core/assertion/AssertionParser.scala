/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.assertion

import scala.util.parsing.combinator.JavaTokenParsers
import io.gatling.core.assertion.AssertionTags._

class AssertionParserException(input: String, message: String)
  extends Exception(s"Failed to parse assertion $input : $message")

/**
 * Grammar of the Assertions parser (everything inside an assertion is separated by tabs) :
 *
 * {{{
 *    assertion*
 *
 *   <assertion>       ::= ASSERTION PATH <path> TARGET <type> CONDITION <cond>
 *   <path>            ::= GLOBAL | DETAILS <string>+
 *   <metric>          ::= <time_metric> | <count_metric> | MEAN_REQUESTS_PER_SECOND
 *   <time_metric>     ::= RESPONSE_TIME <time_selection>
 *   <time_selection>  ::= MIN | MAX | MEAN | STANDARD_DEVIATION | PERCENTILES_1 | PERCENTILES_2
 *   <count_metric>    ::= <count_type> <count_selection>
 *   <count_type>      ::= ALL_REQUESTS | FAILED_REQUESTS | SUCCESSFUL_REQUESTS
 *   <count_selection> ::= COUNT | PERCENT
 *   <cond>            ::= LESS_THAN <double> | GREATER_THAN <double>  | IS <double> | BETWEEN <double> <double> | IN <double>+
 *
 * }}}
 */
class AssertionParser extends JavaTokenParsers {

  override val whiteSpace = "\t".r

  private def anyOf[T](list: Parser[T]*) = list.reduce(_ | _)

  // ------------------- //
  // -- Paths parsers -- //
  // ------------------- //

  private val parts = """(?U).+?(?=\tTARGET)""".r.map(_.split(whiteSpace.regex).toList)
  private val global = GlobalTag ^^^ Global
  private val forAll = ForAllTag ^^^ ForAll
  private val details = (DetailsTag ~> parts) ^^ Details.apply
  private val path = PathTag ~> anyOf(global, forAll, details)

  // --------------------- //
  // -- Metrics parsers -- //
  // --------------------- //

  private val countMetric = anyOf(
    AllRequestsTag ^^^ AllRequests,
    FailedRequestsTag ^^^ FailedRequests,
    SuccessfulRequestsTag ^^^ SuccessfulRequests)

  private val timeMetric = ResponseTimeTag ^^^ ResponseTime

  // ------------------------ //
  // -- Selections parsers -- //
  // ------------------------ //

  private val countSelection = anyOf(CountTag ^^^ Count, PercentTag ^^^ Percent)

  private val timeSelection = anyOf(
    MinTag ^^^ Min,
    MaxTag ^^^ Max,
    MeanTag ^^^ Mean,
    StandardDeviationTag ^^^ StandardDeviation,
    Percentiles1Tag ^^^ Percentiles1,
    Percentiles2Tag ^^^ Percentiles2,
    Percentiles3Tag ^^^ Percentiles3,
    Percentiles4Tag ^^^ Percentiles4)

  // --------------------- //
  // -- Targets parsers -- //
  // --------------------- //

  private val countTarget = countMetric ~ countSelection ^^ { case metric ~ selection => CountTarget(metric, selection) }

  private val timeTarget = timeMetric ~ timeSelection ^^ { case metric ~ selection => TimeTarget(metric, selection) }

  private val meanRequestsPerSecondTarget = MeanRequestsPerSecondTag ^^^ MeanRequestsPerSecondTarget

  private val target =
    TargetTag ~> anyOf(countTarget, timeTarget, meanRequestsPerSecondTarget)

  // ---------------------- //
  // -- Condition parser -- //
  // ---------------------- //

  private val num = wholeNumber ^^ (_.toInt)

  private val lessThan = (LessThanTag ~> num) ^^ LessThan.apply
  private val greaterThan = (GreaterThanTag ~> num) ^^ GreaterThan.apply
  private val is = (IsTag ~> num) ^^ Is.apply
  private val between = (BetweenTag ~> (num ~ num)) ^^ { case lower ~ upper => Between(lower, upper) }
  private val in = (InTag ~> num.+) ^^ In.apply

  private val condition = ConditionTag ~> anyOf(lessThan, greaterThan, is, between, in)

  // ---------------------- //
  // -- Assertion parser -- //
  // ---------------------- //

  private val assertion = path ~ target ~ condition ^^ { case p ~ t ~ c => Assertion(p, t, c) }

  def parseAssertion(input: String) =
    parseAll(assertion, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => throw new AssertionParserException(input, failure.msg)
    }
}
