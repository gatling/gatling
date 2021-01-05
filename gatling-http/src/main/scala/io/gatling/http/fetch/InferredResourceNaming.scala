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

package io.gatling.http.fetch

import io.gatling.http.client.uri.Uri

private[http] object InferredResourceNaming {

  val AbsoluteUrlInferredResourceNaming: Uri => String = _.toString

  val RelativeUrlInferredResourceNaming: Uri => String = _.toRelativeUrl

  val PathInferredResourceNaming: Uri => String = _.getPath

  val LastPathElementInferredResourceNaming: Uri => String = _.getPath match {
    case "/" | "" => "/"
    case path if path.endsWith("/") =>
      path.substring(0, path.length - 1).lastIndexOf('/') match {
        case -1 => path
        case i  => path.substring(i + 1, path.length)
      }
    case path =>
      path.lastIndexOf('/') match {
        case -1 => path
        case i  => path.substring(i + 1)
      }
  }

  val UrlTailInferredResourceNaming: Uri => String = uri => {
    val url = uri.toUrl
    val start = url.lastIndexOf('/') + 1
    if (start < url.length)
      url.substring(start, url.length)
    else
      "/"
  }
}
