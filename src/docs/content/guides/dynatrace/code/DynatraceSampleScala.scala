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

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DynatraceSampleScala {

//#dynatrace
private val Hostname = InetAddress.getLocalHost.getHostName

// Source Id identifies the product that triggered the request
private val SI = "GATLING"

// The Load Test Name uniquely identifies a test execution
private val LTN =
  getClass.getSimpleName +
    "_" +
    LocalDateTime.now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

val httpProtocol = http
  .sign { (request, session) =>
    // Virtual User ID of the unique user who sent the request
    val VU = s"${Hostname}_${session.scenario}_${session.userId}"

    // Test Step Name is a logical test step within your load testing script
    val TSN = request.getName

    // Load Script Name - name of the load testing script.
    val LSN = session.scenario

    // Page Context provides information about the document
    val PC = session.groups.mkString(",")

    request.getHeaders.set(
      "x-dynaTrace",
      s"VU=$VU;SI=$SI;TSN=$TSN;LSN=$LSN;LTN=$LTN;PC=$PC"
    )
  }
//#dynatrace
}
