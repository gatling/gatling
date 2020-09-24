/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import java.io.InputStream
import java.util.zip.GZIPOutputStream

import io.gatling.commons.util.Io._

object GzipHelper {

  def gzip(string: String): Array[Byte] = gzip(string.getBytes)

  def gzip(bytes: Array[Byte]): Array[Byte] =
    gzip(new FastByteArrayInputStream(bytes))

  def gzip(in: InputStream): Array[Byte] =
    withCloseable(in) { is =>
      val out = FastByteArrayOutputStream.pooled()
      withCloseable(new GZIPOutputStream(out))(is.copyTo(_))
      out.toByteArray
    }
}
