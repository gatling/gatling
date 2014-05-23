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
package io.gatling.recorder.model

import scala.concurrent.duration.{ Duration, DurationLong }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.model.RequestModel.htmlContentType
import io.gatling.recorder.util.collection.RichSeq

case class ScenarioModel(elements: Seq[ExecModel]) {
  def isEmpty = elements.isEmpty
}

object ScenarioModel extends StrictLogging {

  private def isRedirection(t: (Long, RequestModel)) = HttpHelper.isRedirect(t._2.statusCode)

  private def filterRedirection(requests: Seq[(Long, RequestModel)]): List[(Long, RequestModel)] = {
    val groupedRequests = requests.groupAsLongAs(isRedirection)

    // Remove the redirection and keep the last status code
    groupedRequests.map {
      case (firstArrivalTime, firstReq) :: redirectedReqs if !redirectedReqs.isEmpty => {
        val (lastArrivalTime, lastReq) = redirectedReqs.last
        (firstArrivalTime, firstReq.copy(statusCode = lastReq.statusCode, embeddedResources = lastReq.embeddedResources)) :: Nil
      }
      case reqs => reqs
    }.flatten
  }

  private def hasEmbeddedResources(t: (Long, RequestModel)) = !t._2.embeddedResources.isEmpty

  private def filterFetchedResources(requests: Seq[(Long, RequestModel)]): Seq[(Long, RequestModel)] = {
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

  private def mergeWithPauses(sortedRequests: Seq[(Long, RequestModel)], tags: Seq[(Long, TagModel)],
    thresholdForPauseCreation: Duration): Seq[ExecModel] = {
    // Compute the pause elements
    val arrivalTimes = sortedRequests.map(_._1)
    val initTime = arrivalTimes.headOption.getOrElse(0l)
    val timeBetweenEls = arrivalTimes.zip(initTime +: arrivalTimes).map { case (t2, t1) => (t2 - t1).milliseconds }
    val liftedRequestsWithPause = sortedRequests.zip(timeBetweenEls).map {
      case ((arrivalTime, request), lag) =>
        if (lag > thresholdForPauseCreation)
          (arrivalTime, Vector(new PauseModel(lag), request))
        else
          (arrivalTime, Vector(request))
    }

    val liftedTags = tags.map { case (time, tag) => (time, Vector(tag)) }

    (liftedTags ++ liftedRequestsWithPause).sortBy(_._1).map(_._2).flatten
  }

  def apply(requests: Seq[(Long, RequestModel)],
    tags: Seq[(Long, TagModel)])(implicit config: RecorderConfiguration): ScenarioModel = {

    val sortedRequests = requests.sortBy(_._1)

    val requests1 = if (config.http.followRedirect) filterRedirection(sortedRequests) else sortedRequests
    val requests2 = if (config.http.fetchHtmlResources) filterFetchedResources(requests1) else requests1

    if (config.http.followRedirect) logger.debug(s"Cleaning redirections: ${requests.size}->${requests1.size} requests")
    if (config.http.fetchHtmlResources) logger.debug(s"Cleaning automatically fetched HTML resources: ${requests1.size}->${requests2.size} requests")

    val allElements = mergeWithPauses(requests2, tags, config.core.thresholdForPauseCreation)
    apply(allElements)
  }

}

