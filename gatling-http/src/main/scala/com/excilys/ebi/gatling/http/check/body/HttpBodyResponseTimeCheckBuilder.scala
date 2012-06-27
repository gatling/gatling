package com.excilys.ebi.gatling.http.check.body

import com.excilys.ebi.gatling.core.check.{ MatcherCheckBuilder, ExtractorFactory }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.response.ExtendedResponse
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived

/**
 * HttpBodyesponseTimeCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpBodyResponseTimeCheckBuilder {

	def responseTimeInMillis = new HttpBodyResponseTimeCheckBuilder(findExtendedResponseTimeExtractorFactory)
	def latencyInMillis = new HttpBodyResponseTimeCheckBuilder(findLatencyExtractorFactory)

	private val findExtendedResponseTimeExtractorFactory: ExtractorFactory[ExtendedResponse, String, Long] = (response: ExtendedResponse) => (expression: String) => Some(response.reponseTimeInMillis)

	private val findLatencyExtractorFactory: ExtractorFactory[ExtendedResponse, String, Long] = (response: ExtendedResponse) => (expression: String) => Some(response.latencyInMillis)
}

/**
 * This class builds a response time check
 */
class HttpBodyResponseTimeCheckBuilder(factory: ExtractorFactory[ExtendedResponse, String, Long]) extends HttpExtractorCheckBuilder[Long, String](Session => EMPTY, CompletePageReceived) {

	def find = new MatcherCheckBuilder[HttpCheck[String], ExtendedResponse, String, Long](httpCheckBuilderFactory, factory)
}