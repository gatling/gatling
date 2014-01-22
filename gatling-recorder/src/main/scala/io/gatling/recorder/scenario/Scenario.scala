package io.gatling.recorder.scenario

import scala.concurrent.duration.{ Duration, DurationLong }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.util.RedirectHelper
import io.gatling.recorder.util.collection.RichSeq

case class Scenario(elements: Seq[ScenarioElement]) {
	def isEmpty = elements.isEmpty
}

object Scenario extends StrictLogging {

	private def isRedirection(t: (Long, RequestElement)) = RedirectHelper.isRedirectCode(t._2.statusCode)

	private def filterRedirection(requests: Seq[(Long, RequestElement)]): List[(Long, RequestElement)] = {
		val groupedRequests = requests.groupAsLongAs(isRedirection)

		// Remove the redirection and keep the last status code
		groupedRequests.map {
			case (firstArrivalTime, firstReq) :: redirectedReqs if !redirectedReqs.isEmpty => {
				val (lastArrivalTime, lastReq) = redirectedReqs.last
				(firstArrivalTime, firstReq.copy(statusCode = lastReq.statusCode)) :: Nil
			}
			case reqs => reqs
		}.flatten
	}

	private def hasEmbeddedResources(t: (Long, RequestElement)) = t._2.embeddedResources.isEmpty

	private def filterFetchedResources(requests: Seq[(Long, RequestElement)]): Seq[(Long, RequestElement)] = {
		val groupedRequests = requests.splitWhen(hasEmbeddedResources)

		groupedRequests.map {
			case (time, request) :: t if !request.embeddedResources.isEmpty => {
				val resourceUrls = request.embeddedResources.map(_.url).toSet

				// TODO NRE : are we sure they are both absolute URLs?
				(time, request) :: t.filter { case (t, r) => !resourceUrls.contains(r.uri) }
			}
			case l => l
		}.flatten
	}

	private def mergeWithPauses(sortedRequests: Seq[(Long, RequestElement)], tags: Seq[(Long, TagElement)],
		thresholdForPauseCreation: Duration): Seq[ScenarioElement] = {
		// Compute the pause elements
		val arrivalTimes = sortedRequests.map(_._1)
		val initTime = arrivalTimes.headOption.getOrElse(0l)
		val timeBetweenEls = arrivalTimes.zip(initTime +: arrivalTimes).map { case (t2, t1) => (t2 - t1).milliseconds }
		val liftedRequestsWithPause = sortedRequests.zip(timeBetweenEls).map {
			case ((arrivalTime, request), lag) =>
				if (lag > thresholdForPauseCreation)
					(arrivalTime, Vector(new PauseElement(lag), request))
				else
					(arrivalTime, Vector(request))
		}

		val liftedTags = tags.map { case (time, tag) => (time, Vector(tag)) }

		(liftedTags ++ liftedRequestsWithPause).sortBy(_._1).map(_._2).flatten
	}

	def apply(requests: Seq[(Long, RequestElement)], tags: Seq[(Long, TagElement)])(implicit config: RecorderConfiguration): Scenario = {
		val sortedRequests = requests.sortBy(_._1)

		val requests1 = if (config.http.followRedirect) filterRedirection(requests) else requests
		val requests2 = if (config.http.fetchHtmlResources) filterFetchedResources(requests1) else requests1

		if (config.http.followRedirect) logger.debug(s"Cleaning redirections: ${requests.size}->${requests1.size} requests")
		if (config.http.fetchHtmlResources) logger.debug(s"Cleaning automatically fetched HTML resources: ${requests1.size}->${requests2.size} requests")

		val allElements = mergeWithPauses(requests2, tags, config.core.thresholdForPauseCreation)
		apply(allElements)
	}

}

