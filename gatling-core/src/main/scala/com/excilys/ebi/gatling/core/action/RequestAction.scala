package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.processor.Processor

abstract class RequestAction(next: Action, request: AbstractRequest, givenProcessors: Option[List[Processor]]) extends Action {
  def execute(context: Context)
}