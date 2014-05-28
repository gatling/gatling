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

  private val ConsecutiveResourcesMaxIntervalInMillis = 1000

  private def isRedirection(t: TimedScenarioElement[RequestElement]) = HttpHelper.isRedirect(t.element.statusCode)

  private def filterRedirection(requests: Seq[TimedScenarioElement[RequestElement]]): List[TimedScenarioElement[RequestElement]] = {
    val groupedRequests = requests.groupAsLongAs(isRedirection)

    // Remove the redirection and keep the last status code
    groupedRequests.map {
      case TimedScenarioElement(firstSendTime, _, firstReq) :: redirectedReqs if !redirectedReqs.isEmpty =>
        val TimedScenarioElement(_, lastArrivalTime, lastReq) = redirectedReqs.last
        List(TimedScenarioElement(firstSendTime, lastArrivalTime, firstReq.copy(statusCode = lastReq.statusCode, embeddedResources = lastReq.embeddedResources)))

      case reqs => reqs
    }.flatten
  }

  private def filterFetchedResources(requests: Seq[TimedScenarioElement[RequestElement]]): Seq[TimedScenarioElement[RequestElement]] = {

    val groupChainedRequests: List[List[TimedScenarioElement[RequestElement]]] = {
      var globalAcc = List.empty[List[TimedScenarioElement[RequestElement]]]
      var currentAcc = List.empty[TimedScenarioElement[RequestElement]]
      var previousArrivalTime = requests.head.arrivalTime
      for (request <- requests) {
        if (request.sendTime - previousArrivalTime < ConsecutiveResourcesMaxIntervalInMillis) {
          currentAcc = currentAcc ::: List(request)
        } else {
          if (!currentAcc.isEmpty)
            globalAcc = globalAcc ::: List(currentAcc)
          currentAcc = List(request)
        }
        previousArrivalTime = request.arrivalTime
      }
      globalAcc ::: List(currentAcc)
    }

    groupChainedRequests.map {
      case List(request) => request
      case mainRequest :: resources =>
        val arrivalTime = resources.map(_.arrivalTime).max

        // TODO NRE : are we sure they are both absolute URLs?
        val nonEmbeddedResources = resources.filterNot(request => mainRequest.element.embeddedResources.exists(_.url == request.element.uri)).map(_.element)
        mainRequest.copy(arrivalTime = arrivalTime, element = mainRequest.element.copy(nonEmbeddedResources = nonEmbeddedResources))
    }
  }

  // FIXME no need for sortedRequests
  private def mergeWithPauses(sortedRequests: Seq[TimedScenarioElement[RequestElement]], tags: Seq[TimedScenarioElement[TagElement]],
                              thresholdForPauseCreation: Duration): Seq[ScenarioElement] = {

    if (sortedRequests.size <= 1)
      sortedRequests.map(_.element)
    else {
      val allElements: Seq[TimedScenarioElement[ScenarioElement]] = (sortedRequests ++ tags).sortBy(_.arrivalTime)
      var lastSendDateTime = allElements.last.sendTime
      val allElementsWithTagsStickingToNextRequest: Seq[TimedScenarioElement[ScenarioElement]] = allElements.reverse.foldLeft(List.empty[TimedScenarioElement[ScenarioElement]]) { (acc, current) =>
        current match {
          case TimedScenarioElement(sendTime, _, TagElement(text)) =>
            TimedScenarioElement(lastSendDateTime, lastSendDateTime, TagElement(text)) :: acc

          case TimedScenarioElement(sendTime, _, _) =>
            lastSendDateTime = sendTime
            current :: acc
        }
      }

      val pauses = allElementsWithTagsStickingToNextRequest.tail.zip(allElementsWithTagsStickingToNextRequest)
        .map {
          case (element, previousElement) =>
            val pauseDurationInMillis = math.max(element.sendTime - previousElement.arrivalTime, 0L)
            TimedScenarioElement(element.arrivalTime, element.arrivalTime, PauseElement(pauseDurationInMillis milliseconds))
        }

      val combined = allElementsWithTagsStickingToNextRequest.zip(pauses).flatMap { case (elem, pause) => List(elem, pause) } ++ Seq(allElementsWithTagsStickingToNextRequest.last)

      combined.filter {
        case TimedScenarioElement(_, _, PauseElement(duration)) => duration >= thresholdForPauseCreation
        case _ => true
      }.map(_.element)
    }
  }

  def apply(requests: Seq[TimedScenarioElement[RequestElement]], tags: Seq[TimedScenarioElement[TagElement]])(implicit config: RecorderConfiguration): ScenarioDefinition = {
    val sortedRequests = requests.sortBy(_.arrivalTime)

    val requests1 = if (config.http.followRedirect) filterRedirection(sortedRequests) else sortedRequests
    val requests2 = if (config.http.fetchHtmlResources) filterFetchedResources(requests1) else requests1

    if (config.http.followRedirect) logger.debug(s"Cleaning redirections: ${requests.size}->${requests1.size} requests")
    if (config.http.fetchHtmlResources) logger.debug(s"Cleaning automatically fetched HTML resources: ${requests1.size}->${requests2.size} requests")

    val allElements = mergeWithPauses(requests2, tags, config.core.thresholdForPauseCreation)
    apply(allElements)
  }
}
