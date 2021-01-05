/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.core.check._
import io.gatling.http.response.Response

object HttpHeaderExtractors {
  def find(headerName: CharSequence, occurrence: Int): FindCriterionExtractor[Response, CharSequence, String] =
    new FindCriterionExtractor[Response, CharSequence, String]("header", headerName, occurrence, _.headers(headerName).lift(occurrence).success)

  def findAll(headerName: CharSequence): FindAllCriterionExtractor[Response, CharSequence, String] =
    new FindAllCriterionExtractor[Response, CharSequence, String]("header", headerName, _.headers(headerName).liftSeqOption.success)

  def count(headerName: CharSequence): CountCriterionExtractor[Response, CharSequence] =
    new CountCriterionExtractor[Response, CharSequence]("header", headerName, _.headers(headerName).liftSeqOption.map(_.size).success)
}
