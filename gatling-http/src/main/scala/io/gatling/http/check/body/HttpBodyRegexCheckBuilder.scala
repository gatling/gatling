/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.body

import io.gatling.core.check.extractor.regex.{ GroupExtractor, RegexExtractors }
import io.gatling.core.session.Expression
import io.gatling.http.check.{ HttpCheckBuilders, HttpMultipleCheckBuilder }

object HttpBodyRegexCheckBuilder {

	def regex(expression: Expression[String]) = genericRegex[String](expression)
	def regexCapture2(expression: Expression[String]) = genericRegex[(String, String)](expression)
	def regexCapture3(expression: Expression[String]) = genericRegex[(String, String, String)](expression)
	def regexCapture4(expression: Expression[String]) = genericRegex[(String, String, String, String)](expression)

	def genericRegex[X](expression: Expression[String])(implicit groupExtractor: GroupExtractor[X]) = new HttpMultipleCheckBuilder[String, String, X](
		HttpCheckBuilders.bodyCheckFactory,
		HttpCheckBuilders.stringResponsePreparer,
		RegexExtractors.extractOne,
		RegexExtractors.extractMultiple,
		RegexExtractors.count,
		expression)
}
