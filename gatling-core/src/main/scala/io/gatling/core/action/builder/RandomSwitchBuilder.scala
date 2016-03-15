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
package io.gatling.core.action.builder

import scala.annotation.tailrec
import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.commons.validation._
import io.gatling.commons.util.Collections._
import io.gatling.core.action.{ Action, Switch }
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.core.util.NameGen

import com.typesafe.scalalogging.StrictLogging

object RandomSwitchBuilder {

  val Accuracy = 10000

  def percentageToInt(p: Double) = (p * Accuracy / 100).toInt

  def randomWithinAccuracy: Int = ThreadLocalRandom.current.nextInt(Accuracy)

  def apply(possibilities: List[(Double, ChainBuilder)], elseNext: Option[ChainBuilder]) = {
    val normalizedPossibilities = possibilities
      .collect { case (p, c) => (percentageToInt(p), c) }
      .filter { case (p, c) => p > 0 }
    new RandomSwitchBuilder(normalizedPossibilities, elseNext)
  }
}

class RandomSwitchBuilder(possibilities: List[(Int, ChainBuilder)], elseNext: Option[ChainBuilder]) extends ActionBuilder with StrictLogging with NameGen {

  import RandomSwitchBuilder._

  val sum = possibilities.sumBy(_._1)
  require(sum <= Accuracy, s"Random switch weights sum is ${sum.toDouble / 100}, mustn't be bigger than 100%")
  if (sum == Accuracy && elseNext.isDefined)
    logger.warn("Random switch has a 100% sum, yet a else is defined?!")

  override def build(ctx: ScenarioContext, next: Action): Action = {

    val possibleActions = possibilities.map {
      case (percentage, possibility) =>
        val possibilityAction = possibility.build(ctx, next)
        (percentage, possibilityAction)
    }

    val elseNextAction = elseNext.map(_.build(ctx, next)).getOrElse(next)

    val nextAction: Expression[Action] = _ => {

        @tailrec
        def determineNextAction(index: Int, possibilities: List[(Int, Action)]): Action = possibilities match {
          case Nil => elseNextAction
          case (percentage, possibleAction) :: others =>
            if (percentage >= index)
              possibleAction
            else
              determineNextAction(index - percentage, others)
        }

      determineNextAction(randomWithinAccuracy, possibleActions).success
    }
    new Switch(nextAction, ctx.coreComponents.statsEngine, genName("randomSwitch"), next)
  }
}
