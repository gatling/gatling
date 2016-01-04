/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.header

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._
import io.gatling.http.response.Response

abstract class HttpHeaderExtractor[X] extends CriterionExtractor[Response, String, X] { val criterionName = "header" }

class SingleHttpHeaderExtractor(val criterion: String, val occurrence: Int) extends HttpHeaderExtractor[String] with FindArity {

  def extract(prepared: Response): Validation[Option[String]] =
    prepared.headers(criterion).lift(occurrence).success
}

class MultipleHttpHeaderExtractor(val criterion: String) extends HttpHeaderExtractor[Seq[String]] with FindAllArity {

  def extract(prepared: Response): Validation[Option[Seq[String]]] =
    prepared.headers(criterion).liftSeqOption.success
}

class CountHttpHeaderExtractor(val criterion: String) extends HttpHeaderExtractor[Int] with CountArity {

  def extract(prepared: Response): Validation[Option[Int]] =
    prepared.headers(criterion).liftSeqOption.map(_.size).success
}
