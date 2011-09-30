package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging

trait AbstractActionBuilder extends Logging {
  def build(scenarioId: Int): Action
  def withNext(next: Action): AbstractActionBuilder
  def inGroups(groups: List[String]): AbstractActionBuilder
}