/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.model

import io.gatling.recorder.config.RecorderConfiguration.fakeConfig
import io.gatling.recorder.config.ConfigKeys
import io.gatling.recorder.enumeration.FilterStrategy
import io.gatling.core.filter.BlackList
import io.gatling.core.filter.WhiteList
import scala.concurrent.duration.Duration

object ModelFixtures {

  import java.util.ArrayList

      
     val config_basic = fakeConfig(Map(

    ConfigKeys.core.ClassName -> "TestSimulation",
    ConfigKeys.core.Encoding -> "UTF-8",
    ConfigKeys.core.Package -> "com.mypackage",
    ConfigKeys.core.RequestBodiesFolder -> "",
    ConfigKeys.core.SaveConfig -> "true",
    ConfigKeys.core.SimulationOutputFolder -> "/dir/a",
    ConfigKeys.core.ThresholdForPauseCreation -> 50,

    ConfigKeys.http.FollowRedirect -> true,
    ConfigKeys.http.FetchHtmlResources -> true,
    ConfigKeys.http.AutomaticReferer -> false,

    ConfigKeys.proxy.outgoing.Host -> "www.a.com", // TODO check whether null or empty string
    ConfigKeys.proxy.outgoing.Username -> "username",
    ConfigKeys.proxy.outgoing.Password -> "password",
    ConfigKeys.proxy.outgoing.Port -> 8888,
    ConfigKeys.proxy.outgoing.SslPort -> 8888,

    ConfigKeys.proxy.Port -> 8000,
    ConfigKeys.proxy.SslPort -> 8001,

    ConfigKeys.filters.FilterStrategy -> "Disabled",
    ConfigKeys.filters.BlacklistPatterns -> new ArrayList { "a"; "a" },
    ConfigKeys.filters.WhitelistPatterns -> new ArrayList { "a"; "a" }
    
  ))

 
   implicit var config = config_basic
 
       val config_no_proxy = fakeConfig(Map(

    ConfigKeys.core.ClassName -> "TestSimulation",
    ConfigKeys.core.Encoding -> "UTF-8",
    ConfigKeys.core.Package -> "com.mypackage",
    ConfigKeys.core.RequestBodiesFolder -> "",
    ConfigKeys.core.SaveConfig -> "true",
    ConfigKeys.core.SimulationOutputFolder -> "/dir/a",
    ConfigKeys.core.ThresholdForPauseCreation -> 50,

    ConfigKeys.http.FollowRedirect -> true,
    ConfigKeys.http.FetchHtmlResources -> true,
    ConfigKeys.http.AutomaticReferer -> false,

    ConfigKeys.proxy.outgoing.Host -> "", 
    ConfigKeys.proxy.outgoing.Username -> "",
    ConfigKeys.proxy.outgoing.Password -> "",
    ConfigKeys.proxy.outgoing.Port -> 0,
    ConfigKeys.proxy.outgoing.SslPort -> 0,

    ConfigKeys.proxy.Port -> 8000,
    ConfigKeys.proxy.SslPort -> 8001,

    ConfigKeys.filters.FilterStrategy -> "Disabled",
    ConfigKeys.filters.BlacklistPatterns -> new ArrayList { "a"; "a" },
    ConfigKeys.filters.WhitelistPatterns -> new ArrayList { "a"; "a" }
    
  ))
   
  // TODO - set up the filters
    val config_no_proxy_filter = fakeConfig(Map(

    ConfigKeys.core.ClassName -> "TestSimulation",
    ConfigKeys.core.Encoding -> "UTF-8",
    ConfigKeys.core.Package -> "com.mypackage",
    ConfigKeys.core.RequestBodiesFolder -> "",
    ConfigKeys.core.SaveConfig -> "true",
    ConfigKeys.core.SimulationOutputFolder -> "/dir/a",
    ConfigKeys.core.ThresholdForPauseCreation -> 50,

    ConfigKeys.http.FollowRedirect -> true,
    ConfigKeys.http.FetchHtmlResources -> true,
    ConfigKeys.http.AutomaticReferer -> false,

    ConfigKeys.proxy.outgoing.Host -> "", 
    ConfigKeys.proxy.outgoing.Username -> "",
    ConfigKeys.proxy.outgoing.Password -> "",
    ConfigKeys.proxy.outgoing.Port -> 0,
    ConfigKeys.proxy.outgoing.SslPort -> 0,
    
    ConfigKeys.proxy.Port -> 8000,
    ConfigKeys.proxy.SslPort -> 8001,

    ConfigKeys.filters.FilterStrategy -> "Disabled",
    ConfigKeys.filters.BlacklistPatterns -> new ArrayList { "a"; "a" },
    ConfigKeys.filters.WhitelistPatterns -> new ArrayList { "a"; "a" }
    
  ))
  
  val config_FetchHtmlResources_false = fakeConfig(Map(

    ConfigKeys.core.ClassName -> "TestSimulation",
    ConfigKeys.core.Encoding -> "UTF-8",
    ConfigKeys.core.Package -> "com.mypackage",
    ConfigKeys.core.RequestBodiesFolder -> "",
    ConfigKeys.core.SaveConfig -> "true",
    ConfigKeys.core.SimulationOutputFolder -> "/dir/a",
    ConfigKeys.core.ThresholdForPauseCreation -> 50,

    ConfigKeys.http.FollowRedirect -> true,
    ConfigKeys.http.FetchHtmlResources -> false,
    ConfigKeys.http.AutomaticReferer -> false,

    ConfigKeys.proxy.outgoing.Host -> "", 
    ConfigKeys.proxy.outgoing.Username -> "",
    ConfigKeys.proxy.outgoing.Password -> "",
    ConfigKeys.proxy.outgoing.Port -> 0,
    ConfigKeys.proxy.outgoing.SslPort -> 0,
    
    ConfigKeys.proxy.Port -> 8000,
    ConfigKeys.proxy.SslPort -> 8001,

    ConfigKeys.filters.FilterStrategy -> "Disabled",
    ConfigKeys.filters.BlacklistPatterns -> new ArrayList { "a"; "a" },
    ConfigKeys.filters.WhitelistPatterns -> new ArrayList { "a"; "a" }
    
  ))
  
  // from harreaderspec
    //val config_With_ResourcesFiltering = fakeConfig(Map(ConfigKeys.http.FetchHtmlResources -> true))
    // By default, we assume that we don't want to filter out the HTML resources
   // val config_without_FetchHtmlResources = fakeConfig(Map(ConfigKeys.http.FetchHtmlResources -> false))
   
   
  val reqHeaders = Map("Connection" -> "keep-alive", "Accept" -> "*/*",
    "Accept-Language" -> "en-us", "Accept-Encoding" -> "gzip, deflate",
    "User-Agent" -> "Mozilla/5.0 ")

  val reqHeadersForm = Map("Connection" -> "keep-alive", "Accept" -> "*/*",
    "Accept-Language" -> "en-us", "Accept-Encoding" -> "gzip, deflate",
    "User-Agent" -> "Mozilla/5.0 ",
    "Content-Type" -> "application/x-www-form-urlencoded")

  val reqHeadersUnique = Map("AAAAAAA" -> "ZZZZZZZZ","AAAAAAA2" -> "ZZZZZZZZ2")

  val contentParams = List(("a", "b"), ("c", "d"))
  val contentBody = (new String("{a=b,c=d}")).getBytes()
  val reqBodyParams = Option(RequestBodyParams(contentParams))
  val reqBody = Option(RequestBodyBytes(contentBody))

  val r1 = RequestModel("http://gatling.io/main.css", "GET", reqHeaders, None, 200, List.empty, Option(""))
  val r1a = RequestModel("http://gatling.io/main2.css?a=b", "POST", reqHeadersForm, reqBodyParams, 200, List.empty, Option(""))
  val r1b = RequestModel("http://gatling.io/wrong/main3", "POST", reqHeadersForm, reqBody, 200, List.empty, Option(""))
  val r1c = RequestModel("http://gatling.io/wrong/?fdsf=fdsf", "GET", reqHeaders ++ reqHeadersUnique, None, 200, List.empty, Option(""))

  val r2 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 200, List.empty, Option(""))
  val r2redirect302 = RequestModel("http://gatling.io/main2-302.css", "GET", Map.empty, None, 302, List.empty, Option(""))
  val r2redirect301 = RequestModel("http://gatling.io/main2-301.css", "GET", Map.empty, None, 302, List.empty, Option(""))

  val r3 = RequestModel("http://gatling.io/2h42g3hj.versioned.main3.css", "GET", Map.empty, None, 200, List.empty, Option(""))

  val r99error400 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 400, List.empty, Option(""))
  val r99error401 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 401, List.empty, Option(""))
  val r99error402 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 402, List.empty, Option(""))
  val r99error500 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 500, List.empty, Option(""))
  val r99error503 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 503, List.empty, Option(""))
  
  
  val basicModel: SimulationModel = {

    val sim = SimulationModel()

    val time = System.currentTimeMillis()

    sim += (time -> r1)
    sim += (time + 1 -> r1a)
    sim += (time + 2 -> r1b)
    sim += (time + 3 -> r1c)
    sim += (time + 4 -> r2)
    sim += (time + 5 -> r3)
    sim.newNavigation(time + 100, "first navigation")
    
    sim addPause (Duration.create(50, "milliseconds"))  // this pause should be placed in the previous navigation
    sim += (time + 200 -> r2)
    sim += (time + 201 -> r2)
    sim addPause (Duration.create(50, "milliseconds"))
    sim += (time + 303 -> r3)
    sim.newNavigation(time + 304, "second navigation")

    sim += (time + 306 -> r3)
    sim.newNavigation(time + 400, "3rd navigation")

    sim += (time + 407 -> r3)
    
    sim.postProcess

    sim
  }

    val redirectingModel: SimulationModel = {

    val sim = SimulationModel()
    val time = System.currentTimeMillis()

    sim += (time -> r2redirect302)
    sim += (time + 1 -> r1a)
    sim += (time + 2 -> r2redirect301)
    sim += (time + 3 -> r1c)
    sim += (time + 4 -> r2)
    sim += (time + 5 -> r2redirect302)
    sim += (time + 6 -> r2)
    sim.newNavigation(time + 100, "first navigation")
    
    sim.postProcess

    sim
  }
    
    // check the generated checks...
  val erroringModel: SimulationModel = {

    val sim = SimulationModel()
    val time = System.currentTimeMillis()

    sim += (time -> r99error400)
    sim += (time + 1 -> r99error401)
    sim += (time + 2 -> r99error402)
    sim += (time + 3 -> r99error500)
    sim += (time + 4 -> r99error503)
    sim.newNavigation(time + 100, "first navigation")
    
    sim.postProcess

    sim
  }
}