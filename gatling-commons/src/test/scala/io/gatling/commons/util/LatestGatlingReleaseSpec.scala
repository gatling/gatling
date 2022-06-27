/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import java.time.{ Instant, ZoneOffset, ZonedDateTime }

import io.gatling.BaseSpec

class LatestGatlingReleaseSpec extends BaseSpec {

  "parseMavenCentralResponse" should "successfully extra version" in {
    val response =
      """{"responseHeader":{"status":0,"QTime":8,"params":{"q":"g:io.gatling AND a:gatling-core AND p:jar","core":"gav","indent":"off","fl":"id,g,a,v,p,ec,timestamp,tags","start":"","sort":"score desc,timestamp desc,g asc,a asc,v desc","rows":"1","wt":"json","version":"2.2"}},"response":{"numFound":66,"start":0,"docs":[{"id":"io.gatling:gatling-core:3.7.6","g":"io.gatling","a":"gatling-core","v":"3.7.6","p":"jar","timestamp":1646240604000,"ec":["-sources.jar",".pom","-javadoc.jar",".jar"],"tags":["core","gatling"]}]}}"""
    LatestGatlingRelease.parseMavenCentralResponse(response) shouldBe GatlingVersion(
      "3.7.6",
      ZonedDateTime.ofInstant(Instant.ofEpochMilli(1646240604000L), ZoneOffset.UTC)
    )
  }
}
