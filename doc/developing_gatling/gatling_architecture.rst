####################
Gatling Architecture
####################

Process Overview
================

Gatling's process is divided in three parts: 

1. Simulation Compilation
2. Run Simulation
3. Generate Reports

Next picture illustrates the global process and the tasks done during each part (Open in new windows if you cannot read):

.. image:: img/gatling-process-overview.png
	:alt: Gatling Process Overview
	:scale: 70

Projects and Modules
====================

The complete Gatling suite is made of 3 Maven projects and 9 Maven modules as illustrated below.

.. image:: img/maven_structure.png
	:alt: Gatling Maven Structure

There are three projects because of licensing issues as explained :ref:`here <license>`:

* Gatling is under Apache License
* Gatling VTD is under the GPL
* Gatling Highcharts is **not** Open Source

Therefore, you can only contribute code to **Gatling** and **Gatling VTD**. Concerning Gatling Highcharts, you can still `file issues <https://github.com/excilys/gatling/issues>`_ if you find any bug.

Application Programming Interfaces (APIs)
=========================================

Gatling APIs allows developers to add components or even plugins to Gatling easily; they are shown below.

.. image:: img/gatling-api.png
	:alt: Gatling APIs

As you can see, there are no more than 7 APIs, some are more important than others of course. The 4 most notable APIs of Gatling are the Charting-API, the DataWriter-API, the Feeder API and the Check API as they allow you to create plugins for Gatling. The 3 others offer functionalities that you can use in your plugins.

Charting API
------------

This is the API against which `Gatling Highcharts <https://github.com/excilys/gatling-highcharts>`_ is built; it allows one to build the chart reports with any charting library.

Checks API
----------

This is the API against which `Gatling VTD <https://github.com/excilys/gatling-vtd>`_ is built; it allows one to add check methods in Gatling.

Feeder API
----------

This API allows one to add sources and strategies for the Feeders. The only existing examples are in `gatling-core <https://github.com/excilys/gatling/tree/1.5.X/gatling-core/src/main/scala/com/excilys/ebi/gatling/core/feeder>`_ for the moment.

DataWriter API
--------------

This API allows one to push reporting data to the subsequent writers.

Other APIs
----------

The other APIs are small helpers to help you use some facilities of Gatling without having to code them. You are strongly advised to use as much Gatling's APIs when developing plugins. It will give some consistency to the Gatling Suite ecosystem.

Since version 1.0, there are three utility APIs:

* The Config API, used to manage the configuration of your plugin;
* The Logging API, used to manage the logs of your plugin;
* The Resources API, used to manage part of the life cycle of the closeable resources of your plugin.