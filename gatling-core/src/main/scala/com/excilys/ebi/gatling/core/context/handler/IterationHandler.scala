package com.excilys.ebi.gatling.core.context.handler
import com.excilys.ebi.gatling.core.log.Logging
import scala.collection.immutable.Stack
import com.excilys.ebi.gatling.core.context.Context

abstract trait IterationHandler extends Logging {

	def init(context: Context, uuid: String, userDefinedName: Option[String]) = {}

	def increment(c: Context, uuid: String, userDefinedName: Option[String]) = {}

	def expire(c: Context, uuid: String, userDefinedName: Option[String]) = {}

}