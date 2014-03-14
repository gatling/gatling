.. _quickstart:

##########
Quickstart
##########

In this section we will use Gatling to load test a simple cloud hosted web server and will introduce you to the basic elements of the DSL.

Getting Gatling
===============

You can get Gatling bundles as a .tar.gz or .zip file `here <https://github.com/excilys/gatling/wiki/Downloads>`_.

Requirements
============

Gatling 2 is compiled with JDK7, yet into JDK6 bytecode.

Yet, we recommend that you use the **latest JDK7**. NIO is based on native code, so it depends on JVM implementation and bugs are frequently fixed. For example, NIO have been broken on Oracle JDK7 until 1.7.0_10. Gatling is mostly tested on Oracle JDK7, OS X and Linux.

Installing Gatling
==================

Just unzip the downloaded bundle to a folder of your choice.

We just ask you **don't use a path with spaces** in it, there might be some issues.

For Windows users, we also recommend that you do not place Gatling in *Programs* folder as there might be permission issues.


Configure your OS
=================

You might first want to have a look at how to **tune Gatling and your OS** according to your use case.

A word on encoding
==================

Gatling uses by **default UTF-8**. If you want to use a different one, you have to:

  * select the proper encoding in the Recorder
  * configure the proper encoding in the gatling.conf file. This is the one that will be used for compiling your simulations and building your requests.
  * make sure your text editor is properly configured and doesn't change the original encoding.

Running Gatling
===============

Gatling offers a command line interface (CLI) that can be run using the following command::

  ~$ $GATLING_HOME/bin/gatling.sh

Windows users:
    you can double click on the gatling.bat file located in GATLING_HOME/bin

Once launched, you should see a menu with the simulation examples::

  Choose a simulation number:
     [0] computerdatabase.Simulation

To run a simulation, simply type the number of the simulation you want to run, choose a name for the folder where the results will be generated, and a description for the run.

And... voila!

  Note: If Gatling does not work as expected, see our FAQ.

Your first simulation
======================

Now, you're ready to go!

This page will guide you through most of Gatling HTTP features.

You'll learn about simulations, scenarios, feeders, recorder, loops, scala functions, etc.

You're going to see some Scala, but don't panic!
================================================

Yes, Gatling simulation scripts are Scala classes.

Don't worry, Gatling doesn't expect you to be a hardcore Scala hacker.

Just please read this manual properly and learn the DSL.
In most situations, this DSL will cover most of your needs and you'll be able to build your scenarios without much `Scala <http://www.scala-lang.org/>`_ knowledge.

However, you might also sometimes run into situations where you have to hack a bit.
We then recommend you have a look at `Scala School <http://twitter.github.io/scala_school>`_.

Feel also free to join our `Google Group <https://groups.google.com/forum/#!forum/gatling>`_ and ask for help.

For the non Scala people, here's what a Gatling simulation class look like::

  package foo.bar (1)

  import io.gatling.core.Predef._ (2)
  import io.gatling.http.Predef._
  import scala.concurrent.duration._

  class MySimulation extends Simulation { (3)

    // your code starts here
    val scn = scenario("My scenario")
            .exec(http("My Page")
              .get("http://mywebsite.com/page.html")) (4)

    setUp(scn.inject(atOnceUsers(10)) (5)
    // your code ends here
  }

Let's explain :

  1. The optional package.
  2. The required imports.
  3. The class declaration. Note that your simulation extends ``Simulation``.
  4. Your scenario definition. ``val`` is the keyword for defining a non-re-assignable value.
  5. The set up, where you configure the scenarios you want to run and the injection profile.

The application under test
==========================

In this tutorial, you'll be playing with an application named 'computer-database' deployed on Heroku at the following url:

http://computer-database.heroku.com

This application is one of the samples of `Play! <http://www.playframework.com/>`_'.
You can also run it all your local machine: just download Play!'s bundle and check out the samples.

This is a simple CRUD application for managing computer models. The main features available are:

  * Creating / Editing / Listing computer models
  * Searching / Sorting / Paginating computer models

Planning the test
=================

To test the performance of this application, we'd like to create scenarios representative of what really happens when users navigate it.
So we tried to imagine what a real user would do with our application, shrank it and we got the following:

  * The user opens the application.
  * The user searches for 'macbook'.
  * The user opens one of the related model.
  * The user goes back to home page.
  * The user iterates through pages.
  * The user creates a new model.

Now that we have decided what would be the common use of our application, we can create the scenario for Gatling.

Gatling Recorder
================

To ease the creation of scenarios, we will use the Recorder, a tool provided with Gatling that allows you to record your actions on a web application and export them as Gatling scenarios.

This tool is launched with a script located in the bin directory along the gatling one::

  ~$ $GATLING_HOME/bin/recorder.sh

Configuration
-------------

Once launched, you get the following GUI, which lets use configure how requests and response will be recorded:

.. image:: img/recorder.png

Set up Gatling Recorder with the following options:

  * ``computerdatabase`` package
  * ``BasicSimulation`` name
  * ``Follow Redirects?`` checked.
  * ``Automatic Referers`` checked
  * ``Black list first`` filter strategy selected
  * ``.*\.css``, ``.*\.js`` and ``.*\.ico`` filters.

After configuring the recorder, all you have to do is to start it and configure your browser to use Gatling Recorder's proxy.

  For information about how to configure your browser, you can check out the Recorder's documentation.

Recording the scenario
----------------------

All you have to do now is to browse the application:

  1. Enter 'Search' tag
  2. Go to the website: http://computer-database.heroku.com/
  3. Search for models with 'macbook' in their name.
  4. Select 'Macbook pro'.
  5. Enter 'Browse' tag
  6. Go back to home page.
  7. Iterates several times through the model pages by clicking on ``Next`` button.
  8. Enter 'Edit' tag
  9. Create a new computer model:

    * Click on ``Add new computer``.
    * Fill the form.
    * Click on ``Create this computer``

  Try to act as a user, don't jump from one page to another without taking the time to read.
  This will make your scenario closer to real user behavior.

When you have finished to play the scenario, you can click on Stop, and your first Gatling scenario will be created by the recorder.

The Gatling scenario corresponding to our example is available in the folder ``user-files/simulations/computerdatabase`` of your Gatling installation under the name ``BasicSimulation.scala``.

Gatling scenario explained
==========================

So now you've got a file with some mysterious dialect.
Nice! but... what does this mean? Don't worry, we are going to decode these bizarre words for you.

This file is a real Scala class containing 4 different parts:

  * The HTTP protocol configuration: a placeholder for common parameters for all the HTTP requests
  * The headers definition: 
  * The scenario definition
  * The setup definition

For more details see `here <general/simulation_structure.html>`_.

Go further with Gatling
=======================

Now we have a basic Simulation to work with, we will apply a suite of refactoring to introduce more advanced concepts and DSL constructs.

The resulting simulations are available in the folder ``user-files/simulations/computerdatabase/advanced/``.

Step 01: Bring order into this mess
-----------------------------------

Presently our Simulation is a bit messy, we have a big scenario without real business meaning.
So first let split it in composable business processes, like one would do with PageObject pattern with Selenium.
This will ease the writing of various scenarios by user population.

In our scenario we have three separated processes:

  * Search: search models by name
  * Browse: browse the list of models
  * Edit: edit a given model

So we will create three Scala objects, objects are native Scala singletons, to encapsulate these processes::

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

Step 02: More users = more load!
--------------------------------

So, this is great, we can load test our server with... one user!
We are going to increase the number of users.

Let define two populations of users:

  * The regular users: they can search and browse computer models.
  * The admin users: they can search, browse and edit computer models.

Translating into scenario this gives::

  val users = scenario("Users").exec(Search.search, Browse.browse)
  val admins = scenario("Admins").exec(Search.search, Browse.browse, Edit.edit)

To increase the number of simulated users, all you have to do is to change the configuration of the simulation as follows::

  setUp(users.inject(atOnceUsers(10)).protocols(httpConf))

  Note: Here we set only 10 users, because we don't want to flood our test web application, please be kind and don't crash our Heroku instance ;-)

If you want to simulate 3 000 users, you don't want them to start at the same time.
Indeed, they are more likely to connect to your web application gradually.

Gatling provides the ``rampUsers`` builtin to implement this behavior.
The value of the ramp indicates the duration over which the users will be linearly started.

In our scenario let's have 10 regular users and 2 admins, and ramp them on 10 sec so we don't hammer the server::

  setUp(
    users.inject(rampUsers(10) over (10 seconds)),
    admins.inject(rampUsers(2) over (10 seconds))
  ).protocols(httpConf)

Step 03: Dynamic values with Feeders
------------------------------------

We have set our simulation to run a bunch of users, but they all search for the same model.
Wouldn't it be nice if every user could search a different model name?

We need dynamic data so that all users don't play the same and we end up with a behavior completely different from the live system (caching, JIT...).
This is where Feeders will be useful.

Feeders are data sources containing all the values you want to use in your scenarios.
There are several types of Feeders, the simpliest being the CSV Feeder: this is the one we will use in our test.
Feeders are explained in details in the Feeders reference.

Here are the feeder we use and the modifications we made to our scenario::

  object Search {

    val feeder = csv("search.csv").random (1) (2)

    val search = exec(http("Home")
      .get("/"))
      .pause(1)
      .feed(feeder) (3)
      .exec(http("Search")
        .get("/computers")
        .queryParam("""f""", "${searchCriterion}") (4)
        .check(regex("""<a href="([^"]+)">${searchComputerName}</a>""").saveAs("computerURL"))) (5)
      .pause(1)
      .exec(http("Select")
        .get("${computerURL}") (6)
        .check(status.is(200)))
      .pause(1)
  }

Let's explain :

  1. First we create a feeder from a csv file with the following columns : ``searchCriterion``, ``searchComputerName``.
  2. The default feeder is a queue, so for this test, we use a random one to avoid feeder starvation.
  3. Every time a user passes here, a record is popped from the feeder and injected into the user's session.
     Thus user has two new session data named ``searchCriterion``, ``searchComputerName``.
  4. We use session data using Gatling's EL to parametrized the search.
  5. We use a regex with an EL, to capture a part of the HTML response, here an hyperlink, and save it in the user session with the name ``computerURL``.
  6. We use the previously save hyperlink to get a specific page.

Step 04: Don't repeat yourself!
-------------------------------

In the ``browse`` process we have a lot of repetition when iterating through the pages.
We have four time the same request with a different query param value. Can we try to DRY this ?

First we will extract the repeated ``exec`` block in a function, yes ``Simulation`` are plain Scala so we can use all the power of the language if needed::

  def gotoPage(page: String) = exec(http("Page " + page)
    .get("/computers")
    .queryParam("""p""", page))
    .pause(1)

We can now call this function and pass the desired page number.
But we have still repetition, it's time to introduce a new builtin structure::

  def gotoUntil(max: String) = repeat(max.toInt, "i") { (1)
    gotoPage("${i}") (2)
  }

Let's explained:

  1. The ``repeat`` builtin is a loop resolved at RUNTIME, it take the number of repetition and optionally the name of the counter.
  2. As we force the counter name we can use it in Gatling EL and access the nth page.

And finally we can write the ``browse`` process as follow::

  val browse = gotoUntil("4")

Step 05: Check and failure management
-------------------------------------

Until now we use ``check`` to extract some data from the html response and store it in session.
But ``check`` are also handy to check some properties of the http response.
By default Gatling check if the http response status is 200x.

To demonstrate the failure management we will introduce a ``check`` on a condition that fails randomly::

  val random = ThreadLocalRandom.current() (1)
  val edit = exec(http("Form")
      .get("/computers/new"))
    .pause(1)
    .exec(http("Post")
      .post("/computers")
      ...
      .check(status.is(session => 200 + random.nextInt(2)))) (2)

Let's explained:

  1. First we create a thread local random number generator to avoid contention.
  2. We do a check on a condition that's been customized with a lambda.
     It will be evaluated every time a user executes the request.
     As response status is 200 the check will fail randomly.

To handle this random failure we use the ``tryMax`` and ``exitHereIfFailed`` constructs as follow::

  val edit = tryMax(2) { (1)
    exec(...)
  }.exitHereIfFailed (2)

Let's explained:

  1. ``tryMax`` allow to try a fix number of time an ``exec`` block in case of failure.
     Here we try at max 2 times the block.
  2. If the chain didn't finally succeed, the user exit the whole scenario due to ``exitHereIfFailed``.

That's all Folks!




