package com.excilys.ebi.gatling.http.processor.builder

import com.excilys.ebi.gatling.core.processor.builder.ProcessorBuilder

import com.excilys.ebi.gatling.http.processor.HttpProcessor

trait HttpProcessorBuilder extends ProcessorBuilder {
	override def build: HttpProcessor
}