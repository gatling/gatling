package com.excilys.ebi.gatling.core.processor.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.processor.Processor

trait ProcessorBuilder extends Logging {
  def build: Processor
}