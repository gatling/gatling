########
Concepts
########

Scenarios
=========

What is a scenario?
-------------------

To represent users' behaviors, testers will have to define scenarios which will be written as scripts given to Gatling. These scenarios can be the result of measurements on the running application with analytics tools, or expected behavior from the future users of a new application. In any case, the creation of these scenarios is the key to meaningful results of the stress test.

A scenario represents a typical user's behavior, for example, in an e-commerce application. A standard scenario could be: 

1. Access home page
2. Select a category of product
3. Make a search in this category
4. Open a product description
5. Go back
6. Open another product description
7. Buy product
8. Log in
9. Confirm buying
10. Pay
11. Log out

This is a functional scenario, understandable by anyone. There can be as many scenarios as different use cases of the application. We can also imagine the use of a product comparator or an admin panel; each use case can be described as a scenario.

Scenarios in Gatling
--------------------
In Gatling, scenarios are represented as easy-to-maintain scripts in conjunction with a DSL (`Domain Specific Language <http://en.wikipedia.org/wiki/Domain-specific_language>`_). This allows fast writing of scenarios and easy understanding of existing scenarios. Here is a simple example of scenario written for Gatling::

	scenario("Standard User")
		.exec( http("Access Github").get("http://github.com") )
		.pause(2, 3)
		.exec( http("Search for 'gatling'").get("http://github.com/search").queryParam("q","gatling"))
		.pause(2))

As we can easily guess, this scenario:

* Is named "Standard User"
* Contains 2 HTTP Requests
* Contains 2 pauses

Pauses are here to simulate the reading or thinking time of the user. Indeed, when a user clicks on a link, the page has to be loaded and the user will read it and decide what to do next.

The HTTP requests are the ones which are actually sent to the tested application when a user clicks on a button or a link. Each HTTP Request is easily read:

1. **Access Github** is a **GET** request pointing at **http://github.com**
2. **Search for 'gatling'** is a **GET** request pointing at **http://github.com/search?q=gatling**

For a complete description of scenarios, see the Reference articles

Simulation
==========

Definition
----------

A simulation is a load test. It is made of different scenarios, each representing a typical user behavior. Here is an example of simulation definition::

	val stdUser = scenario("Standard User")...
	val admUser = scenario("Admin User")...
	val advUser = scenario("Advanced User")...

	setUp(
	  stdUser.users(2000).ramp(60),
	  admUser.users(5).ramp(400).delay(60),
	  advUser.users(500).ramp(200)
	)

Each scenario is defined first; here, we have three scenarios representing *standard users*, *advanced users* and *administrators*. For each population of users, the **Number of users** is defined along with a **Ramp** and a **Delay**.

* The **number of users** is self explanatory: it represents the number of users that will be simulated using this scenario.
* The **ramp** is used to simulate the progressive arriving of user on the application. _ramp(60)_ indicates that all users must be started within 60 seconds. Gatling will start each user at regular intervals; for example, if we have 60 users and a ramp of 60 seconds, a user will be started each second.
* The **delay** is used to define when the scenario will start regarding the beginning of the simulation. As shown below, in the previous simulation, the admUser scenario will start after the others with a delay of 60 seconds.

.. image:: img/delay_explained.png
	:alt: Delay explained

.. note:: Using a ramp can be critical for JVM based applications as the JIT compiler identifies hot spots. (The JVM "warms up")

Session
-------

For each simulated user, there is a session. This session is accessible to one and only one user at runtime. It allows Gatling to store information while processing the scenarios, but it also allows testers to dynamically store data and use it in their requests.

For more information, check the :ref:`Session reference section <session>`.

Feeders
=======

When the tested application offers the possibility to authenticate, tests should take this into consideration and use data to test log in, log out, actions allowed only for certain users, and so on. 

Gatling doesn't provide the tools to generate this test data. However, it allows you to take existing data and feed scenarios with them thanks to Feeders

For more information, check the :ref:`Feeders reference section <feeders>`.

Checks
======

Each time a request is sent, a response is eventually sent by the server. Gatling is able to analyze this response with checks.

A check is a response processor that captures some part of it and verifies that it is what the user expects. For example, when sending a HTTP request, one could expect a redirect; with a check, you can verify that the status of the response is actually a 30X code.

These checks allow you to be sure that the results obtained during the simulation are not related to server malfunctions. For example, Gatling could retrieve responses with good response times, but these responses may not be what they should, therefore, the analysis you could make on the results given by Gatling may be wrong.

For more information, check the :ref:`Checks reference section <checks>`.

Reports
=======

By default, Gatling automatically generates reports at the end of a simulation. They consist in HTML files, therefore, they are portable and they can be viewed on any device with a web browser.

Reports are detailed in the :ref:`corresponding page <Reports>`