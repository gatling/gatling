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
import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.util.collection.RichSeq
import scala.collection.mutable
import scala.collection.mutable.{
  Map,
  SynchronizedMap,
  HashMap
}
import scala.collection.immutable.TreeMap
import scala.collection.immutable.HashSet
import java.util.concurrent.atomic.AtomicReference

case class SimulationModel(implicit config: RecorderConfiguration) {

  private val navigations = new mutable.ArrayBuffer[(Long, NavigationModel)] with mutable.SynchronizedBuffer[(Long, NavigationModel)]
  private var currentNavigation = new NavigationModel
  private var requests: Set[RequestModel] = HashSet()
  private var protocol: ProtocolModel = null // instantiate once the capture complete
  val name: String = config.core.className
  private val requestIDMap = makeMapIdentifier
  private var requiresNewNavigation = false

  val proxyCredentials: AtomicReference[String] = new AtomicReference[String]

  def getNavigations = { navigations }
  def getRequests = { requests }
  def getProtocol = { protocol }
  def isEmpty = navigations.isEmpty

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

  def clear = {
    navigations.clear
  }

  def newNavigation(timestamp: Long, navigationName: String) = {

    currentNavigation.name = navigationName.replaceAll("\\W", "_")

    navigations += timestamp -> currentNavigation
    currentNavigation = new NavigationModel
    requiresNewNavigation=false
  }

  def +=(a: (Long, RequestModel)) = {

    currentNavigation += a
    uniquifyRequestIdentifier(a._2)
    requests += a._2
    requiresNewNavigation=true
  }

  def setProxyAuth(credentials: Option[(String, String)]) = {

    credentials match {
      case Some(s) => { proxyCredentials.set(s._1 + "|" + s._2) }
      case None    => null
    }
  }

  /**
   * once the recording is finished we need to do some stuff
   * before some parts can be rendered.
   *
   * mostly needs to iterate over the whole simulation.
   * TODO - could likely be done incrementally with some more work. risk is that we forget to call postProcess....
   */
  def postProcess() = {

    // insert navigation if the user doesn't
    if(requiresNewNavigation)
      newNavigation(System.currentTimeMillis(), "default_navigation")
    
    // TODO remove redirects

    // do protocol & headers
    protocol = ProtocolModel(this)

    // TODO fetch HTML resources

    // TODO calculate pauses
    
    

  }
}
