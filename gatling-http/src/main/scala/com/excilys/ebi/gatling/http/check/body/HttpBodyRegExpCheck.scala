/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.check.body

import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.check.body.extractor.HttpBodyRegExpExtractorFactory
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived

/**
 * This class represents a check made on the body of the response with regular expressions
 *
 * @param what the function returning the regular expression
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpBodyRegExpCheck(what: Context => String, occurrence: Int, strategy: CheckStrategy, expected: Option[String], saveAs: Option[String])
	extends HttpCheck(what, new HttpBodyRegExpExtractorFactory(occurrence), strategy, expected, saveAs, CompletePageReceived)