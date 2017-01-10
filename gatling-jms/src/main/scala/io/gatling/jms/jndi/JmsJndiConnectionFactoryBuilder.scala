/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
import javax.jms.ConnectionFactory
import javax.naming.{ Context, InitialContext }

import io.gatling.core.config.Credentials

import com.typesafe.scalalogging.StrictLogging

case object JmsJndiConnectionFactoryBuilderBase {

  def connectionFactoryName(cfn: String) = JmsJndiConnectionFactoryBuilderUrlStep(cfn)
}

case class JmsJndiConnectionFactoryBuilderUrlStep(connectionFactoryName: String) {

  def url(theUrl: String) =
    JmsJndiConnectionFactoryBuilderFactoryStep(connectionFactoryName, theUrl)
}

case class JmsJndiConnectionFactoryBuilderFactoryStep(
    connectionFactoryName: String,
    url:                   String,
    credentials:           Option[Credentials] = None
) {

  def credentials(user: String, password: String) = copy(credentials = Some(Credentials(user, password)))

  def contextFactory(cf: String) =
    JmsJndiConnectionFactoryBuilder(cf, connectionFactoryName, url, credentials)
}

case class JmsJndiConnectionFactoryBuilder(
    contextFactory:        String,
    connectionFactoryName: String,
    url:                   String,
    credentials:           Option[Credentials]
) extends StrictLogging {

  def build() = {
    // create InitialContext
    val properties = new JHashtable[String, String]
    properties.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory)
    properties.put(Context.PROVIDER_URL, url)

    credentials.foreach { credentials =>
      properties.put(Context.SECURITY_PRINCIPAL, credentials.username)
      properties.put(Context.SECURITY_CREDENTIALS, credentials.password)
    }

    val ctx = new InitialContext(properties)
    logger.info(s"Got InitialContext $ctx")

    // create QueueConnectionFactory
    val qcf = ctx.lookup(connectionFactoryName).asInstanceOf[ConnectionFactory]
    logger.info(s"Got ConnectionFactory $qcf")
    qcf
  }

}
