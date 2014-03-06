##########
Benchmarks
##########

JMeter performance comparison
=============================

JMeter has a simple benchmark used for tracking performance changes between versions:
http://wiki.apache.org/jmeter/JMeterPerformance

Here's what the Gatling script looks like::

	package jmeter

	import com.excilys.ebi.gatling.core.Predef._
	import com.excilys.ebi.gatling.http.Predef._
	import com.excilys.ebi.gatling.http.Headers.Names._
	import bootstrap._

	class JMeterBenchmark extends Simulation {

	  val httpConf = httpConfig
	    .baseURL("http://localhost:8080/examples/servlets")
	    .acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.7")
	    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
	    .acceptEncodingHeader("gzip, deflate")
	    .acceptLanguageHeader("fr,fr")
	    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:8.0.1) Gecko/20100101 Firefox/8.0.1")

	  val scn = scenario("JMeter Benckmark with Gatling")
	    .repeat(30) {
	      exec(http("sessionExample1")
	        .get("/servlet/SessionExample")
	        .check(status.is(200)))
	        .pause(5)
	        .exec(http("sessionExample2Post")
	          .post("/servlet/SessionExample")
	          .param("dataname", "TOTO")
	          .param("datavalue", "TITI")
	          .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
	          .check(status.is(200)))
	        .pause(5)
	        .exec(http("index")
	          .get("/index.html")
	          .check(status.is(200)))
	        .pause(5)
	        .exec(http("sessionExample3")
	          .get("/servlet/SessionExample")
	          .check(status.is(200)))
	        .pause(5) //
	    }

	  setUp(scn.users(1500).ramp(100).delay(7).protocolConfig(httpConf))
	}

You can also compare the verbosity of both scripts:

* JMeter: https://gist.github.com/3870492
* Gatling: https://gist.github.com/3870509 

And here are the results, run in quite similar conditions:

* same local Tomcat 6.0.24 with same heap options,
* Mac OS X 10.8.2
* Hotspot 1.6.0_35
* 2.3 GHz Intel Core i7 proc
* default Gatling JVM options (512Mo heap)

See for yourself!

.. image :: img/benchmark-jmeter-transactions.png
	:alt: JMeter
	:scale: 65

JMeter 2.8

.. image:: img/benchmark-gatling-transactions.png
	:alt: Gatling
	:scale: 85

Gatling 1.3.2 