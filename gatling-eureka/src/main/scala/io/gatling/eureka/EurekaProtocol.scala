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
package io.gatling.eureka

import scala.io.Codec
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import java.io.BufferedInputStream
import scala.io.Source
import scala.language.reflectiveCalls
import java.net.HttpURLConnection
import java.net.URL
import xml.{ NodeSeq, Elem }

/**
 * Netflix/Eureka Gatling integration.
 *
 * @autor diegopacheco
 *
 */
class EurekaProtocolBuilder(url: String, appName: String) {

  implicit def closingSource(source: Source) = new {
    val lines = source.getLines()
    var isOpen = true
    def closeAfterGetLines() = new Iterator[String] {
      def hasNext = isOpen && hasNextAndCloseIfDone
      def next() = {
        val line = lines.next()
        hasNextAndCloseIfDone
        line
      }
      private def hasNextAndCloseIfDone = if (lines.hasNext) true else { source.close(); isOpen = false; false }
    }
  }

  def ip(): String = {
    val eurekaRequestURL: String = url + "/v2/apps/" + appName
    println("This code runs: " + eurekaRequestURL)

    val u = new URL(eurekaRequestURL);
    val uc = u.openConnection();
    val gzInputStream = new GZIPInputStream(new BufferedInputStream(uc.getInputStream))
    val eurekaXMLResponse = Source.fromInputStream(gzInputStream).closeAfterGetLines().mkString
    val eurekaXML = scala.xml.XML.loadString(eurekaXMLResponse)
    println("Response xml: " + eurekaXML)

    val ip: String = (eurekaXML \ "instance" \ "ipAddr").text
    return ip

  }

}

object MainAppTest extends App {
  val ip = new EurekaProtocolBuilder("http://127.0.0.1:8080/eureka", "EUREKA").ip()
  println("IP: " + ip)
}

