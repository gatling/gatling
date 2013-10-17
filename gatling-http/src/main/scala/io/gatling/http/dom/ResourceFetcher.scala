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

import scala.annotation.switch
import scala.collection.JavaConversions._
import scala.collection.concurrent

import com.ning.http.client.Request
import io.gatling.core.action.GroupEnd
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.extractor.css.SilentLagartoDOMBuilder
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Success, SuccessWrapper }
import io.gatling.http.HeaderNames
import io.gatling.http.action.{ HttpRequestAction, HttpRequestActionBuilder }
import io.gatling.http.ahc.HttpTx
import io.gatling.http.cache.CacheHandling
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.HttpRequestBaseBuilder
import io.gatling.http.response.{ Response, ResponseBuilder }
import jodd.lagarto.dom.NodeSelector
import org.jboss.netty.util.internal.ConcurrentHashMap

sealed trait ResourceFetched {
	def uri: URI
	def status: Status
	def sessionUpdates: Session => Session
}
case class RegularResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session) extends ResourceFetched
case class CssResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: Option[String]) extends ResourceFetched
case class HtmlResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session, statusCode: Option[Int], content: Option[String]) extends ResourceFetched

object ResourceFetcher {

	val cssCache: concurrent.Map[(HttpProtocol, URI), CssContent] = new ConcurrentHashMap[(HttpProtocol, URI), CssContent]
	val htmlCache: concurrent.Map[(HttpProtocol, URI), (String, List[EmbeddedResource])] = new ConcurrentHashMap[(HttpProtocol, URI), (String, List[EmbeddedResource])]

	val resourceChecks = List(HttpRequestActionBuilder.defaultHttpCheck)

	sealed trait Resources
	case class Cached(resources: List[EmbeddedResource]) extends Resources
	case class Uncached(resources: List[EmbeddedResource]) extends Resources

	def apply(response: Response, protocol: HttpProtocol, html: => Option[String]): Option[HttpTx => ResourceFetcher] = {

		val htmlDocumentURI = response.request.getURI

		val htmlCacheExpireFlag =
			if (protocol.cache)
				Option(response.getHeader(HeaderNames.LAST_MODIFIED)).orElse(Option(response.getHeader(HeaderNames.ETAG)))
			else
				None

		val resources: Resources = (response.getStatusCode: @switch) match {
			case 200 =>
				htmlCacheExpireFlag match {
					case Some(newHtmlCacheExpireFlag) =>
						htmlCache.get(protocol, htmlDocumentURI) match {
							case Some((oldHtmlCacheExpireFlag, resources)) if newHtmlCacheExpireFlag == oldHtmlCacheExpireFlag =>
								//cache entry didn't expire, use it
								Cached(resources)
							case _ =>
								// cache entry expired, flush it
								htmlCache.remove(htmlDocumentURI)
								val resources = HtmlParser.getEmbeddedResources(htmlDocumentURI, html.getOrElse(""))
								Uncached(resources.filter(protocol.fetchHtmlResourcesFilters))
						}

					case None =>
						// don't cache
						val resources = HtmlParser.getEmbeddedResources(htmlDocumentURI, html.getOrElse(""))
						Uncached(resources.filter(protocol.fetchHtmlResourcesFilters))
				}

			case 304 =>
				// no content, retrieve from cache if exist
				htmlCache.get(protocol, htmlDocumentURI).map {
					case (_, resources) =>
						Cached(resources.filter(protocol.fetchHtmlResourcesFilters))
				}.getOrElse(Uncached(Nil))

			case _ => Uncached(Nil)
		}

		resources match {
			case Cached(Nil) => None
			case Cached(resources) => Some((tx: HttpTx) => new ResourceFetcher(htmlDocumentURI, htmlCacheExpireFlag, resources, tx))
			case Uncached(Nil) => None
			case Uncached(resources) =>
				val nodeSelector: Option[NodeSelector] =
					if (resources.exists(_.resType == Css))
						html.map { h =>
							val domBuilder = new SilentLagartoDOMBuilder().setParseSpecialTagsAsCdata(true)
							new NodeSelector(domBuilder.parse(h))
						}
					else
						None

				Some((tx: HttpTx) => new IncompleteResourceFetcher(htmlDocumentURI, protocol, htmlCacheExpireFlag, resources, nodeSelector, tx))
		}
	}

	def apply(htmlDocumentURI: URI, protocol: HttpProtocol): Option[HttpTx => ResourceFetcher] =
		htmlCache.get(protocol, htmlDocumentURI).flatMap {
			case (_, resources) =>
				val filteredResources = resources.filter(protocol.fetchHtmlResourcesFilters)

				filteredResources match {
					case Nil => None
					case filteredResources => Some((tx: HttpTx) => new ResourceFetcher(htmlDocumentURI, None, filteredResources, tx))
				}
		}
}

class ResourceFetcher(htmlDocumentURI: URI, htmlCacheExpireFlag: Option[String], initialResources: Seq[EmbeddedResource], tx: HttpTx) extends BaseActor {

	var session = tx.session.enterGroup(s"${tx.requestName} embedded resources")
	val bufferedRequestsByHost = collection.mutable.HashMap.empty[String, List[Request]].withDefaultValue(Nil)
	val availableTokensByHost = collection.mutable.HashMap.empty[String, Int].withDefaultValue(tx.protocol.maxConnectionsPerHost)
	var pendingRequestsCount = initialResources.size
	var globalStatus: Status = OK
	val start = nowMillis
	fetchOrBufferResources(initialResources)

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
			session = this.session,
			request = request,
			requestName = requestName,
			checks = ResourceFetcher.resourceChecks,
			responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(ResourceFetcher.resourceChecks, None, tx.protocol),
			next = self,
			resourceFetching = true)

		HttpRequestAction.handleHttpTransaction(resourceTx)
	}

	def handleCachedRequest(request: Request) {
		if (ResourceFetcher.cssCache.contains(request.getURI)) {
			cssFetched(request.getURI, OK, identity, None)
		} else if (ResourceFetcher.htmlCache.contains(request.getURI)) {
			htmlFetched(htmlDocumentURI, OK, identity, None, None)
		} else {
			resourceFetched(request.getURI, OK, identity)
		}
	}

	def fetchOrBufferResources(resources: Iterable[EmbeddedResource]) {

		def buildRequest(resource: EmbeddedResource) = {
			val urlExpression: Expression[String] = _ => resource.uri.toString.success
			val requestBuilder = HttpRequestBaseBuilder.http(urlExpression).get(resource.uri)
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

					var cached: List[Request] = Nil
					var nonCached: List[Request] = Nil
					requests.foreach { request =>
						val uri = request.getURI
						CacheHandling.getExpire(tx.protocol, session, uri) match {
							case None => nonCached = request :: nonCached

							case Some(expire) if nowMillis > expire =>
								session = CacheHandling.clearExpire(session, uri)
								nonCached = request :: nonCached

							case _ =>
								logger.info(s"Fetching resource ${request.getURI} from cache")
								cached = request :: cached
						}
					}

					cached.reverse.foreach(handleCachedRequest)

					val availableTokens = availableTokensByHost(host)
					host -> nonCached.reverse.splitAt(availableTokens)
			}

		immediateAndBufferedRequestsPerHost.foreach {
			case (host, (immediateRequests, bufferedRequests)) =>
				sendRequests(host, immediateRequests)
				bufferRequests(host, bufferedRequests)
		}
	}

	def done(status: Status) {
		logger.debug("All resources were fetched")
		GroupEnd.endGroup(session.logGroupRequest(nowMillis - start, status), tx.next)
		context.stop(self)
	}

	def resourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session) {

		def releaseToken(host: String, bufferedRequests: List[Request]) {
			bufferedRequests match {
				case Nil =>
					// nothing to send for this host
					availableTokensByHost += host -> (availableTokensByHost(host) + 1)

				case request :: tail =>
					bufferedRequestsByHost += host -> tail
					val uri = request.getURI
					CacheHandling.getExpire(tx.protocol, session, uri) match {
						case None =>
							// recycle token, fetch a buffered resource
							fetchResource(request)

						case Some(expire) if nowMillis > expire =>
							// expire reached
							session = CacheHandling.clearExpire(session, uri)
							fetchResource(request)

						case _ =>
							handleCachedRequest(request)
							releaseToken(host, tail)
					}
			}
		}

		logger.debug(s"Resource $uri was fetched")
		session = sessionUpdates(session)
		pendingRequestsCount -= 1

		if (status == KO)
			globalStatus = KO

		if (pendingRequestsCount == 0)
			done(globalStatus)
		else
			releaseToken(uri.getHost, bufferedRequestsByHost.get(uri.getHost).getOrElse(Nil))
	}

	def cssFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: Option[String]) {
		resourceFetched(uri, status, sessionUpdates)
	}

	def htmlFetched(uri: URI, status: Status, sessionUpdates: Session => Session, statusCode: Option[Int], content: Option[String]) {
		// TODO
		if (status == OK)
			statusCode match {
				case Some(200) => // parse
				case Some(304) => // cache
				case _ =>
			}

		resourceFetched(uri, status, sessionUpdates)
	}

	def receive: Receive = {
		case RegularResourceFetched(uri, status, sessionUpdates) => resourceFetched(uri, status, sessionUpdates)
		case CssResourceFetched(uri, status, sessionUpdates, content) => cssFetched(uri, status, sessionUpdates, content)
		case HtmlResourceFetched(uri, status, sessionUpdates, statusCode, content) => htmlFetched(uri, status, sessionUpdates, statusCode, content)
	}
}

class IncompleteResourceFetcher(htmlDocumentURI: URI, protocol: HttpProtocol, htmlCacheExpireFlag: Option[String], directResources: Seq[EmbeddedResource], nodeSelector: Option[NodeSelector], tx: HttpTx)
	extends ResourceFetcher(htmlDocumentURI, htmlCacheExpireFlag, directResources, tx) {

	var orderedExpectedCss: List[URI] = directResources.collect { case EmbeddedResource(uri, Css) => uri }.toList
	var fetchedCss = 0
	val resources = collection.mutable.ArrayBuffer.empty[EmbeddedResource] ++= directResources

	override def done(status: Status) {
		if (status == OK) {
			// update cache if everything went fine
			// FIXME what is a resource is always unavailable, shouldn't we cache?
			htmlCacheExpireFlag.foreach { htmlCacheExpireFlag =>
				ResourceFetcher.htmlCache.putIfAbsent((protocol, htmlDocumentURI), (htmlCacheExpireFlag, resources.toList))
			}
		}
		super.done(status)
	}

	override def cssFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: Option[String]) {

		def allCssReceived(nodeSelector: NodeSelector) {
			// received all css, parsing

			def fetchCssResources(res: Iterable[EmbeddedResource]) {
				resources ++= res
				pendingRequestsCount += res.size
				fetchOrBufferResources(res)
			}

			val fontFaceRules = orderedExpectedCss.map { cssUrl =>
				ResourceFetcher.cssCache.get(protocol, cssUrl).map(_.fontFaceRules.map(EmbeddedResource(_)))
					.getOrElse {
						logger.warn(s"Found a css url $cssUrl missing from the result map?!")
						Nil
					}
			}.flatten

			val appliedCssRulesImages = {

				val styleRules = orderedExpectedCss.map { cssUrl =>
					ResourceFetcher.cssCache.get(protocol, cssUrl).map(_.styleRules)
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

				sortedStyleRules.map { styleRule =>
					val nodes = nodeSelector.select(styleRule.selector)
					nodes.map(_ -> styleRule.uri)
				}.flatten
					.toMap
					.values.map(EmbeddedResource(_))
			}

			fetchCssResources(fontFaceRules)
			fetchCssResources(appliedCssRulesImages)
		}

		fetchedCss += 1

		for (content <- content if status == OK) {
			val rules = CssParser.extractRules(uri, content)
			ResourceFetcher.cssCache.putIfAbsent((protocol, uri), rules)
		}

		ResourceFetcher.cssCache.get(protocol, uri).foreach { rules =>
			orderedExpectedCss = rules.importRules ::: orderedExpectedCss
			val cssImports = rules.importRules.map(EmbeddedResource(_, Css))
			resources ++= cssImports
			fetchOrBufferResources(cssImports)
		}

		for {
			nodeSelector <- nodeSelector
			if fetchedCss == orderedExpectedCss.size
		} allCssReceived(nodeSelector)

		super.cssFetched(uri, status, sessionUpdates, content)
	}
}
