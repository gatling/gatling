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
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.collection.mutable
import scala.collection.mutable.{ Map, SynchronizedMap, HashMap }
import scala.collection.immutable.{ TreeMap, HashSet }
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.FiniteDuration
import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.util.collection.RichSeq

case class SimulationModel(implicit config: RecorderConfiguration) {

  private val navigations = new mutable.ArrayBuffer[(Long, NavigationModel)] with mutable.SynchronizedBuffer[(Long, NavigationModel)]
  private var requests: Set[RequestModel] = HashSet()
  private var protocol: ProtocolModel = _ // instantiate once the capture complete

  private val proxyCredentials1: AtomicReference[String] = new AtomicReference[String]
  private val name1: String = config.core.className

  // for pre conditions
  private var requiresNewNavigation = false
  private var postProcessed = false
  private var lastRequestTimestamp: Long = 0 // check the requests are in order
  private var lastNavigationTimestamp: Long = 0

  // for state tracking
  private var lastNavigation: NavigationModel = _
  private var currentNavigation = new NavigationModel
  private val requestIDMap = makeMapIdentifier

  // require that the model is post processed before being able to get anthing out
  // TODO can change the require to a lazy execution of postProcess so it is transparent to the caller
  def getNavigations = {
    require(postProcessed)
    navigations
  }
  def getRequests = {
    require(postProcessed)
    requests
  }
  def getProtocol = {
    require(postProcessed)
    protocol
  }
  def proxyCredentials = {
    require(postProcessed)
    proxyCredentials1
  }
  def name = {
    require(postProcessed)
    name1
  }
  def packageName = {
    require(postProcessed)
    config.core.pkg
  }

  def isEmpty = requests.isEmpty

  /**
   * if there is already a request with the same identifier
   * change the identifier name method to be one that
   * appends a hash to the name to make it unique in the simulation
   */
  private def uniquifyRequestIdentifier(requestEl: RequestModel) = {

    requestIDMap.get(requestEl) match {
      case Some(s) => {
        if (requestEl.uri != s) requestEl.identifier = requestEl.identifierHash
      }
      case _ => {
        requestIDMap.put(requestEl, requestEl.uri);
      }
    }
  }

  private def makeMapIdentifier: Map[RequestModel, String] = {
    new HashMap[RequestModel, String] with SynchronizedMap[RequestModel, String] {
      override def default(key: RequestModel) =
        "-"
    }
  }

  def newNavigation(timestamp: Long, navigationName: String) = {

    require(currentNavigation.requestList.size > 0, "Cannot add Navigation - no requests in this navigation yet" + navigationName)
    require((lastNavigationTimestamp < timestamp && timestamp > lastRequestTimestamp),
      "Cannot add navigations out of order :" + navigationName)

    currentNavigation.name = navigationName.replaceAll("\\W", "_")
    navigations += timestamp -> currentNavigation

    lastNavigation = currentNavigation
    requiresNewNavigation = false
    lastNavigationTimestamp = timestamp

    currentNavigation = new NavigationModel
  }

  // adds a request
  def +=(a: (Long, RequestModel)) = {

    require(lastRequestTimestamp == 0 || (lastRequestTimestamp > 0 && lastRequestTimestamp <= a._1),
      "Cannot add requests out of order : ( last : " + lastRequestTimestamp + ", new : " + a._1 + ") " + a)

    // ignore redirect if following
    val isARedirect = (a._2.statusCode == 302 || a._2.statusCode == 301)
    val follow = config.http.followRedirect
    if ((config.http.followRedirect && !isARedirect) ||
      !config.http.followRedirect) {
      currentNavigation += a
      uniquifyRequestIdentifier(a._2)
      requests += a._2
      requiresNewNavigation = true
      lastRequestTimestamp = a._1
    }
  }

  def addPause(delta: FiniteDuration) = {

    // any pause between navigations is attributed to the previous navigation
    // as this is likely where the user spent their time
    if (currentNavigation.requestList.size == 0) {
      lastNavigation += (System.currentTimeMillis, new PauseModel(delta))
    } else {
      currentNavigation += (System.currentTimeMillis, new PauseModel(delta))
    }
  }

  def setProxyAuth(credentials: Option[(String, String)]) = {

    credentials match {
      case Some(s) => { proxyCredentials1.set(s._1 + "|" + s._2) }
      case None    => None
    }
  }

  /**
   * once the recording is finished we need to do some stuff
   * before some parts can be rendered.
   *
   * mostly needs to iterate over the whole simulation.
   * TODO - could likely be done incrementally with some more work.
   */
  def postProcess() = {

    if (requests.size > 0) {
      postProcessed = true

      // insert navigation if the user doesn't
      if (requiresNewNavigation)
        newNavigation(lastRequestTimestamp + 1, "default_navigation") //System.currentTimeMillis()

      // do protocol & headers
      protocol = ProtocolModel(this)
    }

  }
}
