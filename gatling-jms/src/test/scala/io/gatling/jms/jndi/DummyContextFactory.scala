/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.util
import javax.jms.{ Connection, ConnectionFactory, JMSContext }
import javax.naming.Context
import javax.naming.spi.InitialContextFactory

import org.apache.activemq.jndi.ReadOnlyContext

class DummyContextFactory extends InitialContextFactory {
  override def getInitialContext(environment: util.Hashtable[_, _]): Context = {
    val bindings = new util.HashMap[String, Object]()
    bindings.put("DummyConnectionFactory", new DummyConnectionFactory(environment))
    new ReadOnlyContext(environment, bindings)
  }
}

class DummyConnectionFactory(env: util.Hashtable[_, _]) extends ConnectionFactory {
  val environment: util.Hashtable[_, _] = env

  override def createConnection(): Connection = null

  override def createConnection(userName: String, password: String): Connection = null

  override def createContext(sessionMode: Int): JMSContext = null

  override def createContext(userName: String, password: String, sessionMode: Int): JMSContext = null

  override def createContext(userName: String, password: String): JMSContext = null

  override def createContext(): JMSContext = null
}
