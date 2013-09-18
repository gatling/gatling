.. _advanced-usage:

##############
Advanced Usage
##############

If you are here, you probably know a bit of Gatling. To show the advanced features of Gatling, we will continue to improve the scenario we developed in :ref:`first-steps-with-gatling`. This scenario and the advanced one are available as examples in the Gatling bundle.

Checks
======

What are they?
--------------

Checks are an important feature of Gatling, as you may have noticed, there are some added by the Recorder::

	.exec(http("request_1")
	      .get("/")
	      .headers(headers_1)
	      .check(status.is(302)) // This is our check!
	  )

Making sure that the scenario goes well
---------------------------------------

What if the login process succeeded (we actually got our 302 status code), but the page shown is an error page? (Custom 404 for example).

We would like to verify that we actually are viewing the list of our accounts. To do so, we will use a Regular Expression Check::

	.exec(
	  http("request_4")
	    .get("/private/bank/accounts.html")
	    .headers(headers_4)
	    .check(regex("""<td class="number">ACC${account_id}</td>""").exists)
	)

.. note:: The ``${account_id}`` syntax is the same as for the feeders, we will explain it right after.

Here, we verify that the string ``"""<td class="number">ACC${account_id}</td>"""`` is present in the body of the response we received.

Saving data
-----------

Checks can also be used for saving the extracted data so that it can be reused further in the scenario flow.

For example, one can use a capture group on a regex::

	check(
	  regex("""<a href="/excilys-bank-web/private/bank/account/(ACC[0-9]*)/operations.html">""")
	  .saveAs("acc1"))

Here, if the regex matches the response body, the captured group first occurrence will be saved in the user's session under the ``acc1`` key.

For more information about Checks, see the :ref:`Checks reference section <checks>`.

.. _the-session:

The Session
===========

When a simulation is run, each user has its own session in Gatling. It is used by the engine to exchange data between requests, but you can also benefit from this session.

What if you want to make your scenario more dynamic? For now, we store the ``account_id`` in our Feeder, this is quite annoying, and could lead to complex file generation. We want to remove the ``account_id`` from our feeder and get it directly from the ``Account Page``.

To do so, we can use the ``saveAs(sessionKey: String)`` method; it will save the value captured by the check into the session under the key ``sessionKey``. Thanks to this feature, we can now change our :ref:`feeder <feeders>`::

	/* user_credentials.csv */
	username,password
	user1,password1
	...
	user10,password10

.. note:: We use a different name, because now we only have the user's credentials.

:: 

	/* Old Scenario */                                      |  /* New Scenario */
	-------------------------------------------------------------------------------------------------------
	feed(csv("user_information.csv"))                       |  feed(csv("user_credentials.csv"))
	...                                                     |  ...
	.check(                                                 |  .check(
	  regex("""<td class="number">ACC${account_id}</td>""") |    regex("""<td class="number">ACC(\d+)</td>""")
	  .exists                                               |      .saveAs("account_id")
	)                                                       |  )

As you might have seen, we use an Expression Language to get values from the session. If you have used ELs from other languages, you can recognize the syntax: ``${ variable }``. Here, variable represents the key of the session value we want to get. See :ref:`here <session-el>` for more details.

.. note:: We also removed the exists strategy which is actually a default automatically added if you don't explicitaly specify one.

Conditional Execution
=====================

You might have to execute different actions depending on a value in the session; to do so, you can use conditional execution. As an example, we could simulate that user7 is the only one to click on the logout button::


	.doIf("${username}", "user7") {
	    exec(
	      http("request_9")
	        ...
	    )
	    .pause(0 milliseconds, 100 milliseconds)
	    .exec(
	      http("request_10")
	        ...
	    )
	}

.. _scala-functions:

Using Scala Functions
---------------------

``doIf`` and ``asLongAs`` condition can be expressed using two strings that will be tested for equality (as shown in the previous example). It can also be expressed as a Scala Function of type ``Session => Boolean``.

To illustrate this, we can simulate the opposite of the previous example; that is to say every user will click on ``logout`` except user7::

	.doIf(session => session.getAttribute("username") != "user7") {
	  exec
	    ...
	}

.. note:: This version will be used in our scenario.

.. _multi-scenarios:

Multi-Scenarios Simulations
===========================

What if your web application is used by different kind of users? You might have administrators, users, advanced users, etc. We can easily simulate the use of the application by each of these groups; with Gatling, you can even simulate the use of the application by all these groups at the same time.

To do so, just define another scenario in your simulation file::

	val scn = scenario("Scenario Name")
	        ...

	val otherScn = scenario("Other Scenario Name")
	        ...

There you go, you have defined several scenarios in one simulation file. If you try to run this simulation like this, you'll notice that Gatling won't see the difference; indeed, you need to add this scenario to the List of scenarios to be simulated::

	setUp(
	  scn.users(10).ramp(10).protocolConfig(httpConf),
	  otherScn.users(5).ramp(20).protocolConfig(httpConf)
	)

.. note:: As you can see, the ``httpConf`` can be reused for several scenarios.

If you want to delay the beginning of a scenario, you can use the method ``delay(duration: Int)``::

	otherScn.users(5).ramp(20).delay(30).protocolConfig(httpConf)

With this configuration ``otherScn`` will start 30 seconds after ``scn``.

Simulation Modularization
=========================

If you are to reuse scenarios in different simulations, you can modularize it thanks to the import mechanism of Scala. For example, in the previous example, we can split the simulation file into 2 scenario files, 1 header file and 1 simulation file::

    user-files/
    |_ simulations
       |_ advanced
          |_ AdvancedExampleSimulation.scala
          |_ Headers.scala
          |_ SomeOtherScenario.scala
          |_ SomeScenario.scala
       |_ basic
          |_ BasicExampleSimulation.scala

The content of the files are the following:

::

	/* AdvancedExampleSimulation.scala */
	package advanced
	import com.excilys.ebi.gatling.core.Predef._
	import com.excilys.ebi.gatling.http.Predef._
	import com.excilys.ebi.gatling.jdbc.Predef._

	class AdvancedExampleSimulation extends Simulation {
	  val urlBase = "http://excilysbank.gatling.cloudbees.net"
	  val httpConf = httpConfig.baseURL(urlBase)

	  setUp(SomeScenario.scn.users(10).ramp(10).protocolConfig(httpConf),
	        SomeOtherScenario.otherScn.users(5).ramp(20).delay(30).protocolConfig(httpConf))
	}

::

	/* SomeScenario.scala */
	package advanced
	import com.excilys.ebi.gatling.core.Predef._
	import com.excilys.ebi.gatling.http.Predef._
	import com.excilys.ebi.gatling.jdbc.Predef._
	import Headers._

	object SomeScenario {
	  val scn = scenario("Scenario name")
	    .exec(
	      http("request_1")
	        .get("/")
	        .headers(headers_1)
	        .check(status.is(302)))
	        ...
	}

::

	/* SomeOtherScenario.scala */
	package advanced
	import com.excilys.ebi.gatling.core.Predef._
	import com.excilys.ebi.gatling.http.Predef._
	import com.excilys.ebi.gatling.jdbc.Predef._
	import Headers._

	object SomeOtherScenario {
	  val otherScn = scenario("Other Scenario Name")
	    .exec(
	      http("other_request_1")
	        .get("/")
	        .check(status.is(302)))
	        ...
	}

::

	/* Headers.scala */
	package advanced

	object Headers {
	  val headers_1 = Map(
	  ...
	}

You can now run the simulation if you want and look at the generated reports. Description of simulation scripts is finished, we are now going to discuss some other features of Gatling.

The end of the tutorial
=======================

That's it for the tutorial! You can learn more about Gatling reading the reference articles of this wiki (accessible from the right sidebar).

If you want to address specific problems, you might find what you need in the cookbook.

Happy Gatling!