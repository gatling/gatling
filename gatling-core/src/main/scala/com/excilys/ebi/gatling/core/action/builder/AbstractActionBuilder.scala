package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging

abstract class TRUE
abstract class FALSE

trait AbstractActionBuilder extends Logging {
  def build(scenarioId: Int): Action
  def withNext(next: Action): AbstractActionBuilder
}