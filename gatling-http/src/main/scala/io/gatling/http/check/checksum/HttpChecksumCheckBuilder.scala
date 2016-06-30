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
import io.gatling.core.check._
import io.gatling.core.check.extractor._
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpMd5CheckType
trait HttpSha1CheckType

object HttpChecksumCheckBuilder {

  def httpChecksum[T](algorithm: String) = {
    val extractor = new Extractor[Response, String] with SingleArity {
      val name = algorithm
      def apply(prepared: Response) = prepared.checksum(algorithm).success
    }.expressionSuccess

    new DefaultFindCheckBuilder[T, Response, String](extractor)
  }

  val Md5 = httpChecksum[HttpMd5CheckType]("MD5")
  val Sha1 = httpChecksum[HttpSha1CheckType]("SHA1")
}

object HttpChecksumProvider {

  val Md5 = new HttpChecksumProvider[HttpMd5CheckType](wrapped => new ChecksumCheck("MD5", wrapped))
  val Sha1 = new HttpChecksumProvider[HttpSha1CheckType](wrapped => new ChecksumCheck("SHA1", wrapped))
}

class HttpChecksumProvider[T](override val extender: Extender[HttpCheck, Response]) extends CheckProtocolProvider[T, HttpCheck, Response, Response] {

  override val preparer: Preparer[Response, Response] = PassThroughResponsePreparer
}