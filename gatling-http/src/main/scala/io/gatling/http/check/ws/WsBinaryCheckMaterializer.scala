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

package io.gatling.http.check.ws

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.bytes.BodyBytesCheckType

class WsBinaryCheckMaterializer[T, P](override val preparer: Preparer[Array[Byte], P])
    extends CheckMaterializer[T, WsBinaryCheck, Array[Byte], P](new WsBinaryCheck(_))

object WsBinaryCheckMaterializer {

  val BodyBytes: CheckMaterializer[BodyBytesCheckType, WsBinaryCheck, Array[Byte], Array[Byte]] =
    new WsBinaryCheckMaterializer[BodyBytesCheckType, Array[Byte]](identityPreparer)

  val BodyLength: CheckMaterializer[BodyBytesCheckType, WsBinaryCheck, Array[Byte], Int] =
    new WsBinaryCheckMaterializer[BodyBytesCheckType, Int](_.length.success)
}
