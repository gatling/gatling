.. _getting-started:

###############
Getting Started
###############

In this quick start guide, you'll follow the minimal steps to have Gatling up and running in no time.

Getting Gatling
===============

You can get Gatling **bundles** as a ``.tar.gz`` or ``.zip`` file `here <https://github.com/excilys/gatling/wiki/Downloads>`_.

Requirements
============

Gatling 1 is compiled with JDK6, while Gatling 2 is compiled with JDK7, yet into JDK6 bytecode. In any case, Gatling 1 can't be built with JDK7 as some Swing classes have been parameterized since.

The launch scripts set up JVM options that may only be available since latest JDK6 versions.
If you use an old JDK6, please consider upgrading, or edit the scripts for removing the unsupported options.
If you use an JDK < 6, you have to upgrade.

Yet, we recommend that you use the latest JDK7. NIO is based on native code, so it depends on JVM implementation and bugs are frequently fixed. For example, NIO have been broken on Oracle JDK7 until 1.7.0_10. Gatling is mostly tested on Oracle JDK7, OS X and Linux.

Installing Gatling
==================

Just unzip the downloaded bundle to a folder of your choice.

.. note::

	* **X-platform users**: don't use a path with spaces in it.

	* **Windows users**: we recommend that you do not put Gatling in *Programs* folder as there might be permission issues.

Configure your OS
=================

You might first want to have a look at how to :ref:`tune Gatling and your OS <http-tuning>` according to your use case.

A word on encoding
==================

Gatling uses by default **UTF-8**. If you want to use a different one, you have to:

* select the proper encoding in the Recorder
* configure the proper encoding in the ``gatling.conf`` file. This is the one that will be used for compiling your simulations and building your requests.
* make sure your text editor is properly configured and doesn't change the original encoding.

Running Gatling
===============

Gatling offers a command line interface (CLI) that can be run using the following command::

	~$ $GATLING_HOME/bin/gatling.sh

.. note:: **Windows users**: you can double click on the gatling.bat file located in GATLING_HOME/bin

Once executed, you should see a menu with the simulation examples::

	Collecting simulations...
	Choose a simulation number:
	  [0] advanced.AdvancedExampleSimulation
	  [1] basic.BasicExampleSimulation

To run a simulation, simply type the number of the simulation you want to run, choose a name for the folder where the results will be generated, and a description for the run.

And... voila!

.. note:: If Gatling does not work as expected, see our :ref:`faq`.

Debugging
=========

Gatling can log requests and responses. See config file ``conf/logback.xml``.

Going further
=============

This is how Gatling works, now you have to write your own simulations. We provide you with several resources to learn how to do it:

* The sample simulation scripts in the ``user-files`` folder will give you a sneak peek of what Gatling scripts look like
* The :ref:`first-steps-with-gatling` explains how to write a simulation and run it
* The :ref:`advanced-usage` page goes deeper and describes advanced features of Gatling
* The :ref:`reference` articles explain Gatling components in details