.. _quickstart:

##########
Quickstart
##########

Preliminary
===========

In this section we will use Gatling to load test a simple cloud hosted web server and will introduce you to the basic elements of the DSL.

Getting the bundle
------------------

You can get Gatling bundles as a .tar.gz or .zip file `here <https://github.com/excilys/gatling/wiki/Downloads>`__.

Installing
----------

Just unzip the downloaded bundle to a folder of your choice.

.. warning::
    We just ask you **don't use a path containing spaces**, there might be some issues.

    For Windows users, we also recommend that you do not place Gatling in *Programs* folder as there might be permission issues.

In order to run Gatling, you need to have installed a JDK. We recommend you the last version. 

For all details regarding the installation and the tuning of the operating system (OS), please refer to the :ref:`operations <operations>` section.

A Word on Encoding
------------------

Gatling's **default encoding is UTF-8**. If you want to use a different one, you have to:

    * select the proper encoding while using the Recorder
    * configure the proper encoding in the ``gatling.conf`` file.
      It will be used for compiling your simulations and building your requests.
    * make sure your text editor encoding is properly configured to match.

A Word on Scala
---------------

Gatling simulation scripts are written in `Scala <http://www.scala-lang.org/>`_, **but don't panic!** You can use all the basic functions of Gatling without knowing much about Scala.
In most situations, this DSL will cover most of your needs and you'll be able to build your scenarios.

If you are interested in knowing more about Scala, we then recommend you have a look at `Scala School <http://twitter.github.io/scala_school>`_.

.. note::
    Feel also free to join our `Google Group`_ and ask for help.

Test Case
=========

This page will guide you through most of Gatling HTTP features. You'll learn about *simulations*, *scenarios*, *feeders*, *recorder*, *loops*, etc.

Application under Test
----------------------

In this tutorial, we will use an application named *Computer-Database* deployed at the URL: `<http://computer-database.heroku.com>`__

This application is a simple CRUD application for managing computer models, it is one of the samples of `Play! <http://www.playframework.com/>`_.
You can also run it on your local machine: download Play!'s bundle and check out `the samples <https://github.com/playframework/playframework/tree/master/samples/scala/computer-database>`__.

Scenario
--------

To test the performance of this application, we will create scenarios representative of what really happens when users navigate it.

Here is what we think a real user would do with the application:
  #. A user arrives on the application.
  #. The user searches for 'macbook'.
  #. The user opens one of the related model.
  #. The user goes back to home page.
  #. The user iterates through pages.
  #. The user creates a new model.

Basics
======

Using the Recorder
------------------

To ease the creation of the scenario, we will use the *Recorder*, a tool provided with Gatling that allows you to record your actions on a web application and export them as a Gatling scenario.

This tool is launched with a script located in the ``bin`` directory::

  ~$ $GATLING_HOME/bin/recorder.sh/bat

Once launched, you get the following GUI, which lets use configure how requests and response will be recorded.

Set it up with the following options:
  * *computerdatabase* package
  * *BasicSimulation* name
  * *Follow Redirects?* checked
  * *Automatic Referers?* checked
  * *Black list first* filter strategy selected
  * *.\*\\.css*, *.\*\\.js* and *.\*\\.ico* in the black list filters

.. image:: img/recorder.png

After configuring the recorder, all you have to do is to start it and configure your browser to use Gatling Recorder's proxy.

.. note::
  For more information regarding Recorder and browser configuration, please check out :ref:`Recorder reference page <recorder>`.

Recording the scenario
----------------------

All you have to do now is to browse the application:  
  #. Enter 'Search' tag.
  #. Go to the website: http://computer-database.heroku.com
  #. Search for models with 'macbook' in their name.
  #. Select 'Macbook pro'.
  #. Enter 'Browse' tag.
  #. Go back to home page.
  #. Iterates several times through the model pages by clicking on *Next* button.
  #. Enter 'Edit' tag.
  #. Click on *Add new computer*.
  #. Fill the form.
  #. Click on *Create this computer*.    

Try to act as a user, don't jump from one page to another without taking the time to read.
This will make your scenario closer to real users' behavior.

When you have finished playing the scenario, click on Stop in the Recorder interface

The Simulation will be generated in the folder ``user-files/simulations/computerdatabase`` of your Gatling installation under the name *BasicSimulation.scala*.

Gatling scenario explained
--------------------------

Here is the produced output:
::

  package computerdatabase // (1)

  import io.gatling.core.Predef._ // (2)
  import io.gatling.http.Predef._
  import scala.concurrent.duration._

  class BasicSimulation extends Simulation { // (3)

    val httpConf = http // (4)
      .baseURL("http://computer-database.heroku.com") // (5)
      .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // (6)
      .doNotTrackHeader("1")
      .acceptLanguageHeader("en-US,en;q=0.5")
      .acceptEncodingHeader("gzip, deflate")
      .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:27.0) Gecko/20100101 Firefox/27.0")

    val scn = scenario("BasicSimulation") // (7)
      .exec(http("request_1")  // (8)
        .get("/")) // (9)
      .pause(5) // 10
      ...

    setUp( // (11)
      scn.inject(atOnceUsers(1) // (12)
    ).protocols(httpConf) // (13)
  }


What does it mean?

1. The optional package.
2. The required imports.
3. The class declaration. Note that it extends ``Simulation``.
4. The common configuration to all HTTP requests.

.. note::
    ``val`` is the keyword for defining a non-re-assignable value.
    Types are not defined and are inferred by the Scala compiler.

5. The baseURL that will be prepended to all relative urls.
6. Common HTTP headers that will be sent with all the requests.
7. The scenario definition.
8. A HTTP request, named *request_1*. This name will be displayed in the final reports.
9. The url this request targets with the *GET* method.
10. Some pause/think time.

.. note::
    Duration unit defaults to ``seconds``, e.g. ``pause(5)`` is equivalent to ``pause(5 seconds)``.

11. Where one set ups the scenarios that will be launched in this Simulation.
12. Declaring to inject into scenario named *scn* one single user.
13. Attaching the HTTP configuration declared above.

.. note::
    For more details regarding Simulation structure, please check out :ref:`Simulation reference page <simulation-structure>`.

Running Gatling
---------------

Launch the second script located in the ``bin`` directory::

  ~$ $GATLING_HOME/bin/gatling.sh/bat


You should see a menu with the simulation examples::

  Choose a simulation number:
     [0] computerdatabase.BasicSimulation


When the simulation is done, the console will display a link to the HTML reports.

.. note::
    If Gatling doesn't work as expected, see our :ref:`FAQ <faq>` or ask on our `Google Group`_.

Going further
=============

Now we have a basic Simulation to work with, we will apply a suite of refactoring to introduce more advanced concepts and DSL constructs.

Step 01: Isolate processes
--------------------------

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
--------------------------------

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
-------------------------------------------------

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

    val feeder = csv("search.csv").random // (1) (2)

    val search = exec(http("Home")
      .get("/"))
      .pause(1)
      .feed(feeder) // (3)
      .exec(http("Search")
        .get("/computers")
        .queryParam("f", "${searchCriterion}") // (4)
        .check(regex("""<a href="([^"]+)">${searchComputerName}</a>""").saveAs("url"))) // (5)
      .pause(1)
      .exec(http("Select")
        .get("${url}")) // (6)
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
----------------

In the *browse* process we have a lot of repetition when iterating through the pages.
We have four time the same request with a different query param value. Can we try to DRY this?

First we will extract the repeated ``exec`` block in a function.
Indeed, ``Simulation``\ s are plain Scala classes so we can use all the power of the language if needed::

  object Browse {

    def gotoPage(page: String) = exec(http("Page " + page)
      .get("/computers?p=" + page)
      .pause(1)

    val browse = ???
  }

We can now call this function and pass the desired page number.
But we have still repetition, it's time to introduce a new builtin structure::

  object Browse {

    def gotoPage(page: String) = exec(http("Page " + page)
      .get("/computers?p=" + page)
      .pause(1)

    val browse = repeat(5, "i") { // (1)
      gotoPage("${i}") // (2)
    }
  }

Explanations:
  1. The ``repeat`` builtin is a loop resolved at **runtime**.
     It takes the number of repetitions and optionally the name of the counter (that's stored in the user's Session).
  2. As we force the counter name we can use it in Gatling EL and access the nth page.

.. note::
    For more details regarding loops, please check out :ref:`Loops reference page <scenario-loops>`.

Step 05: Check and failure management
-------------------------------------

Until now we used ``check`` to extract some data from the html response and store it in session.
But ``check`` is also handy to check some properties of the http response.
By default Gatling check if the http response status is *20x* or *304*.

To demonstrate failure management we will introduce a ``check`` on a condition that fails randomly::

  import scala.concurrent.forkjoin.ThreadLocalRandom // (1)

  val edit = exec(http("Form")
      .get("/computers/new"))
    .pause(1)
    .exec(http("Post")
      .post("/computers")
      ...
      .check(status.is(session => 200 + ThreadLocalRandom.current.nextInt(2)))) // (2)

Explanations:
  1. First we import ``ThreadLocalRandom``. This class is just a backport of the JDK7 one for running with JDK6.
  2. We do a check on a condition that's been customized with a lambda.
     It will be evaluated every time a user executes the request and randomly return *200* or *201*.
     As response status is 200, the check will fail randomly.

To handle this random failure we use the ``tryMax`` and ``exitHereIfFailed`` constructs as follow::

  val edit = tryMax(2) { // (1)
    exec(...)
  }.exitHereIfFailed // (2)

Explanations:
  1. ``tryMax`` tries a given block up to n times.
     Here we try at max twice.
  2. If all tentatives failed, the user exit the whole scenario due to ``exitHereIfFailed``.

.. note::
    For more details regarding conditional blocks, please check out :ref:`Conditional Statements reference page <scenario-conditions>`.

That's all Folks!

.. _Google Group: https://groups.google.com/forum/#!forum/gatling
