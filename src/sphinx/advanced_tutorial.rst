.. _advanced_tutorial:

#################
Advanced Tutorial
#################

In this section, we assume that you have already gone through the :ref:`quickstart<quickstart>` section and that you  have a basic simulation to work with. We will apply to it a suite of refactoring to introduce more advanced concepts and DSL constructs.

Step 01: Isolate processes
==========================

Presently our Simulation is one big monolithic scenario.

So first let split it into composable business processes, like one would do with PageObject pattern with Selenium.
This way, you'll be able to easily reuse some parts and build complex behaviors without sacrificing maintenance.

In our scenario we have three separated processes:
  * Search: search models by name
  * Browse: browse the list of models
  * Edit: edit a given model

We are going to extract those chains and store them into *objects*.
Objects are native Scala singletons.
You can create those into dedicated files, or directly into the same file as the Simulation.

::

  object Search {

    val search = exec(http("Home") // let's give proper names, they are displayed in the reports, and used as keys
        .get("/"))
      .pause(7)
      .exec(http("Search")
        .get("/computers")
        .queryParam("""f""", """macbook"""))
      .pause(2)
        .exec(http("Select")
        .get("/computers/6"))
      .pause(3)
  }

  object Browse {

    val browse = ...
  }

  object Edit {

    val edit = ...
  }

We can now rewrite our scenario using these reusable business processes::

   val scn = scenario("Scenario Name").exec(Search.search, Browse.browse, Edit.edit)

Step 02: Configure virtual users
================================

So, this is great, we can load test our server with... one user!
Let's increase the number of users.

Let define two populations of users:
  * *regular* users: they can search and browse computer models.
  * *admin* users: they can also edit computer models.

Translating into scenario this gives::

  val users = scenario("Users").exec(Search.search, Browse.browse)
  val admins = scenario("Admins").exec(Search.search, Browse.browse, Edit.edit)

To increase the number of simulated users, all you have to do is to change the configuration of the simulation as follows::

  setUp(users.inject(atOnceUsers(10)).protocols(httpConf))


Here we set only 10 users, because we don't want to flood our test web application. *Please*, be kind and don't crash our Heroku instance ;-)

If you want to simulate 3 000 users, you might not want them to start at the same time.
Indeed, they are more likely to connect to your web application gradually.

Gatling provides the ``rampUsers`` builtin to implement this behavior.
The value of the ramp indicates the duration over which the users will be linearly started.

In our scenario let's have 10 regular users and 2 admins, and ramp them over 10 seconds so we don't hammer the server::

  setUp(
    users.inject(rampUsers(10) over (10 seconds)),
    admins.inject(rampUsers(2) over (10 seconds))
  ).protocols(httpConf)

Step 03: Use dynamic data with Feeders and Checks
=================================================

We have set our simulation to run a bunch of users, but they all search for the same model.
Wouldn't it be nice if every user could search a different model name?

We need dynamic data so that all users don't play the same and we end up with a behavior completely different from the live system (caching, JIT...).
This is where Feeders will be useful.

Feeders are data sources containing all the values you want to use in your scenarios.
There are several types of Feeders, the most simple being the CSV Feeder: this is the one we will use in our test.

First let's create a file named *search.csv* and place it in ``user-files/data`` folder.

This file contains the following lines::

	searchCriterion,searchComputerName
	Macbook,MacBook Pro
	eee,ASUS Eee PC 1005PE

Let's then declare a feeder and use it to feed our users::

  object Search {

    val feeder = csv("search.csv").random // 1, 2

    val search = exec(http("Home")
      .get("/"))
      .pause(1)
      .feed(feeder) // 3
      .exec(http("Search")
        .get("/computers")
        .queryParam("f", "${searchCriterion}") // 4
        .check(regex("""<a href="([^"]+)">${searchComputerName}</a>""").saveAs("url"))) // 5
      .pause(1)
      .exec(http("Select")
        .get("${url}")) // 6
      .pause(1)
  }


Explanations:
  1. First we create a feeder from a csv file with the following columns : *searchCriterion*, *searchComputerName*.
  2. The default feeder strategy is queue, so for this test, we use a random one instead in order to avoid feeder starvation.
  3. Every time a user reaches the feed step, it pops a record from the feeder.
     This user has two new session attributes named *searchCriterion*, *searchComputerName*.
  4. We use session data using Gatling's EL to parameterize the search.
  5. We use a regex with an EL, to capture a part of the HTML response, here an hyperlink, and save it in the user session with the name *computerURL*.
     Note how Scala triple quotes are handy: you don't have to escape double quotes inside the regex with backslashes.
  6. We use the previously save hyperlink to get a specific page.

.. note::
    For more details regarding *Feeders*, please check out :ref:`Feeder reference page <feeder>`.
    
    For more details regarding *HTTP Checks*, please check out :ref:`Checks reference page <http-check>`.

Step 04: Looping
================

In the *browse* process we have a lot of repetition when iterating through the pages.
We have four time the same request with a different query param value. Can we try to DRY this?

First we will extract the repeated ``exec`` block in a function.
Indeed, ``Simulation``\ s are plain Scala classes so we can use all the power of the language if needed::

  object Browse {

    def gotoPage(page: Int) = exec(http("Page " + page)
      .get("/computers?p=" + page)
      .pause(1)

    val browse = gotoPage(0).gotoPage(1).gotoPage(2).gotoPage(3).gotoPage(4)
  }

We can now call this function and pass the desired page number.
But we have still repetition, it's time to introduce a new builtin structure::

  object Browse {

    val browse = repeat(5, "n") { // 1
      exec(http("Page ${n}")
        .get("/computers?p=${n}") // 2
      .pause(1)
    }
  }

Explanations:
  1. The ``repeat`` builtin is a loop resolved at **runtime**.
     It takes the number of repetitions and optionally the name of the counter (that's stored in the user's Session).
  2. As we force the counter name we can use it in Gatling EL and access the nth page.

.. note::
    For more details regarding loops, please check out :ref:`Loops reference page <scenario-loops>`.

Step 05: Check and failure management
=====================================

Until now we used ``check`` to extract some data from the html response and store it in session.
But ``check`` is also handy to check some properties of the http response.
By default Gatling check if the http response status is *20x* or *304*.

To demonstrate failure management we will introduce a ``check`` on a condition that fails randomly::

  import scala.concurrent.forkjoin.ThreadLocalRandom // 1

  val edit = exec(http("Form")
      .get("/computers/new"))
    .pause(1)
    .exec(http("Post")
      .post("/computers")
      ...
      .check(status.is(session => 200 + ThreadLocalRandom.current.nextInt(2)))) // 2

Explanations:
  1. First we import ``ThreadLocalRandom``. This class is just a backport of the JDK7 one for running with JDK6.
  2. We do a check on a condition that's been customized with a lambda.
     It will be evaluated every time a user executes the request and randomly return *200* or *201*.
     As response status is 200, the check will fail randomly.

To handle this random failure we use the ``tryMax`` and ``exitHereIfFailed`` constructs as follow::

  val edit = tryMax(2) { // 1
    exec(...)
  }.exitHereIfFailed // 2

Explanations:
  1. ``tryMax`` tries a given block up to n times.
     Here we try at max twice.
  2. If all tentatives failed, the user exit the whole scenario due to ``exitHereIfFailed``.

.. note::
    For more details regarding conditional blocks, please check out :ref:`Conditional Statements reference page <scenario-conditions>`.

That's all Folks!

.. note::
    The files for this tutorial can be found in the distribution in the ``user-files/simulations`` directory, and on Github `here <https://github.com/excilys/gatling/tree/master/gatling-bundle/src/universal/user-files/simulations>`__.
