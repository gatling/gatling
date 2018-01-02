/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import javax.naming.Context

import io.gatling.BaseSpec
import io.gatling.jms.Predef.jmsJndiConnectionFactory

import scala.collection.JavaConverters.dictionaryAsScalaMapConverter

class JmsJndiConnectionFactoryBuilderSpec extends BaseSpec {

  "jndi connection factory" should "pass all properties to InitialContext" in {

    val jndiCf = jmsJndiConnectionFactory
      .connectionFactoryName("DummyConnectionFactory")
      .url("testUrl")
      .credentials("user", "secret")
      .property("testProperty", "testValue")
      .property("extProperty", "extValue")
      .contextFactory(classOf[DummyContextFactory].getName)

    val factory = jndiCf.build()
    val contextEnv = factory.asInstanceOf[DummyConnectionFactory].environment.asScala
    contextEnv should contain allOf (
      Context.INITIAL_CONTEXT_FACTORY -> classOf[DummyContextFactory].getName,
      Context.PROVIDER_URL -> "testUrl",
      Context.SECURITY_PRINCIPAL -> "user",
      Context.SECURITY_CREDENTIALS -> "secret",
      "testProperty" -> "testValue",
      "extProperty" -> "extValue"
    )
  }
}
