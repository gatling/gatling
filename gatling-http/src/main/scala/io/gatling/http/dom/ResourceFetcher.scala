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
import io.gatling.core.filter.Filters
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

	case class HtmlPageResourcesCacheValue(expire: String, requests: List[NamedRequest])

	// FIXME use a value class to make URI meaning explicit
	val cssCache: concurrent.Map[URI, CssContent] = new ConcurrentHashMap[URI, CssContent]
	val resourceCache: concurrent.Map[(HttpProtocol, URI), HtmlPageResourcesCacheValue] = new ConcurrentHashMap[(HttpProtocol, URI), HtmlPageResourcesCacheValue]

	val resourceChecks = List(HttpRequestActionBuilder.defaultHttpCheck)

	sealed trait Resources
	case class Cached(requests: List[NamedRequest]) extends Resources
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
						resourceCache.get(protocol, htmlDocumentURI) match {
							case Some(HtmlPageResourcesCacheValue(oldHtmlCacheExpireFlag, requests)) if newHtmlCacheExpireFlag == oldHtmlCacheExpireFlag =>
								//cache entry didn't expire, use it
								Cached(requests)
							case _ =>
								// cache entry missing or expired, flush it
								resourceCache.remove(protocol, htmlDocumentURI)
								val resources = HtmlParser.getEmbeddedResources(htmlDocumentURI, response.getResponseBody)
								val filteredResources = protocol.fetchHtmlResourcesFilters match {
									case Some(filters) => filters.filter(resources)
									case none => resources
								}
								Uncached(filteredResources)
						}

					case None =>
						// don't cache
						val resources = HtmlParser.getEmbeddedResources(htmlDocumentURI, response.getResponseBody)
						val filteredResources = protocol.fetchHtmlResourcesFilters match {
							case Some(filters) => filters.filter(resources)
							case none => resources
						}
						Uncached(filteredResources)
				}

			case 304 =>
				// no content, retrieve from cache if exist
				resourceCache.get(protocol, htmlDocumentURI) match {
					case Some(v) => Cached(v.requests)
					case _ => Uncached(Nil)
				}

			case _ => Uncached(Nil)
		}

		resources match {
			case Cached(Nil) => None
			case Uncached(Nil) => None
			case Cached(requests) => Some(() => new ResourceFetcher(htmlDocumentURI, htmlCacheExpireFlag, requests, tx))
			case Uncached(resources) =>
				// no need to hold a reference to the full body String if it won't be used
				val body =
					if (resources.exists(_.isInstanceOf[CssResource])) {
						Some(response.getResponseBody)
					} else
						None

				Some(() => new IncompleteResourceFetcher(htmlDocumentURI, protocol, htmlCacheExpireFlag, resources, body, tx))
		}
	}

	def fromCachedRequest(htmlDocumentURI: URI, tx: HttpTx): Option[() => ResourceFetcher] =
		resourceCache.get(tx.protocol, htmlDocumentURI).flatMap { cacheValue =>

			cacheValue.requests match {
				case Nil => None
				case requests => Some(() => new ResourceFetcher(htmlDocumentURI, None, requests, tx))
			}
		}
}

class ResourceFetcher(htmlDocumentURI: URI, htmlCacheExpireFlag: Option[String], val requests: List[NamedRequest], tx: HttpTx) extends BaseActor {

	var session = tx.session
	val bufferedRequestsByHost = mutable.HashMap.empty[String, List[NamedRequest]].withDefaultValue(Nil)
	val availableTokensByHost = mutable.HashMap.empty[String, Int].withDefaultValue(tx.protocol.maxConnectionsPerHost)
	var pendingRequestsCount = requests.size
	var globalStatus: Status = OK
	val start = nowMillis
	fetchOrBufferResources(requests)

	def fetchResource(request: NamedRequest) {
		logger.debug(s"Fetching ressource ${request.ahcRequest.getUrl}")

		val resourceTx = tx.copy(
			session = this.session,
			request = request.ahcRequest,
			requestName = request.name,
			checks = ResourceFetcher.resourceChecks,
			responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(ResourceFetcher.resourceChecks, None, tx.protocol),
			next = self,
			resourceFetching = true)

		HttpRequestAction.beginHttpTransaction(resourceTx)
	}

	def handleCachedRequest(request: NamedRequest) {
		logger.info(s"Fetching resource ${request.ahcRequest.getURI} from cache")
		if (ResourceFetcher.cssCache.contains(request.ahcRequest.getURI))
			cssFetched(request.ahcRequest.getURI, OK, identity, "")
		else
			resourceFetched(request.ahcRequest.getURI, OK, identity)
	}

	def fetchOrBufferResources(requests: List[NamedRequest]) {

		def sendRequests(host: String, requests: List[NamedRequest]) {
			requests.foreach(fetchResource)
			availableTokensByHost += host -> (availableTokensByHost(host) - requests.size)
		}

		def bufferRequests(host: String, requests: List[NamedRequest]) {
			bufferedRequestsByHost += host -> (bufferedRequestsByHost(host) ::: requests.toList)
		}

		val (nonCachedRequests, cachedRequests) = requests.partition { request =>
			val uri = request.ahcRequest.getURI
			CacheHandling.getExpire(tx.protocol, session, uri) match {
				case None => true
				case Some(expire) if nowMillis > expire =>
					// ugly, side effecting
					session = CacheHandling.clearExpire(session, uri)
					true
				case _ => false
			}
		}

		cachedRequests.foreach(handleCachedRequest)

		nonCachedRequests
			.groupBy(_.ahcRequest.getURI.getHost)
			.foreach {
				case (host, requests) =>
					val availableTokens = availableTokensByHost(host)
					val (immediateRequests, bufferedRequests) = requests.splitAt(availableTokens)
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

		def releaseToken(host: String, bufferedRequests: List[NamedRequest]) {
			bufferedRequests match {
				case Nil =>
					// nothing to send for this host
					availableTokensByHost += host -> (availableTokensByHost(host) + 1)

				case request :: tail =>
					bufferedRequestsByHost += host -> tail
					val uri = request.ahcRequest.getURI
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
		else {
			val requests = bufferedRequestsByHost.get(uri.getHost) match {
				case Some(requests) => requests
				case _ => Nil
			}
			releaseToken(uri.getHost, requests)
		}
	}

	def cssFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: String) {
		resourceFetched(uri, status, sessionUpdates)
	}

	def receive: Receive = {
		case RegularResourceFetched(uri, status, sessionUpdates) => resourceFetched(uri, status, sessionUpdates)
		case CssResourceFetched(uri, status, sessionUpdates, content) => cssFetched(uri, status, sessionUpdates, content)
	}
}

class IncompleteResourceFetcher(htmlDocumentURI: URI, protocol: HttpProtocol, htmlCacheExpireFlag: Option[String], resources: List[EmbeddedResource], body: Option[String], tx: HttpTx)
	extends ResourceFetcher(htmlDocumentURI, htmlCacheExpireFlag, resources.flatMap(_.toRequest(protocol)), tx) {

	// FIXME add a flag for telling provided resources apart and not caching them
	var expectedCss: List[CssResource] = resources.collect { case css: CssResource => css }
	var fetchedCss = 0
	val totalRequests = collection.mutable.ArrayBuffer.empty[NamedRequest] ++= requests

	override def cssFetched(uri: URI, status: Status, sessionUpdates: Session => Session, content: String) {

		def allCssReceived() {

			val cssContents = expectedCss.map { cssResource => ResourceFetcher.cssCache.get(cssResource.uri) }.flatten
			val cssResources = CssParser.cssResources(body, cssContents)
			val filteredCssResources = protocol.fetchHtmlResourcesFilters match {
				case Some(filters) => filters.filter(cssResources)
				case _ => cssResources
			}
			val cssResourceRequests = filteredCssResources.flatMap(_.toRequest(protocol))
			totalRequests ++= cssResourceRequests
			pendingRequestsCount += cssResourceRequests.size
			fetchOrBufferResources(cssResourceRequests)

			htmlCacheExpireFlag.foreach { htmlCacheExpireFlag =>
				ResourceFetcher.resourceCache.putIfAbsent((protocol, htmlDocumentURI), ResourceFetcher.HtmlPageResourcesCacheValue(htmlCacheExpireFlag, totalRequests.toList))
			}
		}

		fetchedCss += 1

		if (status == OK && !content.isEmpty) {
			val rules = CssParser.extractRules(uri, content)
			ResourceFetcher.cssCache.putIfAbsent(uri, rules)
		}

		ResourceFetcher.cssCache.get(uri).foreach { rules =>
			val cssImports = rules.importRules.map(CssResource)
			val filteredCssImports = protocol.fetchHtmlResourcesFilters match {
				case Some(filters) => filters.filter(cssImports)
				case None => cssImports
			}
			expectedCss = filteredCssImports ::: expectedCss
			val cssImportRequests = filteredCssImports.flatMap(_.toRequest(protocol))
			totalRequests ++= cssImportRequests
			fetchOrBufferResources(cssImportRequests)
		}

		if (fetchedCss == expectedCss.size) {
			allCssReceived()
		}

		super.cssFetched(uri, status, sessionUpdates, content)
	}
}
