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
package io.gatling.http.check.checksum

import io.gatling.commons.validation._
import io.gatling.core.check.{ DefaultFindCheckBuilder, Check }
import io.gatling.core.check.extractor._
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

object HttpChecksumCheckBuilder {

  def checksum(algorithm: String) = {

    val checksumCheckFactory = (wrapped: Check[Response]) => new ChecksumCheck(algorithm, wrapped)
    val extractor = new Extractor[Response, String] with SingleArity {
      val name = algorithm
      def apply(prepared: Response) = prepared.checksum(algorithm).success
    }.expressionSuccess

    new DefaultFindCheckBuilder[HttpCheck, Response, Response, String](
      checksumCheckFactory,
      PassThroughResponsePreparer,
      extractor
    )
  }

  val Md5 = checksum("MD5")
  val Sha1 = checksum("SHA1")
}
