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

package io.gatling.http.check.checksum

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.checksum.{ Md5CheckType, Sha1CheckType }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckScope.Chunks
import io.gatling.http.response.Response

object HttpChecksumCheckMaterializer {
  val Md5: CheckMaterializer[Md5CheckType, HttpCheck, Response, String] = new HttpChecksumCheckMaterializer[Md5CheckType](ChecksumAlgorithm.Md5)
  val Sha1: CheckMaterializer[Sha1CheckType, HttpCheck, Response, String] = new HttpChecksumCheckMaterializer[Sha1CheckType](ChecksumAlgorithm.Sha1)
}

final class HttpChecksumCheckMaterializer[T](algorithm: ChecksumAlgorithm)
    extends CheckMaterializer[T, HttpCheck, Response, String](check => HttpCheck(new ChecksumCheck(check, algorithm), Chunks)) {
  override val preparer: Preparer[Response, String] = _.checksum(algorithm) match {
    case Some(chk) => chk.success
    case _         => s"$algorithm checksum wasn't computed".failure
  }
}
