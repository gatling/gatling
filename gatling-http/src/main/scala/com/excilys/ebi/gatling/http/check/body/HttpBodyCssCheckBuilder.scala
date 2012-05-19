package com.excilys.ebi.gatling.http.check.body
import com.excilys.ebi.gatling.core.check.CheckContext.getOrUpdateCheckContextAttribute
import com.excilys.ebi.gatling.core.check.extractor.jsonpath.JsonPathExtractor
import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.ning.http.client.Response
import com.excilys.ebi.gatling.core.check.extractor.css.CssExtractor


object HttpBodyCssCheckBuilder {

	def css(expression: EvaluatableString) = new HttpBodyCheckBuilder(findExtractorFactory, findAllExtractorFactory, countExtractorFactory, expression)

	private val HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY = "HttpBodyCssExtractor"

	private def getCachedExtractor(response: Response) = getOrUpdateCheckContextAttribute(HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY, new CssExtractor(response.getResponseBody))

	private def findExtractorFactory(occurrence: Int): ExtractorFactory[Response, String] = (response: Response) => getCachedExtractor(response).extractOne(occurrence)

	private val findAllExtractorFactory: ExtractorFactory[Response, Seq[String]] = (response: Response) => getCachedExtractor(response).extractMultiple

	private val countExtractorFactory: ExtractorFactory[Response, Int] = (response: Response) => getCachedExtractor(response).count
}
	
