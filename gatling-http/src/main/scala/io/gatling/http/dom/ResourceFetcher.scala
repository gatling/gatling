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
import scala.collection.mutable

import com.ning.http.client.Request
import io.gatling.core.action.GroupEnd
import io.gatling.core.akka.BaseActor
import io.gatling.core.filter.FilterListWrapper
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.Validation
import io.gatling.core.validation.{ Success, SuccessWrapper }
import io.gatling.http.HeaderNames
import io.gatling.http.action.{ HttpRequestAction, HttpRequestActionBuilder }
import io.gatling.http.ahc.HttpTx
import io.gatling.http.cache.CacheHandling
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.HttpRequestBaseBuilder
import io.gatling.http.response.{ Response, ResponseBuilder }
import org.jboss.netty.util.internal.ConcurrentHashMap

sealed trait ResourceFetched {
	def uri: URI
	def status: Status
	def sessionUpdates: Session => Session
}
case class RegularResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session) extends ResourceFetched
case class CssResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: String) extends ResourceFetched

object ResourceFetcher {

	val cssCache: concurrent.Map[(HttpProtocol, URI), CssContent] = new ConcurrentHashMap[(HttpProtocol, URI), CssContent]
	val htmlCache: concurrent.Map[(HttpProtocol, URI), (String, List[EmbeddedResource])] = new ConcurrentHashMap[(HttpProtocol, URI), (String, List[EmbeddedResource])]

	val resourceChecks = List(HttpRequestActionBuilder.defaultHttpCheck)

	sealed trait Resources
	case class Cached(resources: List[EmbeddedResource]) extends Resources
	case class Uncached(resources: List[EmbeddedResource]) extends Resources

	def fromReceivedHtmlPage(response: Response, tx: HttpTx): Option[() => ResourceFetcher] = {

		val htmlDocumentURI = response.request.getURI
		val protocol = tx.protocol

		val htmlCacheExpireFlag =
			if (protocol.cache)
				response.getHeaderSafe(HeaderNames.LAST_MODIFIED).orElse(response.getHeaderSafe(HeaderNames.ETAG))
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
								// cache entry missing or expired, flush it
								htmlCache.remove(htmlDocumentURI)
								val resources = HtmlParser.getEmbeddedResources(htmlDocumentURI, response.getResponseBody)
								Uncached(protocol.fetchHtmlResourcesFilters.filter(resources))
						}

					case None =>
						// don't cache
						val resources = HtmlParser.getEmbeddedResources(htmlDocumentURI, response.getResponseBody)
						Uncached(protocol.fetchHtmlResourcesFilters.filter(resources))
				}

			case 304 =>
				// no content, retrieve from cache if exist
				htmlCache.get(protocol, htmlDocumentURI).map {
					case (_, resources) =>
						Cached(protocol.fetchHtmlResourcesFilters.filter(resources))
				}.getOrElse(Uncached(Nil))

			case _ => Uncached(Nil)
		}

		resources match {
			case Cached(Nil) => None
			case Uncached(Nil) => None
			case Cached(resources) => Some(() => new ResourceFetcher(htmlDocumentURI, htmlCacheExpireFlag, resources, tx))
			case Uncached(resources) =>
				val body =
					if (resources.exists(_.resType == Css) && !response.getResponseBody.isEmpty) {
						Some(response.getResponseBody)
					} else
						None

				Some(() => new IncompleteResourceFetcher(htmlDocumentURI, protocol, htmlCacheExpireFlag, resources, body, tx))
		}
	}

	def fromCachedRequest(htmlDocumentURI: URI, tx: HttpTx): Option[() => ResourceFetcher] =
		htmlCache.get(tx.protocol, htmlDocumentURI).flatMap {
			case (_, resources) =>
				val filteredResources = tx.protocol.fetchHtmlResourcesFilters.filter(resources)

				filteredResources match {
					case Nil => None
					case filteredResources => Some(() => new ResourceFetcher(htmlDocumentURI, None, filteredResources, tx))
				}
		}
}

class ResourceFetcher(htmlDocumentURI: URI, htmlCacheExpireFlag: Option[String], resources: Seq[EmbeddedResource], tx: HttpTx) extends BaseActor {

	var session = tx.session
	val bufferedRequestsByHost = mutable.HashMap.empty[String, List[Request]].withDefaultValue(Nil)
	val availableTokensByHost = mutable.HashMap.empty[String, Int].withDefaultValue(tx.protocol.maxConnectionsPerHost)
	var pendingRequestsCount = resources.size
	var globalStatus: Status = OK
	val start = nowMillis
	fetchOrBufferResources(resources)

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
		if (ResourceFetcher.cssCache.contains(request.getURI))
			cssFetched(request.getURI, OK, identity, "")
		else
			resourceFetched(request.getURI, OK, identity)
	}

	def fetchOrBufferResources(resources: Iterable[EmbeddedResource]) {

		def buildRequest(resource: EmbeddedResource): Validation[Request] = {
			val urlExpression: Expression[String] = _ => resource.uri.toString.success
			val httpRequest = HttpRequestBaseBuilder.http(urlExpression).get(resource.uri).build(tx.protocol, tx.throttled)
			httpRequest.ahcRequest(tx.session)
		}

		def sendRequests(host: String, requests: Iterable[Request]) {
			requests.foreach(fetchResource)
			availableTokensByHost += host -> (availableTokensByHost(host) - requests.size)
		}

		def bufferRequests(host: String, requests: Iterable[Request]) {
			bufferedRequestsByHost += host -> (bufferedRequestsByHost(host) ::: requests.toList)
		}

		val requests = resources.map(buildRequest).collect { case Success(request) => request }
		val immediateAndBufferedRequestsPerHost = requests
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
		tx.next ! session
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

	def cssFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: String) {
		resourceFetched(uri, status, sessionUpdates)
	}

	def receive: Receive = {
		case RegularResourceFetched(uri, status, sessionUpdates) => resourceFetched(uri, status, sessionUpdates)
		case CssResourceFetched(uri, status, sessionUpdates, content) => cssFetched(uri, status, sessionUpdates, content)
	}
}

class IncompleteResourceFetcher(htmlDocumentURI: URI, protocol: HttpProtocol, htmlCacheExpireFlag: Option[String], directResources: Seq[EmbeddedResource], body: Option[String], tx: HttpTx)
	extends ResourceFetcher(htmlDocumentURI, htmlCacheExpireFlag, directResources, tx) {

	var expectedCss: List[URI] = directResources.collect { case EmbeddedResource(uri, Css) => uri }.toList
	var fetchedCss = 0
	val resources = collection.mutable.ArrayBuffer.empty[EmbeddedResource] ++= directResources

	override def cssFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: String) {

		def allCssReceived() {

			val cssResources = CssParser.cssResources(body, expectedCss.map { cssUrl => ResourceFetcher.cssCache.get(protocol, cssUrl) }.flatten)
			resources ++= cssResources
			pendingRequestsCount += cssResources.size
			fetchOrBufferResources(cssResources)

			htmlCacheExpireFlag.foreach { htmlCacheExpireFlag =>
				ResourceFetcher.htmlCache.putIfAbsent((protocol, htmlDocumentURI), (htmlCacheExpireFlag, resources.toList))
			}
		}

		fetchedCss += 1

		if (status == OK && !content.isEmpty) {
			val rules = CssParser.extractRules(uri, content)
			ResourceFetcher.cssCache.putIfAbsent((protocol, uri), rules)
		}

		ResourceFetcher.cssCache.get(protocol, uri).foreach { rules =>
			expectedCss = rules.importRules ::: expectedCss
			val cssImports = rules.importRules.map(EmbeddedResource(_, Css))
			resources ++= cssImports
			fetchOrBufferResources(cssImports)
		}

		if (fetchedCss == expectedCss.size) {
			allCssReceived()
		}

		super.cssFetched(uri, status, sessionUpdates, content)
	}
}
