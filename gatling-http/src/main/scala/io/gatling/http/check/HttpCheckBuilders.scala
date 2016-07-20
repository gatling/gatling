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
package io.gatling.http.check

import io.gatling.commons.validation._
import io.gatling.core.check.{ Check, Extender, Preparer }
import io.gatling.http.check.HttpCheckScope._
import io.gatling.http.response._

object HttpCheckBuilders {

  private def extender(target: HttpCheckScope, responseBodyUsageStrategy: Option[ResponseBodyUsageStrategy]): Extender[HttpCheck, Response] =
    (wrapped: Check[Response]) => HttpCheck(wrapped, target, responseBodyUsageStrategy)

  val StatusExtender = extender(Status, None)
  val UrlExtender = extender(Url, None)
  val ChecksumExtender = extender(Checksum, None)
  val HeaderExtender = extender(Header, None)
  def bodyExtender(responseBodyUsageStrategy: ResponseBodyUsageStrategy) = extender(Body, Some(responseBodyUsageStrategy))
  val StringBodyExtender = bodyExtender(StringResponseBodyUsageStrategy)
  val StreamBodyExtender = bodyExtender(InputStreamResponseBodyUsageStrategy)
  val BytesBodyExtender = bodyExtender(ByteArrayResponseBodyUsageStrategy)
  val TimeExtender = extender(Body, None)

  val PassThroughResponsePreparer: Preparer[Response, Response] = _.success
  val ResponseBodyStringPreparer: Preparer[Response, String] = _.body.string.success
  val ResponseBodyBytesPreparer: Preparer[Response, Array[Byte]] = _.body.bytes.success
  val UrlStringPreparer: Preparer[Response, String] = _.request.getUrl.success
}
