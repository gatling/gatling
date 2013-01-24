/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.request.builder

import com.excilys.ebi.gatling.core.session.Expression

object HeadHttpRequestBuilder {

	def apply(requestName: Expression[String], url: Expression[String]) = new HeadHttpRequestBuilder(HttpAttributes(requestName, "HEAD", url))
}

/**
 * This class defines an HTTP request with word HEAD in the DSL
 */
class HeadHttpRequestBuilder(httpAttributes: HttpAttributes) extends AbstractHttpRequestBuilder[HeadHttpRequestBuilder](httpAttributes) {

	private[http] def newInstance(httpAttributes: HttpAttributes) = new HeadHttpRequestBuilder(httpAttributes)
}
