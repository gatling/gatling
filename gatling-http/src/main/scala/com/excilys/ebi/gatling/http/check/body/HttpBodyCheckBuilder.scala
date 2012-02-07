package com.excilys.ebi.gatling.http.check.body
import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.CheckOneBuilder
import com.excilys.ebi.gatling.core.check.CheckMultipleBuilder
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.check.{ HttpMultipleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.Response

/**
 * This class builds a response body check based on regular expressions
 *
 * @param findExtractorFactory the extractor factory for find
 * @param findAllExtractoryFactory the extractor factory for findAll
 * @param countExtractoryFactory the extractor factory for count
 * @param expression the function returning the expression representing expression is to be checked
 */
class HttpBodyCheckBuilder(findExtractorFactory: Int => Response => String => Option[String],
		findAllExtractoryFactory: Response => String => Option[List[String]],
		countExtractoryFactory: Response => String => Option[Int],
		expression: Session => String) extends HttpMultipleCheckBuilder[String](expression, CompletePageReceived) {

	def find: CheckOneBuilder[HttpCheck[String], Response, String] = find(0)

	def find(occurrence: Int) = new CheckOneBuilder(checkBuildFunction, findExtractorFactory(occurrence))

	def findAll = new CheckMultipleBuilder(checkBuildFunction, findAllExtractoryFactory)

	def count = new CheckOneBuilder(checkBuildFunction, countExtractoryFactory)
}