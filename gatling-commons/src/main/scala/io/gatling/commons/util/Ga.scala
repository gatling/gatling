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

package io.gatling.commons.util

import java.io.BufferedInputStream
import java.net.{ URL, URLEncoder }
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

import scala.concurrent._
import scala.util.Properties._
import scala.util.Using

object Ga {

  private[this] def encode(string: String) = URLEncoder.encode(string, UTF_8.name)

  @SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
  def send(version: String): Unit = {
    import ExecutionContext.Implicits.global

    val trackingId = if (version.endsWith("SNAPSHOT")) "UA-53375088-4" else "UA-53375088-5"

    val bodyBytes =
      s"""tid=$trackingId&dl=${encode("https://gatling.io/" + version)}&de=UTF-8&ul=en-US&t=pageview&v=1&dt=${encode(version)}&cid=${encode(
        UUID.randomUUID.toString
      )}"""
        .getBytes(UTF_8)

    val url = new URL("https://ssl.google-analytics.com/collect")

    Future {
      blocking {
        val conn = url.openConnection().asInstanceOf[HttpsURLConnection]

        try {
          conn.setReadTimeout(1000)
          conn.setConnectTimeout(1000)
          conn.setDoInput(true)
          conn.setDoOutput(true)
          conn.setUseCaches(false)
          conn.setRequestMethod("POST")
          conn.setRequestProperty("Accept", "*/*")
          conn.setRequestProperty("Connection", "Close")
          conn.setRequestProperty("Content-Length", bodyBytes.length.toString)
          conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
          conn.setRequestProperty("Host", "ssl.google-analytics.com")
          conn.setRequestProperty("User-Agent", s"java/$javaVersion")

          Using.resource(conn.getOutputStream) { os =>
            os.write(bodyBytes)
            os.flush()

            // get response before closing
            Using.resource(new BufferedInputStream(conn.getInputStream)) { rd =>
              var byte: Int = -1
              do {
                byte = rd.read
              } while (byte != -1)
            }
          }
        } finally {
          conn.disconnect()
        }
      }
    }
  }
}
