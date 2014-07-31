########
Concepts
########

Virtual User
============

Some load testing tools, such as `ab <http://httpd.apache.org/docs/2.2/programs/ab.html>`__  or `wrk <https://github.com/wg/wrk>`__ are very efficient on url bashing, but can't deal with logic between requests.

Advanced load testing tools such as Gatling can deal with virtual users, each one having its own data and maybe taking a distinct browsing path.

Some other tools implement those *virtual users* as threads. Gatling implements them as messages, which scales much better and can deal easily with thousands of concurrent users.

Scenario
========

To represent users' behaviors, testers will have to define scenarios which will be written as scripts given to Gatling.

These scenarios can be the result of measurements on the running application with analytics tools, or expected users behavior of a new application.
In any case, the creation of these scenarios is the key to meaningful results of the load test.

A scenario represents a typical user behavior. It's a workflow that virtual users will follow.

For example, a standard e-commerce application scenario could be:

1. Access home page
2. Select a browse category
3. Make a search in this category
4. Open a product description
5. Go back
6. Open another product description
7. Buy product
8. Log in
9. Checkout
10. Payment
11. Log out

Scenarios are represented as scripts in conjunction with a DSL (`Domain Specific Language <http://en.wikipedia.org/wiki/Domain-specific_language>`_).
This allows fast writing of scenarios and easy maintenance of existing scenarios.

Here is a simple example of a scenario::

  scenario("Standard User")
    .exec(http("Access Github").get("https://github.com"))
    .pause(2, 3)
    .exec(http("Search for 'gatling'").get("https://github.com/search?q=gatling"))
    .pause(2))

As we can easily guess, this scenario:

* is named "Standard User"
* contains 2 HTTP Requests
* contains 2 pauses

*Pauses* are used to simulate user think time.
Indeed, when a user clicks on a link, the page has to be loaded and the user will read it and decide what to do next.

HTTP requests are what is actually sent to the application under test when a user clicks on a button or a link.
Each HTTP Request is easy to grasp:

1. *Access Github* is a *GET* request pointing at *http://github.com*
2. *Search for 'gatling'* is a *GET* request pointing at *http://github.com/search?q=gatling*

For more information, check the :ref:`Scenario reference section <scenario>`.

Simulation
==========

A simulation is a description of the load test. It describes how, possibly several, user populations will run: which scenario they will execute and how new virtual users will be injected.

Here is an example of simulation definition::

  val stdUser = scenario("Standard User")...
  val admUser = scenario("Admin User")...
  val advUser = scenario("Advanced User")...

  setUp(
    stdUser.inject(atOnceUsers(2000)),
    admUser.inject(nothingFor(60 seconds), rampUsers(5) over (400 seconds)),
    advUser.inject(rampUsers(500) over (200 seconds))
  )

For more information, check the :ref:`Simulation Setup reference section <simulation-setup>`.

Session
=======

Each virtual user is backed by a *Session*.
Those *Sessions* are the actually messages that go down the scenario workflow.
A *Session* is basically a state placeholder, where testers can inject or capture and store data.

For more information, check the :ref:`Session reference section <session>`.

Feeders
=======

When the tested application offers the possibility to authenticate, tests should take this into consideration and use data to test log in, log out, actions allowed only for certain users, and so on. 

Gatling doesn't provide the tools to generate this test data.

*Feeders* are a convenient API for testers to inject data coming from an external source into the virtual users' sessions.

For more information, check the :ref:`Feeders reference section <feeder>`.

Checks
======

Each time a request is sent to the server, a response is eventually sent by the server back to Gatling.

Gatling is able to analyze this response with *Checks*.

A check is a response processor that captures some part of it and verifies that it meets some given condition.
For example, when sending an HTTP request, you could expect a redirect; with a check, you can verify that the status of the response is actually a 30x code.

*Checks* can also be used to capture some elements and store them into the Session so that they can be reused later, for example to build the next request.

For more information, check the :ref:`Checks reference section <http-check>`.

Assertions
==========

The *Assertions* are used to define acceptance criteria on Gatling statistics (e.g. response time 99th percentile) that would make Gatling fail and return an error status code for the test as a whole.

For more information, check the :ref:`Assertions reference section <assertions>`.

Reports
=======

By default, reports are automatically generated at the end of a simulation.
They consist of HTML files. Therefore, they are portable and they can be viewed on any device with a web browser.

For more information, check the :ref:`Reports reference section <reports>`.
