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

This tool is launched with a script located in the *bin* directory::

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

The Simulation will be generated in the folder *user-files/simulations/computerdatabase* of your Gatling installation under the name *BasicSimulation.scala*.

Gatling scenario explained
--------------------------

Here is the produced output:
::

  package computerdatabase // 1

  import io.gatling.core.Predef._ // 2
  import io.gatling.http.Predef._
  import scala.concurrent.duration._

  class BasicSimulation extends Simulation { // 3

    val httpConf = http // 4
      .baseURL("http://computer-database.heroku.com") // 5
      .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // 6
      .doNotTrackHeader("1")
      .acceptLanguageHeader("en-US,en;q=0.5")
      .acceptEncodingHeader("gzip, deflate")
      .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:27.0) Gecko/20100101 Firefox/27.0")

    val scn = scenario("BasicSimulation") // 7
      .exec(http("request_1")  // 8
        .get("/")) // 9
      .pause(5) // 10

    setUp( // 11
      scn.inject(atOnceUsers(1) // 12
    ).protocols(httpConf) // 13
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

Launch the second script located in the *bin* directory::

  ~$ $GATLING_HOME/bin/gatling.sh/bat


You should see a menu with the simulation examples::

  Choose a simulation number:
     [0] computerdatabase.BasicSimulation


When the simulation is done, the console will display a link to the HTML reports.

.. note::
    If Gatling doesn't work as expected, see our :ref:`FAQ <faq>` or ask on our `Google Group`_.

Going Further
-------------

When you're ready to go further, please check out the :ref:`Advanced Tutorial <advanced_tutorial>`.


.. _Google Group: https://groups.google.com/forum/#!forum/gatling