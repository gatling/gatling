package io.gatling.jms.jndi

import java.util
import javax.jms.{Connection, ConnectionFactory}
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
}
