package com.excilys.ebi.gatling.core.check.extractor

class MultiRegExpExtractor(textContent: String) extends Extractor {
	/**
	 * The actual extraction happens here. The regular expression is compiled and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the regular expression to be matched
	 * @return an option containing the value if found, None otherwise
	 */
	def extract(expression: String): List[String] = {
		logger.debug("Extracting with expression : {}", expression)

		expression.r.findAllIn(textContent).matchData.map { matcher =>
			new String(matcher.group(1 min matcher.groupCount))
		}.toList
	}
}