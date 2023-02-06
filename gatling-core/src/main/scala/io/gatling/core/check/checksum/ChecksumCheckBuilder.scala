/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.checksum

import io.gatling.commons.validation._
import io.gatling.core.check.{ CheckBuilder, ChecksumAlgorithm, FindExtractor }
import io.gatling.core.session._

sealed trait Md5CheckType
sealed trait Sha1CheckType

object ChecksumCheckBuilder {
  private def checksum[T](algorithm: ChecksumAlgorithm): CheckBuilder.Find[T, String, String] =
    new CheckBuilder.Find.Default[T, String, String](
      extractor = new FindExtractor[String, String](algorithm.name, Some(_).success).expressionSuccess,
      displayActualValue = false
    )

  val Md5: CheckBuilder.Find[Md5CheckType, String, String] = checksum[Md5CheckType](ChecksumAlgorithm.Md5)

  val Sha1: CheckBuilder.Find[Sha1CheckType, String, String] = checksum[Sha1CheckType](ChecksumAlgorithm.Sha1)
}
