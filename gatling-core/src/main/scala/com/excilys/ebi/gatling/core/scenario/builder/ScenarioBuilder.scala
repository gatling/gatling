package com.excilys.ebi.gatling.core.scenario.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.IfActionBuilder._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.context.Context

object ScenarioBuilder {
  private var expectedExecutionDuration: Map[Int, Long] = Map.empty
  private var numberOfRelevantActionsByScenario: Map[Int, Int] = Map.empty

  def addToExecutionTime(scenarioId: Int, timeValue: Long, timeUnit: TimeUnit) = {
    expectedExecutionDuration += (scenarioId -> (TimeUnit.MILLISECONDS.convert(timeValue, timeUnit) + expectedExecutionDuration.get(scenarioId).getOrElse(0L)))
  }

  def getExecutionTime(scenarioId: Int) = TimeUnit.SECONDS.convert(expectedExecutionDuration.get(scenarioId).get, TimeUnit.MILLISECONDS)

  abstract class ScenarioBuilder[B <: ScenarioBuilder[B]](name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action]) extends AbstractActionBuilder {

    def actionsList = actionBuilders
    def getName = name

    def newInstance(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action]): B

    def pause(delayValue: Int): B = {
      pause(delayValue, delayValue, TimeUnit.SECONDS)
    }

    def pause(delayValue: Int, delayUnit: TimeUnit): B = {
      pause(delayValue, delayValue, delayUnit)
    }

    def pause(delayMinValue: Int, delayMaxValue: Int): B = {
      pause(delayMinValue * 1000, delayMaxValue * 1000, TimeUnit.MILLISECONDS)
    }

    def pause(delayMinValue: Int, delayMaxValue: Int, delayUnit: TimeUnit): B = {
      logger.debug("Adding PauseAction")
      newInstance(name, (pauseActionBuilder withMinDuration delayMinValue withMaxDuration delayMaxValue withTimeUnit delayUnit) :: actionBuilders, next)
    }

    def doIf(testFunction: Context => Boolean, chainTrue: B): B = {
      doIf(testFunction, chainTrue, None)
    }

    def doIf(testFunction: Context => Boolean, chainTrue: B, chainFalse: B): B = {
      doIf(testFunction, chainTrue, Some(chainFalse))
    }

    def doIf(contextKey: String, value: String, chainTrue: B): B = {
      doIf((c: Context) => c.getAttribute(contextKey) == value, chainTrue)
    }

    def doIf(contextKey: String, value: String, chainTrue: B, chainFalse: B): B = {
      doIf((c: Context) => c.getAttribute(contextKey) == value, chainTrue, chainFalse)
    }

    private def doIf(testFunction: Context => Boolean, chainTrue: B, chainFalse: Option[B]): B = {
      logger.debug("Adding IfAction")
      newInstance(name, (ifActionBuilder withTestFunction testFunction withNextTrue chainTrue withNextFalse chainFalse) :: actionBuilders, next)
    }

    def insertChain(chain: B): B = {
      newInstance(name, chain.actionsList ::: actionBuilders, next)
    }

    def iterate(times: Int, chain: B): B = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions: List[AbstractActionBuilder] = Nil
      for (i <- 1 to times) {
        iteratedActions = chainActions ::: iteratedActions
      }
      logger.debug("Adding {} Iterations", times)
      newInstance(name, iteratedActions ::: actionBuilders, next)
    }

    def end(latch: CountDownLatch): B = {
      logger.debug("Adding EndAction")
      newInstance(name, endActionBuilder(latch) :: actionBuilders, next)
    }

    def build(scenarioId: Int): Action = {
      var previousInList: Action = next.getOrElse(null)
      for (actionBuilder <- actionBuilders) {
        previousInList = actionBuilder withNext (previousInList) build (scenarioId)
      }
      previousInList
    }

    def withNext(next: Action) = newInstance(name, actionBuilders, Some(next))
  }
}