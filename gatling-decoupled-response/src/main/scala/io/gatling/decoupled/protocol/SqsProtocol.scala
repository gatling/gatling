/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.decoupled.protocol

import java.net.URL

import scala.concurrent.duration._

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{ Protocol, ProtocolComponents, ProtocolKey }
import io.gatling.core.session.Session
import io.gatling.decoupled.ingestion.{ SqsMessageProcessor, SqsReader }
import io.gatling.decoupled.ingestion.SqsReader.AwsKeys
import io.gatling.decoupled.state.{ ActorBasedPendingRequestsState, PendingRequestsState }

import com.github.benmanes.caffeine.cache.{ Cache, Caffeine }
import software.amazon.awssdk.regions.Region

object SqsProtocol {
  val SqsProtocolKey: ProtocolKey[SqsProtocol, SqsComponents] = new ProtocolKey[SqsProtocol, SqsComponents] {
    override def protocolClass: Class[Protocol] = classOf[SqsProtocol].asInstanceOf[Class[Protocol]]

    override def defaultProtocolValue(configuration: GatlingConfiguration): SqsProtocol =
      throw new IllegalStateException("Can't provide a default value for SqsProtocol")

    private val components: Cache[SqsProtocol, SqsComponents] = Caffeine.newBuilder().build()

    override def newComponents(coreComponents: CoreComponents): SqsProtocol => SqsComponents = { protocol =>
      components.get(
        protocol,
        createSqsComponents(coreComponents, _)
      )
    }
  }

  private def createSqsComponents(coreComponents: CoreComponents, protocol: SqsProtocol): SqsComponents = {
    val state = ActorBasedPendingRequestsState(
      coreComponents,
      protocol.decoupledResponseTimeout,
      protocol.processingTimeout
    )

    val sqsReader = new SqsReader(
      new SqsMessageProcessor(state),
      protocol.awsRegion,
      protocol.queueUrl.toExternalForm,
      protocol.awsKeys
    )(coreComponents.actorSystem)

    sqsReader.run

    SqsComponents(state, sqsReader)
  }

  def default(awsRegion: String, queueUrl: String): SqsProtocol =
    SqsProtocol(
      new URL(queueUrl),
      Region.of(awsRegion),
      10.minutes,
      30.seconds,
      None
    )

}

final case class SqsProtocol(
    queueUrl: URL,
    awsRegion: Region,
    decoupledResponseTimeout: FiniteDuration,
    processingTimeout: FiniteDuration,
    awsKeys: Option[AwsKeys]
) extends Protocol

final case class SqsComponents(pendingRequests: PendingRequestsState, reader: SqsReader) extends ProtocolComponents {
  override def onStart: Session => Session = ProtocolComponents.NoopOnStart
  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit
}
