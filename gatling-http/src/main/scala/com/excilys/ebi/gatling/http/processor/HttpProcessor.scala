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
package com.excilys.ebi.gatling.http.processor

import com.excilys.ebi.gatling.core.processor.Processor
import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.ning.http.client.Response

abstract class HttpProcessor(val httpPhase: HttpPhase, providerType : ProviderType[Response]) extends Processor {

	// FIXME is this still useful?
	def getHttpPhase = httpPhase

	// FIXME shouldn't this be in Processor ?
	def getProviderType: ProviderType[Response] = providerType

	override def toString = this.getClass().getSimpleName()
}