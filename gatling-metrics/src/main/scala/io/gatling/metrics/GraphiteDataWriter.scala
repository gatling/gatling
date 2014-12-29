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
package io.gatling.metrics

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import akka.actor.{ ActorRef, OneForOneStrategy, SupervisorStrategy }
import akka.actor.ActorDSL.actor

import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.writer._
import io.gatling.metrics.message._
import io.gatling.metrics.types._

private[metrics] object GraphiteDataWriter {
  import GraphitePath._
  val AllRequestsKey = graphitePath("allRequests")
  val UsersRootKey = graphitePath("users")
  val AllUsersKey = UsersRootKey / "allUsers"
}

private[gatling] class GraphiteDataWriter extends DataWriter {
  import GraphiteDataWriter._
  import GraphitePath._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5.seconds)(SupervisorStrategy.defaultDecider)

  implicit val config = configuration

  private var graphiteSender: ActorRef = _

  private val requestsByPath = mutable.Map.empty[GraphitePath, RequestMetricsBuffer]
  private val usersByScenario = mutable.Map.empty[GraphitePath, UsersBreakdownBuffer]

  def onInitializeDataWriter(assertions: Seq[Assertion], run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Unit = {
    val metricRootPath = config.data.graphite.rootPathPrefix + "." + sanitizeString(run.simulationId) + "."
    graphiteSender = actor(context, actorName("graphiteSender"))(new GraphiteSender(metricRootPath))

    usersByScenario.update(AllUsersKey, new UsersBreakdownBuffer(scenarios.map(_.nbUsers).sum))
    scenarios.foreach(scenario => usersByScenario += (UsersRootKey / scenario.name) -> new UsersBreakdownBuffer(scenario.nbUsers))

    scheduler.schedule(0 millisecond, configuration.data.graphite.writeInterval second, self, Send)
  }

  def onUserMessage(userMessage: UserMessage): Unit = {
    usersByScenario(UsersRootKey / userMessage.scenarioName).add(userMessage)
    usersByScenario(AllUsersKey).add(userMessage)
  }

  def onGroupMessage(group: GroupMessage): Unit = {}

  def onRequestMessage(request: RequestMessage): Unit = {
    if (!configuration.data.graphite.light) {
      val path = graphitePath(request.groupHierarchy :+ request.name)
      requestsByPath.getOrElseUpdate(path, new RequestMetricsBuffer).add(request.status, request.responseTime)
    }
    requestsByPath.getOrElseUpdate(AllRequestsKey, new RequestMetricsBuffer).add(request.status, request.responseTime)
  }

  def onTerminateDataWriter(): Unit = () // Do nothing, let the ActorSystem free resources

  override def receive: Receive = uninitialized

  override def initialized: Receive = super.initialized.orElse {
    case Send =>
      val requestMetrics = requestsByPath.mapValues(_.metricsByStatus).toMap
      val currentUserBreakdowns = usersByScenario.mapValues(UsersBreakdown(_)).toMap

      // Reset all metrics
      requestsByPath.foreach { case (_, buff) => buff.clear() }

      graphiteSender ! SendMetrics(requestMetrics, currentUserBreakdowns)
  }
}
