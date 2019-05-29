/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

class HttpHeaderFindExtractor(headerName: String, occurrence: Int) extends FindCriterionExtractor[Response, String, String]("header", headerName, occurrence, _.headers(headerName).lift(occurrence).success)

class HttpHeaderFindAllExtractor(headerName: String) extends FindAllCriterionExtractor[Response, String, String]("header", headerName, _.headers(headerName).liftSeqOption.success)

class HttpHeaderCountExtractor(val headerName: String) extends CountCriterionExtractor[Response, String]("header", headerName, _.headers(headerName).liftSeqOption.map(_.size).success)
