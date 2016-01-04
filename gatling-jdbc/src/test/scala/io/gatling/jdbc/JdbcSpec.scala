/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.jdbc

import java.sql.DriverManager

import io.gatling.commons.util.Io.withCloseable

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
