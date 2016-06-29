.. _quickstart:

##########
Quickstart
##########

Introduction
============

In this section we will use Gatling to load test a simple cloud hosted web server and will introduce you to the basic elements of the DSL.

Getting the bundle
------------------

You can get Gatling bundles as a .zip file `here </#/resources/download>`__.

Installing
----------

Just unzip the downloaded bundle to a folder of your choice.

.. warning::
  Windows users : we recommend that you do not place Gatling in the *Programs* folder as there may be permission issues.

In order to run Gatling, you need to have a JDK installed.
Gatling requires **JDK8**, yet we recommend that you use an up-to-date version.

For all details regarding the installation and the tuning of the operating system (OS), please refer to the :ref:`operations` section.

.. warning::
  Gatling launch scripts and Gatling maven plugin honor ``JAVA_HOME`` env var if it's set.
  OS, like OSX, have their own tricky way of discovering which version of Java to run,
  so you might end up running a different version than the one ``java -version`` tells you.
  If you get strange errors such as ``Unsupported major.minor version 51.0`` and you were expecting to run a JDK8,
  you might want to explicitly set ``JAVA_HOME``.

A Word on Encoding
------------------

Gatling's **default encoding is UTF-8**. If you want to use a different one, you have to:

* select the proper encoding while using the Recorder
* configure the proper encoding in the ``gatling.conf`` file.
    It will be used for compiling your simulations, building your requests and your responses.
* make sure your text editor encoding is properly configured to match.

A Word on Scala
---------------

Gatling simulation scripts are written in `Scala <http://www.scala-lang.org/>`_, **but don't panic!** You can use all the basic functions of Gatling without knowing much about Scala.
In most situations the DSL will cover most of your needs and you'll be able to build your scenarios.

If you are interested in knowing more about Scala, then we recommend that you have a look at `Twitter's Scala School <http://twitter.github.io/scala_school>`_.

.. note::
  Feel also free to join our `Google Group`_ and ask for help **once you've read this documentation**.

Test Case
=========

This page will guide you through most of Gatling HTTP features. You'll learn about *simulations*, *scenarios*, *feeders*, *recorder*, *loops*, etc.

Application under Test
----------------------

In this tutorial, we will use an application named *Computer-Database* deployed at the URL: `<http://computer-database.gatling.io>`__.

This application is a simple CRUD application for managing computer models, and was a sample for the `Play Framework <http://www.playframework.com/>`_ before version 2.3.

You can also run it on your local machine: download Play! 2.2.x's bundle and check out `the app <https://github.com/gatling/gatling-computer-database>`__.

Scenario
--------

To test the performance of this application, we will create scenarios representative of what really happens when users navigate it.

Here is what we think a real user would do with the application:
  #. A user arrives at the application.
  #. The user searches for 'macbook'.
  #. The user opens one of the related models.
  #. The user goes back to home page.
  #. The user iterates through pages.
  #. The user creates a new model.

Basics
======

Using the Recorder
------------------

To ease the creation of the scenario, we will use the *Recorder*, a tool provided with Gatling that allows you to record your actions on a web application and export them as a Gatling scenario.

This tool is launched with a script located in the *bin* directory:

* On Linux/Unix::

  $GATLING_HOME/bin/recorder.sh

* On Windows::

  %GATLING_HOME%\bin\recorder.bat

Once launched, the following GUI lets you configure how requests and responses will be recorded.

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

Now simply browse the application:
  #. Enter 'Search' tag.
  #. Go to the website: http://computer-database.gatling.io
  #. Search for models with 'macbook' in their name.
  #. Select 'Macbook pro'.
  #. Enter 'Browse' tag.
  #. Go back to home page.
  #. Iterates several times through the model pages by clicking on *Next* button.
  #. Enter 'Edit' tag.
  #. Click on *Add new computer*.
  #. Fill the form.
  #. Click on *Create this computer*.

Try to act as a real user would, don't immediately jump from one page to another without taking the time to read.
This will make your scenario closer to real users' behavior.

When you have finished playing the scenario, click on ``Stop`` in the Recorder interface.

The Simulation will be generated in the folder ``user-files/simulations/computerdatabase`` of your Gatling installation under the name ``BasicSimulation.scala``.

Gatling scenario explained
--------------------------

Here is the produced output:

.. includecode:: code/QuickStart.scala#quickstart-recorder-output

What does it mean?

1. The optional package.
2. The required imports.
3. The class declaration. Note that it extends ``Simulation``.
4. The common configuration to all HTTP requests.

.. note::
  ``val`` is the keyword for defining a constant value.
  Types are not defined and are inferred by the Scala compiler.

5. The baseURL that will be prepended to all relative urls.
6. Common HTTP headers that will be sent with all the requests.
7. The scenario definition.
8. A HTTP request, named *request_1*. This name will be displayed in the final reports.
9. The url this request targets with the *GET* method.
10. Some pause/think time.

.. note::
  Duration units default to ``seconds``, e.g. ``pause(5)`` is equivalent to ``pause(5 seconds)``.

11. Where one sets up the scenarios that will be launched in this Simulation.
12. Declaring to inject into scenario named *scn* one single user.
13. Attaching the HTTP configuration declared above.

.. note::
  For more details regarding Simulation structure, please check out :ref:`Simulation reference page <simulation-structure>`.

Running Gatling
---------------

Launch the second script located in the *bin* directory:

* On Linux/Unix::

  $GATLING_HOME/bin/gatling.sh

* On Windows::

  %GATLING_HOME%\bin\gatling.bat

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
