/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.ahc

import scala.concurrent.duration.DurationInt
import com.ning.http.client.{ FluentStringsMap, RequestBuilder }
import akka.actor.{ ActorRef, Props }
import akka.actor.ActorDSL.actor
import akka.routing.RoundRobinRouter
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.DataWriter
import io.gatling.core.session.Session
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.HeaderNames
import io.gatling.http.action.HttpRequestAction
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.{ HttpCheck, HttpCheckOrder }
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.fetch.{ CssResourceFetched, RegularResourceFetched, ResourceFetcher }
import io.gatling.http.request.HttpRequest
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.{ isCss, isHtml, resolveFromURI }
import io.gatling.http.util.HttpStringBuilder
import io.gatling.core.result.writer.DataWriterClient

object AsyncHandlerActor extends AkkaDefaults {

	private var _instance: Option[ActorRef] = None

	def start() {
		if (!_instance.isDefined) {
			_instance = Some(system.actorOf(Props[AsyncHandlerActor].withRouter(RoundRobinRouter(nrOfInstances = 3 * Runtime.getRuntime.availableProcessors))))
			system.registerOnTermination(_instance = None)
		}
	}

	def instance() = _instance match {
		case Some(a) => a
		case _ => throw new UnsupportedOperationException("AsyncHandlerActor pool hasn't been started")
	}

	def updateCache(tx: HttpTx, response: Response): Session => Session = CacheHandling.cache(tx.protocol, _, tx.request, response)
	val fail: Session => Session = _.markAsFailed
}

class AsyncHandlerActor extends BaseActor with DataWriterClient {

	override def preRestart(reason: Throwable, message: Option[Any]) {

		message match {
			case Some(OnCompleted(tx, _)) =>
				logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
				tx.next ! tx.session.markAsFailed

			case Some(OnThrowable(tx, _, _)) =>
				logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
				tx.next ! tx.session.markAsFailed

			case _ =>
				logger.error(s"AsyncHandlerActor crashed on unknown message $message, dropping")
		}
	}

	def receive = {
		case OnCompleted(tx, response) => processResponse(tx, response)
		case OnThrowable(tx, response, errorMessage) => ko(tx, identity, response, errorMessage)
	}

	private def logRequest(
		tx: HttpTx,
		status: Status,
		response: Response,
		errorMessage: Option[String] = None) {

		val fullRequestName = if (tx.redirectCount > 0)
			s"${tx.requestName} Redirect ${tx.redirectCount}"
		else tx.requestName

		def dump = {
			val buff = new StringBuilder
			buff.append(eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(eol)
			buff.append("Request:").append(eol).append(s"$fullRequestName: $status ${errorMessage.getOrElse("")}").append(eol)
			buff.append("=========================").append(eol)
			buff.append("Session:").append(eol).append(tx.session).append(eol)
			buff.append("=========================").append(eol)
			buff.append("HTTP request:").append(eol).appendAHCRequest(tx.request)
			buff.append("=========================").append(eol)
			buff.append("HTTP response:").append(eol).appendResponse(response).append(eol)
			buff.append("<<<<<<<<<<<<<<<<<<<<<<<<<")
			buff.toString
		}

		if (status == KO) {
			logger.warn(s"Request '$fullRequestName' failed: ${errorMessage.getOrElse("")}")
			if (!logger.underlying.isTraceEnabled) logger.debug(dump)
		}
		logger.trace(dump)

		val extraInfo: List[Any] = try {
			tx.protocol.responsePart.extraInfoExtractor match {
				case Some(extractor) => extractor(tx.requestName, status, tx.session, tx.request, response)
				case _ => Nil
			}
		} catch {
			case e: Exception =>
				logger.warn("Encountered error while extracting extra request info", e)
				Nil
		}

		writeRequestData(
			tx.session,
			fullRequestName,
			response.firstByteSent,
			response.lastByteSent,
			response.firstByteReceived,
			response.lastByteReceived,
			status,
			errorMessage,
			extraInfo)
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param mutatedSession the new Session
	 */
	private def executeNext(tx: HttpTx, sessionUpdates: Session => Session, status: Status, response: Response) {

		def regularExecuteNext() {
			val updatedSession = sessionUpdates(tx.session)
			tx.next ! updatedSession.increaseDrift(nowMillis - response.lastByteReceived).logGroupRequest(response.reponseTimeInMillis, status)
		}

		// FIXME rewrite with extractors
		if (tx.resourceFetching) {
			val resourceMessage =
				if (isCss(response.headers))
					CssResourceFetched(response.request.getOriginalURI, status, sessionUpdates, response.statusCode, ResourceFetcher.lastModifiedOrEtag(response, tx.protocol), response.body.string)

				else
					RegularResourceFetched(response.request.getOriginalURI, status, sessionUpdates)

			tx.next ! resourceMessage

		} else if (tx.protocol.responsePart.fetchHtmlResources && response.isReceived && isHtml(response.headers)) {

			val explicitResources =
				if (!tx.explicitResources.isEmpty)
					HttpRequest.buildNamedRequests(tx.explicitResources, sessionUpdates(tx.session))
				else
					Nil

			ResourceFetcher.fromPage(response, tx, explicitResources) match {
				case Some(resourceFetcher) => actor(context)(resourceFetcher())
				case None => regularExecuteNext()
			}

		} else
			regularExecuteNext()
	}

	private def logAndExecuteNext(tx: HttpTx, sessionUpdates: Session => Session, status: Status, response: Response, message: Option[String]) {

		val newTx = tx.copy(session = sessionUpdates(tx.session))

		logRequest(newTx, status, response, message)
		executeNext(newTx, sessionUpdates, status, response)
	}

	private def ok(tx: HttpTx, sessionUpdates: Session => Session, response: Response) {
		logAndExecuteNext(tx, sessionUpdates, OK, response, None)
	}

	private def ko(tx: HttpTx, sessionUpdates: Session => Session, response: Response, message: String) {
		logAndExecuteNext(tx, sessionUpdates andThen AsyncHandlerActor.fail, KO, response, Some(message))
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(tx: HttpTx, response: Response) {

		def redirect(sessionUpdates: Session => Session) {

			tx.maxRedirects match {
				case Some(maxRedirects) if maxRedirects == tx.redirectCount =>
					ko(tx, sessionUpdates, response, s"Too many redirects, max is $maxRedirects")

				case _ =>
					response.header(HeaderNames.LOCATION) match {
						case Some(location) =>
							val newTx = tx.copy(session = sessionUpdates(tx.session))

							logRequest(newTx, OK, response)

							val redirectURI = resolveFromURI(tx.request.getURI, location)

							val requestBuilder = new RequestBuilder(tx.request)
								.setMethod("GET")
								.setBodyEncoding(configuration.core.encoding)
								.setQueryParameters(null.asInstanceOf[FluentStringsMap])
								.setParameters(null.asInstanceOf[FluentStringsMap])
								.setURI(redirectURI)
								.setConnectionPoolKeyStrategy(tx.request.getConnectionPoolKeyStrategy)

							for (cookie <- CookieHandling.getStoredCookies(newTx.session, redirectURI))
								requestBuilder.addOrReplaceCookie(cookie)

							val newRequest = requestBuilder.build
							newRequest.getHeaders.remove(HeaderNames.CONTENT_LENGTH)
							newRequest.getHeaders.remove(HeaderNames.CONTENT_TYPE)

							val redirectTx = newTx.copy(request = newRequest, redirectCount = tx.redirectCount + 1)
							HttpRequestAction.startHttpTransaction(redirectTx)

						case None =>
							ko(tx, sessionUpdates, response, "Redirect status, yet no Location header")

					}
			}
		}

		def checkAndProceed(sessionUpdates: Session => Session, checks: List[HttpCheck]) {

			val updateWithCacheUpdate = sessionUpdates andThen AsyncHandlerActor.updateCache(tx, response)

			Check.check(response, tx.session, checks) match {
				case Success(saveCheckExtracts) => ok(tx, updateWithCacheUpdate andThen saveCheckExtracts, response)
				case Failure(errorMessage) => ko(tx, updateWithCacheUpdate, response, errorMessage)
			}
		}

		response.status match {

			case Some(status) =>
				val updateWithUpdatedCookies: Session => Session = CookieHandling.storeCookies(_, status.getUrl, response.cookies)

				if (HttpHelper.isRedirect(status.getStatusCode) && tx.protocol.responsePart.followRedirect)
					redirect(updateWithUpdatedCookies)

				else {
					val checks =
						if (HttpHelper.isNotModified(status.getStatusCode))
							tx.checks.filter(c => c.order != HttpCheckOrder.Body && c.order != HttpCheckOrder.Checksum)
						else
							tx.checks

					checkAndProceed(updateWithUpdatedCookies, checks)
				}

			case None =>
				ko(tx, identity, response, "How come OnComplete was sent with no status?!")
		}
	}
}
