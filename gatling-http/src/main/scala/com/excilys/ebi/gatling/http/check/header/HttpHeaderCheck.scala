/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.check.header

import com.excilys.ebi.gatling.core.check.CheckStrategy
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.check.header.extractor.HttpHeaderExtractorFactory
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived

/**
 * This class represents a check made on the headers of the response
 *
 * @param what the function returning the name of the header to be checked
 * @param to the optional session key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpHeaderCheck(what: Session => String, strategy: CheckStrategy, expected: List[Session => String], saveAs: Option[String])
	extends HttpCheck(what, HttpHeaderExtractorFactory, strategy, expected, saveAs, CompletePageReceived)