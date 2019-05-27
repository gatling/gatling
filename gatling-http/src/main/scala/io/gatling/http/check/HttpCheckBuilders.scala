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

package io.gatling.http.check

import io.gatling.commons.validation._
import io.gatling.core.check.{ Preparer, Specializer }
import io.gatling.http.check.HttpCheckScope._
import io.gatling.http.response._

object HttpCheckBuilders {

  private def specializer(target: HttpCheckScope, responseBodyUsage: Option[ResponseBodyUsage]): Specializer[HttpCheck, Response] =
    HttpCheck(_, target, responseBodyUsage)

  val StatusSpecializer: Specializer[HttpCheck, Response] = specializer(Status, None)
  val UrlSpecializer: Specializer[HttpCheck, Response] = specializer(Url, None)
  val HeaderSpecializer: Specializer[HttpCheck, Response] = specializer(Header, None)
  private def bodySpecializer(responseBodyUsage: ResponseBodyUsage): Specializer[HttpCheck, Response] = specializer(Body, Some(responseBodyUsage))
  val StringBodySpecializer: Specializer[HttpCheck, Response] = bodySpecializer(StringResponseBodyUsage)
  val CharArrayBodySpecializer: Specializer[HttpCheck, Response] = bodySpecializer(CharArrayResponseBodyUsage)
  val StreamBodySpecializer: Specializer[HttpCheck, Response] = bodySpecializer(InputStreamResponseBodyUsage)
  val BytesBodySpecializer: Specializer[HttpCheck, Response] = bodySpecializer(ByteArrayResponseBodyUsage)
  val TimeSpecializer: Specializer[HttpCheck, Response] = specializer(Body, None)

  val PassThroughResponsePreparer: Preparer[Response, Response] = _.success
  val ResponseBodyStringPreparer: Preparer[Response, String] = _.body.string.success
  val ResponseBodyBytesPreparer: Preparer[Response, Array[Byte]] = _.body.bytes.success
  val UrlStringPreparer: Preparer[Response, String] = _.request.getUri.toFullUrl.success
}
