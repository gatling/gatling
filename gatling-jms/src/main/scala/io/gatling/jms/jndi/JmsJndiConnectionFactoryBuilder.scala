/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.jms.jndi

import java.util.{ Hashtable => JHashtable }
import javax.naming.{ Context, InitialContext }

import scala.jdk.CollectionConverters._

import io.gatling.commons.model.Credentials

import com.typesafe.scalalogging.StrictLogging
import jakarta.jms.ConnectionFactory

object JmsJndiConnectionFactoryBuilder {
  object Base {
    def connectionFactoryName(cfn: String): Url = new Url(cfn)
  }

  final class Url(connectionFactoryName: String) {
    def url(theUrl: String): ContextFactory =
      ContextFactory(connectionFactoryName, theUrl, None, Map.empty)
  }

  final case class ContextFactory(
      connectionFactoryName: String,
      url: String,
      credentials: Option[Credentials],
      properties: Map[String, String]
  ) {
    def credentials(user: String, password: String): ContextFactory =
      copy(credentials = Some(Credentials(user, password)))

    def property(key: String, value: String): ContextFactory =
      copy(properties = properties.updated(key, value))

    def contextFactory(cf: String): JmsJndiConnectionFactoryBuilder =
      new JmsJndiConnectionFactoryBuilder(cf, connectionFactoryName, url, credentials, properties)
  }
}

final class JmsJndiConnectionFactoryBuilder(
    contextFactory: String,
    connectionFactoryName: String,
    url: String,
    credentials: Option[Credentials],
    jndiProperties: Map[String, String]
) extends StrictLogging {
  def build(): ConnectionFactory = {
    // create InitialContext
    val properties = new JHashtable[String, String]
    properties.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory)
    properties.put(Context.PROVIDER_URL, url)

    credentials.foreach { credentials =>
      properties.put(Context.SECURITY_PRINCIPAL, credentials.username)
      properties.put(Context.SECURITY_CREDENTIALS, credentials.password)
    }
    properties.putAll(jndiProperties.asJava)

    val ctx = new InitialContext(properties)
    logger.debug(s"Got InitialContext $ctx")

    // create QueueConnectionFactory
    val qcf = ctx.lookup(connectionFactoryName).asInstanceOf[ConnectionFactory]
    logger.debug(s"Got ConnectionFactory $qcf")
    qcf
  }
}
