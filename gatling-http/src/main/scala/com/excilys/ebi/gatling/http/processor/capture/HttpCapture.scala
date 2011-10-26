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
package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.core.provider.ProviderType

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpPhase._

abstract class HttpCapture(val expressionFormatter: Context => String, val attrKey: String, httpPhase: HttpPhase, val providerType: ProviderType)
		extends HttpProcessor(httpPhase) {

	def getAttrKey = attrKey

	def getProviderType = providerType
}
