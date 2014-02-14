/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.breakOut

import com.ning.http.client.FluentStringsMap

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }

package object builder {

	implicit class HttpParams(val params: List[HttpParam]) extends AnyVal {

		def resolveParams(session: Session): Validation[Map[String, Seq[String]]] = {

			val resolvedParams = params.foldLeft(HttpParam.emptyParamListSuccess) { (resolvedParams, param) =>
				{

					val newParams: Validation[List[(String, String)]] = param match {
						case SimpleParam(key, value) =>
							for {
								key <- key(session)
								value <- value(session)
							} yield List(key -> value.toString)

						case MultivaluedParam(key, values) =>
							for {
								key <- key(session)
								values <- values(session)
							} yield values.map(key -> _.toString)(breakOut)

						case ParamSeq(seq) =>
							for {
								seq <- seq(session)
							} yield seq.map { case (key, value) => key -> value.toString }(breakOut)

						case ParamMap(map) =>
							for {
								map <- map(session)
							} yield map.map { case (key, value) => key -> value.toString }(breakOut)
					}

					for {
						newParams <- newParams
						resolvedParams <- resolvedParams
					} yield newParams ::: resolvedParams
				}
			}

			// group by name
			resolvedParams.map(_.groupBy(_._1).mapValues(_.map(_._2)))
		}

		def resolveFluentStringsMap(session: Session): Validation[FluentStringsMap] =
			resolveParams(session).map { params =>

				params.foldLeft(new FluentStringsMap) {
					case (fsm, (key, values)) => fsm.add(key, values)
				}
			}
	}
}
