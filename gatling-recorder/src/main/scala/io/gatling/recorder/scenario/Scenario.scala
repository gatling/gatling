package io.gatling.recorder.scenario

import scala.concurrent.duration.DurationLong
import io.gatling.recorder.util.RedirectHelper
import io.gatling.recorder.config.RecorderConfiguration.configuration

case class Scenario(elements: Seq[ScenarioElement]) {
	def isEmpty = elements.isEmpty
}

object Scenario {

	// See ScenarioSpec for example
	def groupAsLongAsPredicate[T](p: T => Boolean)(elts: Seq[T]): Seq[Seq[T]] = {
		elts.foldRight(List[List[T]]()) {
			case (t, Nil) => (t :: Nil) :: Nil
			case (t, xs @ xh :: xt) =>
				if (p(t)) (t :: xh) :: xt
				else (t :: Nil) :: xs
		}
	}

	private def isRedirection(t: (Long, RequestElement)) = RedirectHelper.isRedirectCode(t._2.statusCode)

	private val groupAsLongAsRedirection = groupAsLongAsPredicate(isRedirection) _

	private def filterRedirection(requests: Seq[(Long, RequestElement)]): Seq[(Long, RequestElement)] = {
		val groupedRequests = groupAsLongAsRedirection(requests)

		// Remove the redirection and keep the last status code
		groupedRequests.map {
			case (firstArrivalTime, firstReq) :: redirectedReqs if !redirectedReqs.isEmpty => {
				val (lastArrivalTime, lastReq) = redirectedReqs.last
				(firstArrivalTime, firstReq.copy(statusCode = lastReq.statusCode)) :: Nil
			}
			case reqs => reqs
		}.flatten
	}

	private def mergeWithPauses(sortedRequests: Seq[(Long, RequestElement)], tags: Seq[(Long, TagElement)]): Seq[ScenarioElement] = {

		// Compute the pause elements
		val arrivalTimes = sortedRequests.map(_._1)
		val initTime = arrivalTimes.headOption.getOrElse(0l)
		val timeBetweenEls = arrivalTimes.zip(initTime +: arrivalTimes).map { case (t2, t1) => t2 - t1 }
		val liftedRequestsWithPause = sortedRequests.zip(timeBetweenEls).map {
			case ((arrivalTime, request), lag) =>
				if (lag > 50) // TODO NICO - set a config for that
					(arrivalTime, Vector(new PauseElement(lag milliseconds), request))
				else
					(arrivalTime, Vector(request))
		}

		val liftedTags = tags.map { case (time, tag) => (time, Vector(tag)) }

		(liftedTags ++ liftedRequestsWithPause).sortBy(_._1).map(_._2).flatten
	}

	def apply(requests: Seq[(Long, RequestElement)], tags: Seq[(Long, TagElement)]): Scenario = {

		val sortedRequests = requests.sortBy(_._1)

		val requests1 = if (configuration.http.followRedirect) filterRedirection(requests) else requests

		val allElements = mergeWithPauses(requests1, tags)
		apply(allElements)
	}

}

