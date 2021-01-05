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

import java.{ util => ju }
import javax.naming.Context

import scala.jdk.CollectionConverters._

import io.gatling.BaseSpec
import io.gatling.jms.Predef.jmsJndiConnectionFactory

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
    val contextEnv = new ju.HashMap[Any, Any](factory.asInstanceOf[DummyConnectionFactory].environment).asScala.toSeq
      .map { case (k, v) => k.toString -> v.toString }
      .sortBy(_._1)
    contextEnv shouldBe Seq(
      "extProperty" -> "extValue",
      Context.INITIAL_CONTEXT_FACTORY -> classOf[DummyContextFactory].getName,
      Context.PROVIDER_URL -> "testUrl",
      Context.SECURITY_CREDENTIALS -> "secret",
      Context.SECURITY_PRINCIPAL -> "user",
      "testProperty" -> "testValue"
    )
  }
}
