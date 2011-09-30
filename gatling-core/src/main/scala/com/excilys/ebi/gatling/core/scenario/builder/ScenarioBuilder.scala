package com.excilys.ebi.gatling.core.scenario.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.IfActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.context.Context

object ScenarioBuilder {
  private var expectedExecutionDuration: Map[Int, Long] = Map.empty
  private var numberOfRelevantActionsByScenario: Map[Int, Int] = Map.empty

  var currentGroups: List[String] = Nil

  def addToExecutionTime(scenarioId: Int, timeValue: Long, timeUnit: TimeUnit) = {
    expectedExecutionDuration += (scenarioId -> (TimeUnit.MILLISECONDS.convert(timeValue, timeUnit) + expectedExecutionDuration.get(scenarioId).getOrElse(0L)))
  }

  def getExecutionTime(scenarioId: Int) = TimeUnit.SECONDS.convert(expectedExecutionDuration.get(scenarioId).get, TimeUnit.MILLISECONDS)

  abstract class ScenarioBuilder[B <: ScenarioBuilder[B]](name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action], groups: Option[List[String]]) extends AbstractActionBuilder {

    def actionsList = actionBuilders
    def getName = name

    def newInstance(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action], groups: Option[List[String]]): B

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
      newInstance(name, (pauseActionBuilder withMinDuration delayMinValue withMaxDuration delayMaxValue withTimeUnit delayUnit) :: actionBuilders, next, groups)
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
      newInstance(name, (ifActionBuilder withTestFunction testFunction withNextTrue chainTrue withNextFalse chainFalse inGroups groups.get) :: actionBuilders, next, groups)
    }

    def doWhile(contextKey: String, value: String, chain: B): B = {
      doWhile((c: Context) => c.getAttribute(contextKey) == value, chain)
    }

    def doFor(durationValue: Int, chain: B): B = {
      doFor(durationValue, TimeUnit.SECONDS, chain)
    }

    def doFor(durationValue: Int, durationUnit: TimeUnit, chain: B): B = {
      doWhile((c: Context) => c.getWhileDuration <= durationUnit.toMillis(durationValue), chain)
    }

    def doWhile(testFunction: Context => Boolean, chain: B): B = {
      logger.debug("Adding While Action")
      newInstance(name, (whileActionBuilder withTestFunction testFunction withNextTrue chain inGroups groups.get) :: actionBuilders, next, groups)
    }

    def startGroup(groupName: String): B = newInstance(name, startGroupBuilder(groupName) :: actionBuilders, next, groups)

    def endGroup(groupName: String): B = newInstance(name, endGroupBuilder(groupName) :: actionBuilders, next, groups)

    def insertChain(chain: B): B = {
      newInstance(name, chain.actionsList ::: actionBuilders, next, groups)
    }

    def iterate(times: Int, chain: B): B = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions: List[AbstractActionBuilder] = Nil
      for (i <- 1 to times) {
        iteratedActions = chainActions ::: iteratedActions
      }
      logger.debug("Adding {} Iterations", times)
      newInstance(name, iteratedActions ::: actionBuilders, next, groups)
    }

    def end(latch: CountDownLatch): B = {
      logger.debug("Adding EndAction")
      newInstance(name, endActionBuilder(latch) :: actionBuilders, next, groups)
    }

    def build(scenarioId: Int): Action = {
      var previousInList: Action = next.getOrElse(null)
      for (actionBuilder <- actionBuilders) {
        actionBuilder match {
          case b: GroupActionBuilder =>
            currentGroups =
              if (b.isEnd)
                currentGroups filterNot (_ == b.getName)
              else
                b.getName :: currentGroups
          case _ =>
            previousInList = actionBuilder withNext previousInList inGroups currentGroups build (scenarioId)
        }

      }
      previousInList
    }

    def withNext(next: Action) = newInstance(name, actionBuilders, Some(next), groups)

    def inGroups(groups: List[String]) = newInstance(name, actionBuilders, next, Some(groups))
  }
}