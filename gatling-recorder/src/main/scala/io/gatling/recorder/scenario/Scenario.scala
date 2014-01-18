package io.gatling.recorder.scenario

import scala.concurrent.duration.DurationLong

import io.gatling.recorder.config.RecorderConfiguration.configuration

import io.gatling.recorder.util.RedirectHelper
import io.gatling.recorder.util.collection._

case class Scenario(elements: Seq[ScenarioElement]) {
	def isEmpty = elements.isEmpty
}

object Scenario {

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

	private def mergeWithPauses(sortedRequests: Seq[(Long, RequestElement)], tags: Seq[(Long, TagElement)]): Seq[ScenarioElement] = {
		// Compute the pause elements
		val arrivalTimes = sortedRequests.map(_._1)
		val initTime = arrivalTimes.headOption.getOrElse(0l)
		val timeBetweenEls = arrivalTimes.zip(initTime +: arrivalTimes).map { case (t2, t1) => (t2 - t1).milliseconds }
		val liftedRequestsWithPause = sortedRequests.zip(timeBetweenEls).map {
			case ((arrivalTime, request), lag) =>
				if (lag > configuration.core.thresholdForPauseCreation)
					(arrivalTime, Vector(new PauseElement(lag), request))
				else
					(arrivalTime, Vector(request))
		}

		val liftedTags = tags.map { case (time, tag) => (time, Vector(tag)) }

		(liftedTags ++ liftedRequestsWithPause).sortBy(_._1).map(_._2).flatten
	}

	def apply(requests: Seq[(Long, RequestElement)], tags: Seq[(Long, TagElement)]): Scenario = {
		val sortedRequests = requests.sortBy(_._1)

		val requests1 = if (configuration.http.followRedirect) filterRedirection(requests) else requests
		val requests2 = if (configuration.http.fetchHtmlResources) filterFetchedResources(requests1) else requests

		val allElements = mergeWithPauses(requests2, tags)
		apply(allElements)
	}

}

