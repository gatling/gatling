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
package com.excilys.ebi.gatling.recorder.ui.info

import org.jboss.netty.handler.codec.http.HttpMessage
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class RequestInfo(val request: HttpRequest, val response: HttpResponse)	extends EventInfo {
	
	private def getHttpBody(message: HttpMessage) = {
		if(message.getContent.hasArray)
			new String(message.getContent.array)
		else
			EMPTY
	}
	
	val requestBody =  getHttpBody(request)
	
	val responseBody = getHttpBody(response) 
	
	override def toString = request.getMethod + " | " + request.getUri
}