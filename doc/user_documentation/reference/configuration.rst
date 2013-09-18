#############
Configuration
#############

This page explains how Gatling can be configured

Introduction
============

Gatling can be configured and optimized in three ways:

* By the configuration files
* By command line options
* By ``$JAVA_OPTS`` environment variable

Configuration files
===================

logback.xml
-----------

This file allows you to configure the log level of Gatling. For further information, you should have a look at `Logback Documentation <http://logback.qos.ch/manual/index.html>`_.

.. note:: In order to log requests and responses, uncomment the dedicated logger in the `default configuration file <https://github.com/excilys/gatling/blob/1.5.X/gatling-bundle/src/main/assembly/assembly-structure/conf/logback.xml>`_.

gatling.conf
------------

This file allows you to set configurable values for the Engine and the modules of Gatling; each value is described in the `default configuration file <https://github.com/excilys/gatling/blob/1.5.X/gatling-bundle/src/main/assembly/assembly-structure/conf/gatling.conf>`_.

If you don't specify an option in the configuration file, it will fall back to a default value. These values are also shown in the `default configuration file <https://github.com/excilys/gatling/blob/1.5.X/gatling-bundle/src/main/assembly/assembly-structure/conf/gatling.conf>`_.

.. _gatling-cli-options:

Command Line Options
====================

Gatling can be started with several options listed below:

* **-nr** (**--no-reports**): Runs simulation but does not generate reports
* **-ro <folderName>** (**--reports-only <folderName>**): Generates the reports for the simulation log file located in <gatling_home>/results/<folderName>
* **-df <folderAbsolutePath>** (**--data-folder <folderAbsolutePath>**): Uses <folderAbsolutePath> as the folder where feeders are stored
* **-rf <folderAbsolutePath>** (**--results-folder <folderAbsolutePath>**): Uses <folderAbsolutePath> as the folder where results are stored
* **-bf <folderAbsolutePath>** (**--request-bodies-folder <folderAbsolutePath>**): Uses <folderAbsolutePath> as the folder where request bodies are stored
* **-sf <folderAbsolutePath>** (**--simulations-folder <folderAbsolutePath>**): Uses <folderAbsolutePath> as the folder where simulations are stored
* **-sbf <folderAbsolutePath>** (**--simulations-binaries-folder <folderAbsolutePath>**): Uses <folderAbsolutePath> as the folder where simulation binaries are stored
* **-s <className>** (**--simulation <className>**): Uses <className> as the name of the simulation to be run


$JAVA_OPTS
==========

Default JAVA_OPTS are set in Gatling's launch scripts, if you want to override them, you'll have to edit these files and replace whatever value you'd like to change in JAVA_OPTS.

If you want to set additional JAVA_OPTS to Gatling, you can do so by defining the JAVA_OPTS before the gatling command::

	~$ JAVA_OPTS="myAdditionalOption" bin/gatling.sh
