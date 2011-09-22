package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.builder.ProcessorBuilder

abstract class RequestAction(next: Action, request: AbstractRequest, givenProcessors: Option[List[ProcessorBuilder]]) extends Action {
  def execute(context: Context)
}