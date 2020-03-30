.. _maven-archetype:

###############
Maven archetype
###############

Gatling's Maven Archetype helps you bootstrap a maven project with Gatling dependencies and plugin.

Versions
========

Check out available versions on `Maven Central <https://search.maven.org/search?q=g:io.gatling.highcharts%20AND%20a:gatling-highcharts-maven-archetype&core=gav>`__.

Beware that milestones (M versions) are not documented for OSS users and are only released for `FrontLine <https://gatling.io/gatling-frontline/>`_ customers.

Usage
=====

The Maven's coordinates for the archetype are ``io.gatling.highcharts:gatling-highcharts-maven-archetype``.

You can either use your IDE's facilities for creating a new project using a Maven archetype or, from the command line, type::

  mvn archetype:generate -DarchetypeGroupId=io.gatling.highcharts -DarchetypeArtifactId=gatling-highcharts-maven-archetype

Select the ``groupId``, ``artifactId`` and package name for your classes before confirming the archetype creation.

Using the generated project
===========================

After importing the project, its structure should look like that:

.. image:: img/archetype_structure.png
  :alt: Archetype Structure

The archetype structure closely follows the bundle's structure :

* Your simulations will live under ``src/test/scala``
* Your resources such as feeder files and request body templates will live under ``src/test/resources``

.. _launchers:

Launchers
---------

You can right click on the ``Engine`` class in your IDE and launch the Gatling load test engine.
Simulation reports will be written in the ``target/gatling`` directory.

You can right click on the ``Recorder`` class in your IDEand launch the Recorder.
Simulations will be generated in the ``src/test/scala`` directory.

Sources
-------

If you're interested in contributing, you can find the `gatling-highcharts-maven-archetype sources <https://github.com/gatling/gatling-highcharts-maven-archetype>`_ on Github.
