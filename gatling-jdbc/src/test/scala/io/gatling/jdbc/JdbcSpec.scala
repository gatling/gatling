package io.gatling.jdbc

import java.sql.DriverManager
import io.gatling.core.util.Io.withCloseable

trait JdbcSpec {

  val Username = "sa"
  val Password = ""

  def withDatabase(dbName: String, initScriptName: String)(block: String => Unit) = {
    val jdbcUrl = s"jdbc:h2:mem:$dbName"
    val fullUrl = s"$jdbcUrl;INIT=RUNSCRIPT FROM 'classpath:$initScriptName'"
    Class.forName("org.h2.Driver")
    withCloseable(DriverManager.getConnection(fullUrl, Username, Password)) { conn => // Kept open, but unused
      block(jdbcUrl)
    }
  }
}
