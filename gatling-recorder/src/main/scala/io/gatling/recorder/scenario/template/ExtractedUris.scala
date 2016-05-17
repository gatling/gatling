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
package io.gatling.recorder.scenario.template

import java.net.URL

import io.gatling.commons.util.StringHelper._
import io.gatling.recorder.scenario.{ RequestElement, ScenarioElement }

import com.dongxiguo.fastring.Fastring.Implicits._

private[scenario] case class Value(name: String, value: String)

private[scenario] case class SchemeHost(scheme: String, host: String)

/**
 * Extracts common URIs parts into vals. The algorithm is the following:
 *
 * group by (scheme, authority)
 * inside a group:
 *    if (count > 1) use the longer common root
 *    else use the (scheme, authority)
 * if multiple roots have the same host but different schemes/ports, create a val for the hos
 *
 * @param scenarioElements - contains uris to extracts common parts from
 */
private[scenario] class ExtractedUris(scenarioElements: Seq[ScenarioElement]) {
  var values: List[Value] = Nil

  private val renders: Map[String, Fastring] = {
    val requestElements = scenarioElements.collect { case elem: RequestElement => elem }
    val uris = requestElements.map(_.uri) ++
      requestElements.map(_.embeddedResources).reduce(_ ++ _).map(_.url) ++
      requestElements.map(_.nonEmbeddedResources).reduce(_ ++ _).map(_.uri)
    val urls = uris.map(uri => new URL(uri)).toList
    val urlGroups: Map[String, List[URL]] = urls.groupBy(url => url.getHost)

    val maxNbDigits = urlGroups.size.toString.length

    urlGroups.zipWithIndex.flatMap {
      case ((_, urls), index) =>

        val valName = "uri" + (index + 1).toString.leftPad(maxNbDigits, "0")
        if (urls.size == 1 || schemesPortAreSame(urls)) {
          val paths = urls.map(url => url.getPath)
          val longestCommonPath = longestCommonRoot(paths)

          val firstUrl = urls.head
          values = new Value(valName, fast"${protocol(firstUrl)}${firstUrl.getAuthority}$longestCommonPath".toString) :: values

          extractLongestPathUrls(urls, longestCommonPath, valName)
        } else {
          values = new Value(valName, urls.head.getHost) :: values

          extractCommonHostUrls(urls, valName)
        }
    }
  }

  private def extractCommonHostUrls(urls: List[URL], valName: String): List[(String, Fastring)] =
    urls.map(url =>
      (url.toString, fast""""${protocol(url)}${user(url)}" + $valName + ${value(s"${port(url)}${url.getPath}${query(url)}")}"""))

  private def extractLongestPathUrls(urls: List[URL], longestCommonPath: String, valName: String): List[(String, Fastring)] =
    urls.map(url => {
      val restPath = url.getPath.substring(longestCommonPath.length)
      (url.toString, fast"$valName + ${value(s"$restPath${query(url)}")}")
    })

  private def longestCommonRoot(pathsStrs: List[String]): String = {
      def longestCommonRoot2(sa1: Array[String], sa2: Array[String]) = {
        val minLen = math.min(sa1.length, sa2.length)
        var p = 0
        while (p < minLen && sa1(p) == sa2(p)) {
          p += 1
        }

        sa1.slice(0, p)
      }

    val paths = pathsStrs.map(_.split("/"))
    paths.reduce(longestCommonRoot2).toSeq.mkString("/")
  }

  private def schemesPortAreSame(urlUris: Seq[URL]): Boolean = {
    val firstUrl = urlUris.head
    urlUris.tail.forall(url => url.getPort == firstUrl.getPort && url.getProtocol == firstUrl.getProtocol)
  }

  private def value(str: String) = fast"${protectWithTripleQuotes(str)}"

  private def query(url: URL): Fastring =
    if (url.getQuery == null) EmptyFastring else fast"?${url.getQuery}"

  private def protocol(url: URL): Fastring =
    fast"${url.getProtocol}://"

  private def user(url: URL): Fastring =
    if (url.getUserInfo == null) EmptyFastring else fast"${url.getUserInfo}@"

  private def port(url: URL): Fastring =
    if (url.getPort < 0) EmptyFastring else fast":${url.getPort}"

  def vals: List[Value] = values

  def renderUri(uri: String): Fastring =
    if (renders.contains(uri)) renders(uri) else fast"$uri"
}
