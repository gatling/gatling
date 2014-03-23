/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check

import io.gatling.core.check.{ Check, CheckFactory, Preparer }
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.check.HttpCheckOrder.{ Body, Checksum, Header, HttpCheckOrder, Status, Url }
import io.gatling.http.response.{ ByteArrayResponseBodyUsageStrategy, InputStreamResponseBodyUsageStrategy, Response, ResponseBodyUsageStrategy, StringResponseBodyUsageStrategy }

object HttpCheckBuilders {

  private def httpCheckFactory(order: HttpCheckOrder, responseBodyUsageStrategy: Option[ResponseBodyUsageStrategy]): CheckFactory[HttpCheck, Response] =
    (wrapped: Check[Response]) => HttpCheck(wrapped, order, responseBodyUsageStrategy)

  val statusCheckFactory = httpCheckFactory(Status, None)
  val urlCheckFactory = httpCheckFactory(Url, None)
  val checksumCheckFactory = httpCheckFactory(Checksum, None)
  val headerCheckFactory = httpCheckFactory(Header, None)
  def bodyCheckFactory(responseBodyUsageStrategy: ResponseBodyUsageStrategy) = httpCheckFactory(Body, Some(responseBodyUsageStrategy))
  val stringBodyCheckFactory = bodyCheckFactory(StringResponseBodyUsageStrategy)
  val streamBodyCheckFactory = bodyCheckFactory(InputStreamResponseBodyUsageStrategy)
  val bytesBodyCheckFactory = bodyCheckFactory(ByteArrayResponseBodyUsageStrategy)
  val timeCheckFactory = httpCheckFactory(Body, None)

  val passThroughResponsePreparer: Preparer[Response, Response] = (r: Response) => r.success
  val responseBodyStringPreparer: Preparer[Response, String] = (response: Response) => response.body.string().success
}
