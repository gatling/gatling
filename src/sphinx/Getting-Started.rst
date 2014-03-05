***************
Getting Started
***************

In this quick start guide, you'll follow the minimal steps to have Gatling up and running in no time.


Getting Gatling
###############

You can get Gatling bundles as a .tar.gz or .zip file `here <https://github.com/excilys/gatling/wiki/Downloads>`_.

Requirements
############

Gatling 2 is compiled with JDK7, yet into JDK6 bytecode.

Yet, we recommend that you use the latest JDK7. NIO is based on native code, so it depends on JVM implementation and bugs are frequently fixed. For example, NIO have been broken on Oracle JDK7 until 1.7.0_10. Gatling is mostly tested on Oracle JDK7, OS X and Linux.

Installing Gatling
##################

Just unzip the downloaded bundle to a folder of your choice.

    X-platform users:
        Don't use a path with spaces in it.

    Windows users:
        We recommend that you do not put Gatling in Programs folder as there might be permission issues.


Configure your OS
#################

You might first want to have a look at how to tune Gatling and your OS according to your use case.

A word on encoding
##################

Gatling uses by default UTF-8. If you want to use a different one, you have to:

  * Select the proper encoding in the Recorder
  * Configure the proper encoding in the gatling.conf file. This is the one that will be used for compiling your simulations and building your requests.
  * Make sure your text editor is properly configured and doesn't change the original encoding.

Running Gatling
###############

Gatling offers a command line interface (CLI) that can be run using the following command::

> ~$ $GATLING_HOME/bin/gatling.sh

  Windows users:
    you can double click on the gatling.bat file located in GATLING_HOME/bin

Once executed, you should see a menu with the simulation examples::

  Choose a simulation number:
     [0] computerdatabase.Simulation
  Invalid characters, please provide a correct simulation number:

To run a simulation, simply type the number of the simulation you want to run, choose a name for the folder where the results will be generated, and a description for the run.

And... voila!

  Note: If Gatling does not work as expected, see our FAQ.

Debugging
#########

Gatling can log requests and responses. See config file ``conf/logback.xml``.

Going further
#############

This is how Gatling works, now you have to write your own simulations. We provide you with several resources to learn how to do it:

  * The sample simulation script in the user-files folder will give you a sneak peek of what Gatling scripts look like
  * The Gatling tutorial explains how to write a simulation and run it
  * The Reference articles explain Gatling components in details



