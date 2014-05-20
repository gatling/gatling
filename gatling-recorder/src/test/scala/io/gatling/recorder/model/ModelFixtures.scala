package io.gatling.recorder.model

import io.gatling.recorder.config.RecorderConfiguration.fakeConfig

object ModelFixtures {

  
    val CONFIG_ROOT = "recorder"

    val FILTER_STRATEGY = "recorder.filters.filterStrategy"
    val WHITELIST_PATTERNS = "recorder.filters.whitelist"
    val BLACKLIST_PATTERNS = "recorder.filters.blacklist"

    val AUTOMATIC_REFERER = "recorder.http.automaticReferer"
    val FOLLOW_REDIRECT = "recorder.http.followRedirect"
    val FETCH_HTML_RESOURCES = "recorder.http.fetchHtmlResources"

    val LOCAL_PORT = "recorder.proxy.port"
    val LOCAL_SSL_PORT = "recorder.proxy.sslPort"

    val PROXY_HOST = "recorder.proxy.outgoing.host"
    val PROXY_USERNAME = "recorder.proxy.outgoing.username"
    val PROXY_PASSWORD = "recorder.proxy.outgoing.password"
    val PROXY_PORT = "recorder.proxy.outgoing.port"
    val PROXY_SSL_PORT = "recorder.proxy.outgoing.sslPort"

    val ENCODING = "recorder.core.encoding"
    val SIMULATION_OUTPUT_FOLDER = "recorder.core.outputFolder"
    val REQUEST_BODIES_FOLDER = "recorder.core.requestBodiesFolder"
    val SIMULATION_PACKAGE = "recorder.core.package"
    val SIMULATION_CLASS_NAME = "recorder.core.className"
    val THRESHOLD_FOR_PAUSE_CREATION = "recorder.core.thresholdForPauseCreation"

    private implicit var config = fakeConfig(Map(
      FOLLOW_REDIRECT -> true,
      FETCH_HTML_RESOURCES -> true,
      PROXY_HOST -> "www.a.com", // TODO check whether null or empty string
      PROXY_USERNAME -> "username",
      PROXY_PASSWORD -> "password",
      PROXY_PORT -> 8888,
      PROXY_SSL_PORT -> 8888))
      
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

    val basicModel = {
    
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
    sim += (time + 203 -> r3)
    sim.newNavigation(time + 300, "second navigation")

    sim += (time + 301 -> r3)
    sim.newNavigation(time + 400, "3rd navigation")

    sim.postProcess

  sim
    }
  
}