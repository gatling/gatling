package io.gatling.recorder.scenario

import scala.concurrent.duration.{ Duration, DurationLong }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.util.collection.RichSeq

case class ScenarioDefinition(elements: Seq[ScenarioElement]) {
  def isEmpty = elements.isEmpty
}

object ScenarioDefinition extends StrictLogging {

  private def isRedirection(t: TimedScenarioElement[RequestElement]) = HttpHelper.isRedirect(t.element.statusCode)

  private def filterRedirection(requests: Seq[TimedScenarioElement[RequestElement]]): List[TimedScenarioElement[RequestElement]] = {
    val groupedRequests = requests.groupAsLongAs(isRedirection)

    // Remove the redirection and keep the last status code
    groupedRequests.map {
      case TimedScenarioElement(firstArrivalTime, firstReq) :: redirectedReqs if !redirectedReqs.isEmpty =>
        val TimedScenarioElement(_, lastReq) = redirectedReqs.last
        TimedScenarioElement(firstArrivalTime, firstReq.copy(statusCode = lastReq.statusCode, embeddedResources = lastReq.embeddedResources)) :: Nil

      case reqs => reqs
    }.flatten
  }

  private def hasEmbeddedResources(t: TimedScenarioElement[RequestElement]) = !t.element.embeddedResources.isEmpty

  private def filterFetchedResources(requests: Seq[TimedScenarioElement[RequestElement]]): Seq[TimedScenarioElement[RequestElement]] = {
    val groupedRequests = requests.splitWhen(hasEmbeddedResources)

    groupedRequests.map {
      case TimedScenarioElement(time, request) :: t if !request.embeddedResources.isEmpty =>
        val resourceUrls = request.embeddedResources.map(_.url).toSet

        // TODO NRE : are we sure they are both absolute URLs?
        TimedScenarioElement(time, request) :: t.filter { case TimedScenarioElement(_, r) => !resourceUrls.contains(r.uri) }

      case l => l
    }.flatten
  }

  private def mergeWithPauses(sortedRequests: Seq[TimedScenarioElement[RequestElement]], tags: Seq[TimedScenarioElement[TagElement]],
                              thresholdForPauseCreation: Duration): Seq[ScenarioElement] = {
    // Compute the pause elements
    val arrivalTimes = sortedRequests.map(_.timestamp)
    val initTime = arrivalTimes.headOption.getOrElse(0l)
    val timeBetweenEls = arrivalTimes.zip(initTime +: arrivalTimes).map { case (t2, t1) => (t2 - t1).milliseconds }
    val liftedRequestsWithPause: Seq[(Long, Seq[ScenarioElement])] = sortedRequests.zip(timeBetweenEls).map {
      case (TimedScenarioElement(arrivalTime, request), lag) =>
        if (lag > thresholdForPauseCreation)
          (arrivalTime, Vector(new PauseElement(lag), request))
        else
          (arrivalTime, Vector(request))
    }

    val liftedTags = tags.map { case TimedScenarioElement(time, tag) => (time, Vector(tag)) }

    (liftedTags ++ liftedRequestsWithPause).sortBy(_._1).map(_._2).flatten
  }

  def apply(requests: Seq[TimedScenarioElement[RequestElement]], tags: Seq[TimedScenarioElement[TagElement]])(implicit config: RecorderConfiguration): ScenarioDefinition = {
    val sortedRequests = requests.sortBy(_.timestamp)

    val requests1 = if (config.http.followRedirect) filterRedirection(sortedRequests) else sortedRequests
    val requests2 = if (config.http.fetchHtmlResources) filterFetchedResources(requests1) else requests1

    if (config.http.followRedirect) logger.debug(s"Cleaning redirections: ${requests.size}->${requests1.size} requests")
    if (config.http.fetchHtmlResources) logger.debug(s"Cleaning automatically fetched HTML resources: ${requests1.size}->${requests2.size} requests")

    val allElements = mergeWithPauses(requests2, tags, config.core.thresholdForPauseCreation)
    apply(allElements)
  }
}
