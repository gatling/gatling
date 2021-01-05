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

package io.gatling.core.check.checksum

import java.util.Locale

import io.gatling.commons.validation._
import io.gatling.core.check.{ DefaultFindCheckBuilder, FindCheckBuilder, FindExtractor }
import io.gatling.core.session._

trait Md5CheckType
trait Sha1CheckType

object ChecksumCheckBuilder {

  private def checksum[T](algorithm: String): FindCheckBuilder[T, String, String] =
    new DefaultFindCheckBuilder[T, String, String](
      extractor = new FindExtractor[String, String](algorithm.toLowerCase(Locale.ROOT), Some(_).success).expressionSuccess,
      displayActualValue = false
    )

  val Md5: FindCheckBuilder[Md5CheckType, String, String] = checksum[Md5CheckType]("MD5")

  val Sha1: FindCheckBuilder[Sha1CheckType, String, String] = checksum[Sha1CheckType]("SHA1")
}
