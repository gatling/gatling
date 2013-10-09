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
package io.gatling.http.request.builder

import com.ning.http.client.RequestBuilder
import com.ning.http.multipart.StringPart

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.util.HttpHelper

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param body the body that should be added to the request
 * @param params the parameters that should be added to the request
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](
	httpAttributes: HttpAttributes,
	bodyAttributes: BodyAttributes,
	params: List[HttpParam])
	extends AbstractHttpRequestWithBodyBuilder[B](httpAttributes, bodyAttributes) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param params the parameters that should be added to the request
	 * @param body the body that should be added to the request
	 * @param paramsAttributes the attributes for requests with HTTP params
	 */
	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		bodyAttributes: BodyAttributes,
		params: List[HttpParam]): B

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		bodyAttributes: BodyAttributes): B = {
		newInstance(httpAttributes, bodyAttributes, params)
	}

	def param(key: Expression[String], value: Expression[Any]): B = param(SimpleParam(key, value))
	def multivaluedParam(key: Expression[String], values: Expression[Seq[Any]]): B = param(MultivaluedParam(key, values))
	def paramsSequence(seq: Expression[Seq[(String, Any)]]): B = param(ParamSeq(seq))
	def paramsMap(map: Expression[Map[String, Any]]): B = param(ParamMap(map))
	private def param(param: HttpParam): B = newInstance(httpAttributes, bodyAttributes, param :: params)

	override protected def configureParts(session: Session, requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

		def configureAsParams: Validation[RequestBuilder] =
			if (!params.isEmpty)
				// As a side effect, requestBuilder.setParameters() resets the body data, so, it should not be called with empty parameters 
				HttpHelper.httpParamsToFluentMap(params, session).map(requestBuilder.setParameters)
			else
				requestBuilder.success

		def configureAsStringParts: Validation[RequestBuilder] =
			HttpHelper.resolveParams(params, session).map { params =>
				for {
					(key, values) <- params
					value <- values
				} requestBuilder.addBodyPart(new StringPart(key, value))

				requestBuilder
			}

		val requestBuilderWithParams =
			if (bodyAttributes.bodyParts.isEmpty) configureAsParams
			else configureAsStringParts

		requestBuilderWithParams.flatMap(super.configureParts(session, _))
	}
}

object HttpRequestWithBodyAndParamsBuilder {

	def apply(method: String, requestName: Expression[String], url: Expression[String]) = new HttpRequestWithBodyAndParamsBuilder(HttpAttributes(requestName, method, url), BodyAttributes(), Nil)
}

class HttpRequestWithBodyAndParamsBuilder(
	httpAttributes: HttpAttributes,
	bodyAttributes: BodyAttributes,
	params: List[HttpParam])
	extends AbstractHttpRequestWithBodyAndParamsBuilder[HttpRequestWithBodyAndParamsBuilder](httpAttributes, bodyAttributes, params) {

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		bodyAttributes: BodyAttributes,
		params: List[HttpParam]) = new HttpRequestWithBodyAndParamsBuilder(httpAttributes, bodyAttributes, params)
}
