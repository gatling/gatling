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

package io.gatling.core.body

import io.gatling.commons.util.{ FastByteArrayInputStream, GzipHelper }

object BodyProcessors {

  def gzip: Body => ByteArrayBody =
    (body: Body) => {
      val gzippedBytes = body match {
        case StringBody(string, charset) => string.map(GzipHelper.gzip(_, charset))
        case ByteArrayBody(byteArray)    => byteArray.map(GzipHelper.gzip)
        case RawFileBody(resourceAndCachedBytes) =>
          resourceAndCachedBytes.map { case ResourceAndCachedBytes(resource, cachedBytes) =>
            cachedBytes match {
              case Some(bytes) => GzipHelper.gzip(bytes)
              case _           => GzipHelper.gzip(resource.inputStream)
            }
          }
        case InputStreamBody(inputStream) => inputStream.map(GzipHelper.gzip)
        case b: ElBody                    => b.asStream.map(GzipHelper.gzip)
      }

      ByteArrayBody(gzippedBytes)
    }

  def stream: Body => InputStreamBody =
    (body: Body) => {
      val stream = body match {
        case StringBody(string, charset) => string.map(s => new FastByteArrayInputStream(s.getBytes(charset)))
        case ByteArrayBody(byteArray)    => byteArray.map(new FastByteArrayInputStream(_))
        case RawFileBody(resourceAndCachedBytes) =>
          resourceAndCachedBytes.map { case ResourceAndCachedBytes(resource, cachedBytes) =>
            cachedBytes match {
              case Some(bytes) => new FastByteArrayInputStream(bytes)
              case _           => resource.inputStream
            }
          }
        case InputStreamBody(inputStream) => inputStream
        case b: ElBody                    => b.asStream
      }

      InputStreamBody(stream)
    }
}
