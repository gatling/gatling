package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.action.Action

abstract class Runner(val scenario: Action, val numberOfUsers: Integer, val rampTime: Option[Integer], val numberOfRelevantActions: Integer) {
  def run()
}