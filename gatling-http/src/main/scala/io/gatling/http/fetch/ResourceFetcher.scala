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
package io.gatling.http.fetch

import java.net.URI

import scala.annotation.switch
import scala.collection.JavaConversions._
import scala.collection.concurrent
import scala.collection.mutable

import com.ning.http.client.Request
import com.typesafe.scalalogging.slf4j.StrictLogging
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
import jsr166e.ConcurrentHashMapV8

sealed trait ResourceFetched {
	def uri: URI
	def status: Status
	def sessionUpdates: Session => Session
}
case class RegularResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session) extends ResourceFetched
case class CssResourceFetched(uri: URI, status: Status, sessionUpdates: Session => Session, statusCode: Int, lastModifiedOrEtag: Option[String], content: String) extends ResourceFetched

case class InferredPageResources(expire: String, requests: List[NamedRequest])

object ResourceFetcher extends StrictLogging {

	val cssContentCache: concurrent.Map[URI, List[EmbeddedResource]] = new ConcurrentHashMapV8[URI, List[EmbeddedResource]]
	val inferredResourcesCache: concurrent.Map[(HttpProtocol, URI), InferredPageResources] = new ConcurrentHashMapV8[(HttpProtocol, URI), InferredPageResources]

	val resourceChecks = List(HttpRequestActionBuilder.defaultHttpCheck)

	def pageResources(htmlDocumentURI: URI, filters: Option[Filters], responseChars: Array[Char]): List[EmbeddedResource] = {
		val htmlInferredResources = HtmlParser.getEmbeddedResources(htmlDocumentURI, responseChars)
		filters match {
			case Some(filters) => filters.filter(htmlInferredResources)
			case none => htmlInferredResources
		}
	}

	def cssResources(cssURI: URI, filters: Option[Filters], content: String): List[EmbeddedResource] = {
		val cssInferredResources = cssContentCache.getOrElseUpdate(cssURI, CssParser.extractResources(cssURI, content))
		filters match {
			case Some(filters) => filters.filter(cssInferredResources)
			case none => cssInferredResources
		}
	}

	def lastModifiedOrEtag(response: Response, protocol: HttpProtocol): Option[String] =
		if (protocol.cache)
			response.getHeaderSafe(HeaderNames.LAST_MODIFIED).orElse(response.getHeaderSafe(HeaderNames.ETAG))
		else
			None

	def fromPage(response: Response, tx: HttpTx, explicitResources: List[NamedRequest]): Option[() => ResourceFetcher] = {

		val htmlDocumentURI = response.request.getURI
		val protocol = tx.protocol

		def pageResourcesRequests(): List[NamedRequest] =
			pageResources(htmlDocumentURI, protocol.htmlResourcesFetchingFilters, response.chars)
				.flatMap(_.toRequest(protocol, tx.throttled))

		val inferredResources: List[NamedRequest] = (response.getStatusCode: @switch) match {
			case 200 =>
				lastModifiedOrEtag(response, protocol) match {
					case Some(newLastModifiedOrEtag) =>
						inferredResourcesCache.get(protocol, htmlDocumentURI) match {
							case Some(InferredPageResources(`newLastModifiedOrEtag`, inferredResources)) =>
								//cache entry didn't expire, use it
								inferredResources
							case _ =>
								// cache entry missing or expired, update it
								val inferredResources = pageResourcesRequests()
								// FIXME add throttle to cache key?
								inferredResourcesCache.put((protocol, htmlDocumentURI), InferredPageResources(newLastModifiedOrEtag, inferredResources))
								inferredResources
						}

					case None =>
						// don't cache
						pageResourcesRequests()
				}

			case 304 =>
				// no content, retrieve from cache if exist
				inferredResourcesCache.get(protocol, htmlDocumentURI) match {
					case Some(inferredPageResources) => inferredPageResources.requests
					case _ =>
						logger.warn(s"Got a 304 for $htmlDocumentURI but could find cache entry?!")
						Nil
				}

			case _ => Nil
		}

		resourceFetcher(tx, inferredResources, explicitResources)
	}

	def fromCache(htmlDocumentURI: URI, tx: HttpTx, explicitResources: List[NamedRequest]): Option[() => ResourceFetcher] = {
		val inferredResources = inferredResourcesCache.get(tx.protocol, htmlDocumentURI).map(_.requests).getOrElse(Nil)

		resourceFetcher(tx, inferredResources, explicitResources)
	}

	private def resourceFetcher(tx: HttpTx, inferredResources: List[NamedRequest], explicitResources: List[NamedRequest]) = {

		val uniqueResources = inferredResources.map(res => res.ahcRequest.getURI -> res).toMap ++
			explicitResources.map(res => res.ahcRequest.getURI -> res).toMap

		if (uniqueResources.isEmpty)
			None
		else {
			Some(() => new ResourceFetcher(tx, uniqueResources.values))
		}
	}
}

// FIXME handle crash
class ResourceFetcher(tx: HttpTx, initialResources: Iterable[NamedRequest]) extends BaseActor {

	var session = tx.session
	val alreadySeen: Set[URI] = initialResources.map(_.ahcRequest.getURI).toSet
	val bufferedRequestsByHost = mutable.HashMap.empty[String, List[NamedRequest]].withDefaultValue(Nil)
	val availableTokensByHost = mutable.HashMap.empty[String, Int].withDefaultValue(tx.protocol.maxConnectionsPerHost)
	var pendingRequestsCount = initialResources.size
	var globalStatus: Status = OK
	val start = nowMillis

	// start fetching
	fetchOrBufferResources(initialResources)

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
		// FIXME check if it's a css this way or use the Content-Type?
		val resourceFetched = if (ResourceFetcher.cssContentCache.contains(request.ahcRequest.getURI))
			CssResourceFetched(request.ahcRequest.getURI, OK, identity, 0, None, "")
		else
			RegularResourceFetched(request.ahcRequest.getURI, OK, identity)

		receive(resourceFetched)
	}

	def fetchOrBufferResources(requests: Iterable[NamedRequest]) {

		def sendRequests(host: String, requests: Iterable[NamedRequest]) {
			requests.foreach(fetchResource)
			availableTokensByHost += host -> (availableTokensByHost(host) - requests.size)
		}

		def bufferRequests(host: String, requests: Iterable[NamedRequest]) {
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

	def resourceFetched(uri: URI, status: Status) {

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

	def cssFetched(uri: URI, status: Status, statusCode: Int, lastModifiedOrEtag: Option[String], content: String) {

		val protocol = tx.protocol

		if (status == OK) {
			// this css might contain some resources

			val rawCssResources: List[NamedRequest] = (statusCode: @switch) match {
				case 200 =>
					// try to get from cache
					lastModifiedOrEtag match {
						case Some(newLastModifiedOrEtag) =>
							ResourceFetcher.inferredResourcesCache.get(protocol, uri) match {
								case Some(InferredPageResources(`newLastModifiedOrEtag`, inferredResources)) =>
									//cache entry didn't expire, use it
									inferredResources
								case _ =>
									// cache entry missing or expired, update it
									ResourceFetcher.cssContentCache.remove(protocol -> uri)
									val inferredResources = ResourceFetcher.cssResources(uri, protocol.htmlResourcesFetchingFilters, content).flatMap(_.toRequest(protocol, tx.throttled))
									ResourceFetcher.inferredResourcesCache.put((protocol, uri), InferredPageResources(newLastModifiedOrEtag, inferredResources))
									inferredResources
							}

						case None =>
							// don't cache
							ResourceFetcher.cssResources(uri, protocol.htmlResourcesFetchingFilters, content).flatMap(_.toRequest(protocol, tx.throttled))
					}

				case 304 =>
					// no content, retrieve from cache if exist
					ResourceFetcher.inferredResourcesCache.get(protocol, uri) match {
						case Some(inferredPageResources) => inferredPageResources.requests
						case _ =>
							logger.warn(s"Got a 304 for $uri but could find cache entry?!")
							Nil
					}
				case _ => Nil
			}

			val filtered = rawCssResources.filterNot(res => alreadySeen.contains(res.ahcRequest.getURI))

			pendingRequestsCount += filtered.size
			fetchOrBufferResources(filtered)
		}
	}

	def receive: Receive = {
		case RegularResourceFetched(uri, status, sessionUpdates) =>
			session = sessionUpdates(session)
			resourceFetched(uri, status)

		case CssResourceFetched(uri, status, sessionUpdates, statusCode, lastModifiedOrEtag, content) =>
			session = sessionUpdates(session)
			cssFetched(uri, status, statusCode, lastModifiedOrEtag, content)
			resourceFetched(uri, status)
	}
}
