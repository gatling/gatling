/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.dom

import java.net.URI

import scala.collection.JavaConversions._
import scala.collection.concurrent

import com.ning.http.client.Request
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.Expression
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Success, SuccessWrapper }
import io.gatling.http.action.HttpRequestAction
import io.gatling.http.ahc.HttpTx
import io.gatling.http.request.builder.HttpRequestBaseBuilder
import io.gatling.http.action.HttpRequestActionBuilder
import io.gatling.http.response.ResponseBuilder
import jodd.lagarto.dom.LagartoDOMBuilder
import jodd.lagarto.dom.NodeSelector
import org.jboss.netty.util.internal.ConcurrentHashMap

sealed trait ResourceFetched {
	def uri: URI
	def status: Status
}
case class RegularResourceFetched(uri: URI, status: Status) extends ResourceFetched
case class CssResourceFetched(uri: URI, status: Status, content: Option[String]) extends ResourceFetched
case class HtmlResourceFetched(uri: URI, status: Status, statusCode: Option[Int], content: Option[String]) extends ResourceFetched

object ResourceFetcher {

	val cssCache: concurrent.Map[URI, CssContent] = new ConcurrentHashMap[URI, CssContent]
	val htmlCache: concurrent.Map[URI, (String, Seq[EmbeddedResource])] = new ConcurrentHashMap[URI, (String, Seq[EmbeddedResource])]

	val resourceChecks = List(HttpRequestActionBuilder.defaultHttpCheck)

	def apply(uri: URI, statusCode: Int, lastModifiedHeader: Option[String], html: Option[String]) = {

		val directResources: Seq[EmbeddedResource] = statusCode match {
			case 200 =>
				// FIXME eTag + protocol.cache
				lastModifiedHeader match {
					case Some(lm) => htmlCache.get(uri) match {
						case Some((cachedLastModified, resources)) if lm == cachedLastModified =>
							resources
						case _ =>
							val resources = HtmlParser.getEmbeddedResources(uri, html.getOrElse(""))
							htmlCache.put(uri, (lm, resources))
							resources
					}

					case None => HtmlParser.getEmbeddedResources(uri, html.getOrElse(""))
				}

			case 304 =>
				htmlCache.get(uri).map(_._2).getOrElse(Nil)
		}

		val nodeSelector: Option[NodeSelector] =
			if (directResources.exists(_.resType == Css))
				html.map { h =>
					val domBuilder = new LagartoDOMBuilder
					domBuilder.setParseSpecialTagsAsCdata(true)
					new NodeSelector(domBuilder.parse(h))
				}
			else
				None

		(tx: HttpTx) => new ResourceFetcher(uri, directResources, nodeSelector, tx)
	}
}

class ResourceFetcher(uri: URI, directResources: Seq[EmbeddedResource], nodeSelector: Option[NodeSelector], tx: HttpTx) extends BaseActor {

	def fetchResource(request: Request) {
		logger.debug(s"Fetching ressource ${request.getUrl}")

		val requestName = {
			val uri = request.getURI.toString
			val start = uri.lastIndexOf('/') + 1
			if (start < uri.length)
				uri.substring(start, uri.length)
			else
				"/"
		}

		val resourceTx = tx.copy(
			request = request,
			requestName = requestName,
			checks = ResourceFetcher.resourceChecks,
			responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(ResourceFetcher.resourceChecks, None, tx.protocol),
			next = self,
			resourceFetching = true)

		HttpRequestAction.handleHttpTransaction(resourceTx)
	}

	def fetchOrBufferResources(resources: Iterable[EmbeddedResource]) {

		def buildRequest(resource: EmbeddedResource) = {
			val urlExpression: Expression[String] = _ => resource.uri.toString.success
			val uriExpression: Expression[URI] = _ => resource.uri.success
			val requestBuilder = HttpRequestBaseBuilder.http(urlExpression).getURI(uriExpression)
			requestBuilder.build(tx.session, tx.protocol)
		}

		def sendRequests(host: String, requests: Iterable[Request]) {
			requests.foreach(fetchResource)
			availableTokensByHost += host -> (availableTokensByHost(host) - requests.size)
		}

		def bufferRequests(host: String, requests: Iterable[Request]) {
			bufferedRequestsByHost += host -> (bufferedRequestsByHost(host) ::: requests.toList)
		}

		val immediateAndBufferedRequestsPerHost = resources
			.map(buildRequest)
			.collect { case Success(request) => request }
			.groupBy(_.getURI.getHost)
			.map {
				case (host, requests) =>
					val availableTokens = availableTokensByHost(host)
					host -> requests.splitAt(availableTokens)
			}

		immediateAndBufferedRequestsPerHost.foreach {
			case (host, (immediateRequests, bufferedRequests)) =>
				sendRequests(host, immediateRequests)
				bufferRequests(host, bufferedRequests)
		}
	}

	var orderedExpectedCss: List[URI] = directResources.collect { case EmbeddedResource(uri, Css) => uri }.toList
	var fetchedCss = 0
	var globalStatus: Status = OK
	var pendingRequestsCount = directResources.size
	val bufferedRequestsByHost = collection.mutable.HashMap.empty[String, List[Request]].withDefaultValue(Nil)
	val availableTokensByHost = collection.mutable.HashMap.empty[String, Int].withDefaultValue(tx.protocol.maxConnectionsPerHost)
	val start = nowMillis

	fetchOrBufferResources(directResources)

	def receive: Receive = {

		def resourceFetched(uri: URI, status: Status) {

			def done(status: Status) {
				logger.debug("All resources were fetched")
				val newSession = (if (globalStatus == KO) tx.session.markAsFailed else tx.session).logGroupRequest(nowMillis - start, status)
				tx.next ! newSession
				context.stop(self)
			}

			def releaseToken(host: String) {
				bufferedRequestsByHost.get(host) match {
					case Some(head :: tail) =>
						// recycle token, fetch a buffered resource
						fetchResource(head)
						bufferedRequestsByHost += host -> tail

					case _ =>
						// nothing to send for this host
						logger.debug(s"No resource left for host $host")
						availableTokensByHost += host -> (availableTokensByHost(host) + 1)
				}
			}

			logger.debug(s"Resource $uri was fetched")
			pendingRequestsCount -= 1

			if (status == KO)
				globalStatus = KO

			if (pendingRequestsCount == 0)
				done(globalStatus)
			else
				releaseToken(uri.getHost)
		}

		def handleCssReceived(uri: URI, status: Status, content: Option[String]) {

			def allCssReceived(nodeSelector: NodeSelector) {
				// received all css, parsing 
				val styleRules = orderedExpectedCss.map { cssUrl =>
					ResourceFetcher.cssCache.get(cssUrl).map(_.styleRules)
						.getOrElse {
							logger.warn(s"Found a css url $cssUrl missing from the result map?!")
							Nil
						}
				}.flatten

				val sortedStyleRules = styleRules.zipWithIndex.toSeq.sortWith {
					case ((styleRule1, index1), (styleRule2, index2)) =>
						val selector1 = styleRule1.selector
						val selector2 = styleRule2.selector
						if (selector1.startsWith(selector2) && selector1.charAt(selector2.length) == ' ') true // selector1 is less precise than selector2
						else if (selector2.startsWith(selector1) && selector2.charAt(selector1.length) == ' ') false // selector1 is more precise than selector2
						else index1 < index2 // default, use order in files
				}.map(_._1)

				val matchedUrls = sortedStyleRules.map { styleRule =>
					val nodes = nodeSelector.select(styleRule.selector)
					nodes.map(_ -> styleRule.uri)
				}.flatten
					.toMap
					.values.map(EmbeddedResource(_))

				// have to fetch those images now
				pendingRequestsCount += matchedUrls.size
				fetchOrBufferResources(matchedUrls)
			}

			fetchedCss += 1

			for {
				content <- content if status == OK
			} {
				val rules = ResourceFetcher.cssCache.getOrElseUpdate(uri, CssParser.extractRules(uri, content))
				orderedExpectedCss = rules.importRules ::: orderedExpectedCss
				fetchOrBufferResources(rules.importRules.map(EmbeddedResource(_, Css)))
				ResourceFetcher.cssCache.putIfAbsent(uri, rules)
			}

			for {
				nodeSelector <- nodeSelector // FIXME nodeSelector should be cached, so never None
				if fetchedCss == orderedExpectedCss.size
			} allCssReceived(nodeSelector)
		}

		def handleHtmlFetched(uri: URI, status: Status, statusCode: Option[Int], content: Option[String]) {
			// TODO
			if (status == OK)
				statusCode match {
					case Some(200) => // parse
					case Some(304) => // cache
					case _ =>
				}
		}

		{
			case RegularResourceFetched(uri, status) =>
				resourceFetched(uri, status)

			case CssResourceFetched(uri, status, content) =>
				handleCssReceived(uri, status, content)
				resourceFetched(uri, status)

			case HtmlResourceFetched(uri, status, statusCode, content) =>
				handleHtmlFetched(uri, status, statusCode, content)
				resourceFetched(uri, status)
		}
	}
}
