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
    contextEnv should contain allOf(
      Context.INITIAL_CONTEXT_FACTORY -> classOf[DummyContextFactory].getName,
      Context.PROVIDER_URL -> "testUrl",
      Context.SECURITY_PRINCIPAL -> "user",
      Context.SECURITY_CREDENTIALS -> "secret",
      "testProperty" -> "testValue",
      "extProperty" -> "extValue"
    )
  }
}
