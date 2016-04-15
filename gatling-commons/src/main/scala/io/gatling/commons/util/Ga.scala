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
package io.gatling.commons.util

import java.io.{ BufferedReader, InputStreamReader }
import java.net.{ URL, URLEncoder }
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Properties._
import scala.util.Success

import io.gatling.commons.util.Io._

object Ga {

  private[this] def encode(string: String) = URLEncoder.encode(string, UTF_8.name)

  def send(version: String): Unit = {
    import ExecutionContext.Implicits.global

    val whenConnected = Future {
      val url = new URL("https://ssl.google-analytics.com/collect")
      url.openConnection().asInstanceOf[HttpsURLConnection]
    }

    val trackingId = if (version.endsWith("SNAPSHOT")) "UA-53375088-4" else "UA-53375088-5"

    val body =
      s"""tid=$trackingId&dl=${encode("http://gatling.io/" + version)}&de=UTF-8&ul=en-US&t=pageview&v=1&dt=${encode(version)}&cid=${encode(UUID.randomUUID.toString)}"""

    val bytes = body.getBytes(UTF_8)

    whenConnected.map { conn =>
      conn.setReadTimeout(1000)
      conn.setConnectTimeout(1000)
      conn.setDoInput(true)
      conn.setDoOutput(true)
      conn.setUseCaches(false)
      conn.setRequestMethod("POST")
      conn.setRequestProperty("Accept", "*/*")
      conn.setRequestProperty("Connection", "Close")
      conn.setRequestProperty("Content-Length", Integer.toString(bytes.length))
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      conn.setRequestProperty("Host", "ssl.google-analytics.com")
      conn.setRequestProperty("User-Agent", s"java/$javaVersion")

      withCloseable(conn.getOutputStream) { os =>
        os.write(bytes)
        os.flush()
        os.close()

        // get response before closing
        val is = conn.getInputStream()
        val rd = new BufferedReader(new InputStreamReader(is))
        var line: String = rd.readLine()
        while (line != null) {
          line = rd.readLine()
        }
        rd.close()
      }
      conn
    }.recoverWith {
      case _ => whenConnected
    }.onComplete {
      case Success(conn) => conn.disconnect()
      case _             =>
    }
  }
}
