package com.excilys.ebi.gatling.http.processor

import com.excilys.ebi.gatling.core.processor.Processor

import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.context.HttpScope

class HttpProcessor(val httpPhase: HttpPhase) extends Processor {
  def getHttpPhase = httpPhase
}