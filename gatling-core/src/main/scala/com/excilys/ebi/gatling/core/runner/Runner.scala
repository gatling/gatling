package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging

abstract class Runner(val scenario: Action, val numberOfUsers: Integer, val rampTime: Option[Integer]) extends Logging {
  def run()
}