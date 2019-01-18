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

package io.gatling.http.check.sse

import io.gatling.core.check._
import io.gatling.core.check.extractor.regex.RegexCheckType
import io.gatling.core.check.extractor.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

trait SseCheckSupport {

  implicit def checkBuilder2SseCheck[A, P, X](checkBuilder: CheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, SseCheck, String, P]): SseCheck =
    checkBuilder.build(materializer)

  implicit def validatorCheckBuilder2SseCheck[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, SseCheck, String, P]): SseCheck =
    validatorCheckBuilder.exists

  implicit def findCheckBuilder2SseCheck[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, SseCheck, String, P]): SseCheck =
    findCheckBuilder.find.exists

  implicit def sseJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers): SseJsonPathCheckMaterializer = new SseJsonPathCheckMaterializer(jsonParsers)

  implicit val sseRegexCheckMaterializer: CheckMaterializer[RegexCheckType, SseCheck, String, CharSequence] = SseRegexCheckMaterializer

  implicit val sseSubstringCheckMaterializer: CheckMaterializer[SubstringCheckType, SseCheck, String, String] = SseSubstringCheckMaterializer
}
