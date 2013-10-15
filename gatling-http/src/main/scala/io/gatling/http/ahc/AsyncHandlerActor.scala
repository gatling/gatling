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
package io.gatling.http.ahc

import scala.collection.JavaConversions.asScalaBuffer
import com.ning.http.client.{ FluentStringsMap, RequestBuilder }
import akka.actor.Props
import akka.actor.ActorDSL.actor
import akka.routing.RoundRobinRouter
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.check.Checks
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.{ DataWriter, RequestMessage }
import io.gatling.core.session.{ Session, MutationList }
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.HeaderNames
import io.gatling.http.action.HttpRequestAction
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.{ HttpCheck, HttpCheckOrder }
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.dom.{ CssResourceFetched, HtmlResourceFetched, RegularResourceFetched, ResourceFetcher }
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper.{ isCss, isHtml, resolveFromURI }
import io.gatling.http.util.HttpStringBuilder
import io.gatling.core.validation.Validation

object AsyncHandlerActor extends AkkaDefaults {
	val redirectedRequestNamePattern = """(.+?) Redirect (\d+)""".r

	val asyncHandlerActor = system.actorOf(Props[AsyncHandlerActor].withRouter(RoundRobinRouter(nrOfInstances = 3 * Runtime.getRuntime.availableProcessors)))

	def updateCookies(tx: HttpTx, response: Response): (Session => Session) = session => CookieHandling.storeCookies(session, response.getUri, response.getCookies.toList)
	def updateCache(tx: HttpTx, response: Response): (Session => Session) = session => CacheHandling.cache(tx.protocol, session, tx.request, response)
	val fail = (session: Session) => session.markAsFailed

}

class AsyncHandlerActor extends BaseActor {

	override def preRestart(reason: Throwable, message: Option[Any]) {
		logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
		message.foreach {
			case OnCompleted(tx, _) => tx.next ! tx.session.markAsFailed
			case OnThrowable(tx, _, _) => tx.next ! tx.session.markAsFailed
		}
	}

	def receive = {
		case OnCompleted(tx, response) => processResponse(tx, response)
		case OnThrowable(tx, response, errorMessage) => ko(tx, Nil, response, errorMessage)
	}

	private def logRequest(
		tx: HttpTx,
		status: Status,
		response: Response,
		errorMessage: Option[String] = None) {

		// FIXME useless if logger.error 
		def dump = {
			val buff = new StringBuilder
			buff.append(eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(eol)
			buff.append("Request:").append(eol).append(s"${tx.requestName}: $status ${errorMessage.getOrElse("")}").append(eol)
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
			logger.warn(s"Request '${tx.requestName}' failed : ${errorMessage.getOrElse("")}")
			if (!logger.underlying.isTraceEnabled) logger.debug(dump)
		}
		logger.trace(dump)

		val extraInfo: List[Any] = try {
			tx.protocol.extraInfoExtractor.map(_(status, tx.session, tx.request, response)).getOrElse(Nil)
		} catch {
			case e: Exception =>
				logger.warn("Encountered error while extracting extra request info", e)
				Nil
		}

		DataWriter.tell(RequestMessage(tx.session.scenarioName, tx.session.userId, tx.session.groupStack, tx.requestName,
			response.firstByteSent, response.firstByteSent, response.firstByteReceived, response.lastByteReceived,
			status, errorMessage, extraInfo))
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param mutatedSession the new Session
	 */
	private def executeNext(tx: HttpTx, mutations: List[Session => Session], status: Status, response: Response) {

		def statusCode() =
			if (response.hasResponseStatus)
				Some(response.getStatusCode)
			else None

		def body() =
			if (response.hasResponseStatus && response.getStatusCode == 200)
				Option(response.getResponseBody(configuration.core.encoding))
			else None

		// FIXME rewrite with extractors
		if (tx.resourceFetching) {
			val resourceMessage =
				if (isCss(response.getHeaders))
					CssResourceFetched(response.request.getURI, status, mutations, body())

				else if (isHtml(response.getHeaders))
					HtmlResourceFetched(response.request.getURI, status, mutations, statusCode(), body())

				else
					RegularResourceFetched(response.request.getURI, status, mutations)

			tx.next ! resourceMessage

		} else if (tx.protocol.fetchHtmlResources && response.hasResponseStatus && isHtml(response.getHeaders)) {

			val resourceFetcher = ResourceFetcher(response.request.getURI, response.getStatusCode, Option(response.getHeader(HeaderNames.LAST_MODIFIED)), body())
			actor(context)(resourceFetcher(tx))

		} else
			tx.next ! tx.session.increaseDrift(nowMillis - response.lastByteReceived).logGroupRequest(response.reponseTimeInMillis, status)
	}

	private def logAndExecuteNext(tx: HttpTx, mutations: List[Session => Session], status: Status, response: Response, message: Option[String]) {

		val newTx = tx.copy(session = mutations.mutate(tx.session))

		logRequest(newTx, status, response, message)
		executeNext(newTx, mutations, status, response)
	}

	private def ok(tx: HttpTx, mutations: List[Session => Session], response: Response) {
		logAndExecuteNext(tx, mutations, OK, response, None)
	}

	private def ko(tx: HttpTx, mutations: List[Session => Session], response: Response, message: String) {
		logAndExecuteNext(tx, AsyncHandlerActor.fail :: mutations, KO, response, Some(message))
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(tx: HttpTx, response: Response) {

		def redirect(mutations: List[Session => Session]) {

			if (tx.protocol.maxRedirects.map(_ == tx.numberOfRedirects).getOrElse(false)) {
				ko(tx, mutations, response, s"Too many redirects, max is ${tx.protocol.maxRedirects.get}")

			} else {

				val newTx = tx.copy(session = mutations.mutate(tx.session))

				logRequest(newTx, OK, response)

				val redirectURI = resolveFromURI(tx.request.getURI, response.getHeader(HeaderNames.LOCATION))

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

				val newRequestName = tx.requestName match {
					case AsyncHandlerActor.redirectedRequestNamePattern(requestBaseName, redirectCount) => s"$requestBaseName Redirect ${redirectCount.toInt + 1}"
					case _ => tx.requestName + " Redirect 1"
				}

				val redirectTx = newTx.copy(request = newRequest, requestName = newRequestName, numberOfRedirects = tx.numberOfRedirects + 1)
				HttpRequestAction.handleHttpTransaction(redirectTx)
			}
		}

		def checkAndProceed(mutations: List[Session => Session], checks: List[HttpCheck]) {

			val mutationsWithCacheUpdate = AsyncHandlerActor.updateCache(tx, response) :: mutations

			val checkResult = Checks.check(response, tx.session, checks)

			checkResult match {
				case Success(saveCheckExtracts) => ok(tx, saveCheckExtracts :: mutationsWithCacheUpdate, response)
				case Failure(errorMessage) => ko(tx, mutationsWithCacheUpdate, response, errorMessage)
			}
		}

		val mutationsWithUpdatedCookies = List(AsyncHandlerActor.updateCookies(tx, response))

		if (response.isRedirected && tx.protocol.followRedirect)
			redirect(mutationsWithUpdatedCookies)
		else {
			val checks =
				if (response.getStatusCode == 304)
					tx.checks.filter(c => c.order != HttpCheckOrder.Body && c.order != HttpCheckOrder.Checksum)
				else
					tx.checks
			checkAndProceed(mutationsWithUpdatedCookies, checks)
		}
	}
}
