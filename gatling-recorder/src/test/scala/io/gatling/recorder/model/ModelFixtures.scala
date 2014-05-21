package io.gatling.recorder.model

import io.gatling.recorder.config.RecorderConfiguration.fakeConfig
import io.gatling.recorder.config.ConfigKeys
import io.gatling.recorder.enumeration.FilterStrategy
import io.gatling.core.filter.BlackList
import io.gatling.core.filter.WhiteList
import scala.concurrent.duration.Duration


object ModelFixtures {

import java.util.ArrayList
     // var list  = new ArrayList {"a"} //
      //val listIs = list instanceof  Iterable
      
    private implicit val config = fakeConfig(Map(
      
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
      ConfigKeys.filters.BlacklistPatterns -> new ArrayList {"a";"a"} ,
      ConfigKeys.filters.WhitelistPatterns -> new ArrayList {"a";"a"} 
      ))
      
      
      
    val reqHeaders = Map("Connection" -> "keep-alive", "Accept" -> "*/*",
      "Accept-Language" -> "en-us", "Accept-Encoding" -> "gzip, deflate",
      "User-Agent" -> "Mozilla/5.0 ")

    val reqHeadersForm = Map("Connection" -> "keep-alive", "Accept" -> "*/*",
      "Accept-Language" -> "en-us", "Accept-Encoding" -> "gzip, deflate",
      "User-Agent" -> "Mozilla/5.0 ",
      "Content-Type" -> "application/x-www-form-urlencoded")

    val reqHeadersUnique = Map("AAAAAAA" -> "ZZZZZZZZ")

    val contentParams = List(("a", "b"), ("c", "d"))
    val contentBody = (new String("{a=b,c=d}")).getBytes()
    val reqBodyParams = Option(RequestBodyParams(contentParams))
    val reqBody = Option(RequestBodyBytes(contentBody))

    val r1 = RequestModel("http://gatling.io/main.css", "GET", reqHeaders, None, 200, List.empty, Option(""))
    val r1a = RequestModel("http://gatling.io/main2.css?a=b", "POST", reqHeadersForm, reqBodyParams, 200, List.empty, Option(""))
    val r1b = RequestModel("http://gatling.io/wrong/main3", "POST", reqHeadersForm, reqBody, 200, List.empty, Option(""))
    val r1c = RequestModel("http://gatling.io/wrong/?fdsf=fdsf", "GET", reqHeaders ++ reqHeadersUnique, None, 200, List.empty, Option(""))

    val r2 = RequestModel("http://gatling.io/main2.css", "GET", Map.empty, None, 200, List.empty, Option(""))

    val r3 = RequestModel("http://gatling.io/2h42g3hj.versioned.main3.css", "GET", Map.empty, None, 200, List.empty, Option(""))

    val basicModel:SimulationModel = {
    
    val sim = SimulationModel()

    val time = System.currentTimeMillis()

    sim += (time -> r1)
    sim += (time + 1 -> r1a)
    sim += (time + 2 -> r1b)
    sim += (time + 2 -> r1c)
    sim += (time + 3 -> r2)
    sim += (time + 4 -> r3)
    sim.newNavigation(time + 100, "first navigation")

    sim += (time + 200 -> r2)
    sim += (time + 201 -> r2)
    sim addPause(Duration.create(50, "milliseconds") )
    sim += (time + 303 -> r3)
    sim.newNavigation(time + 300, "second navigation")

    sim += (time + 306 -> r3)
    sim.newNavigation(time + 400, "3rd navigation")

    sim.postProcess

  sim
    }
  
}