package com.excilys.ebi.gatling.core.processor.builder

import com.excilys.ebi.gatling.core.processor.Processor
import com.excilys.ebi.gatling.core.log.Logging

trait ProcessorBuilder extends Logging {
  def build: Processor
}