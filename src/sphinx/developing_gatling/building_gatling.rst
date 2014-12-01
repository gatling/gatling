################
Building Gatling
################

.. note:: We use `Travis CI <https://travis-ci.org/excilys/gatling>`_ for continuous integration. You can find the latest snapshot on `Sonatype <https://oss.sonatype.org/content/repositories/snapshots/io/gatling/highcharts/gatling-charts-highcharts/2.0.0-SNAPSHOT/>`_

Introduction
============

As stated in the :ref:`Licenses section <license>`, Gatling is divided into two sub-projects :

* Gatling, the core engine
* Gatling Highcharts, the charting engine

In order to fully build Gatling, you'll need to build, in the following order, Gatling then Gatling Highcharts.

Getting the source
==================


Gatling is hosted on `Github <http://github.com/gatling/>`_.

You'll need to `install Git <http://git-scm.com/downloads>`__ on your machine first.

You can then clone Gatling from ``http://github.com/gatling/gatling``, and Gatling Highcharts from ``http://github.com/gatling/gatling-highcharts``::

  git clone http://github.com/gatling/gatling
  git clone http://github.com/gatling/gatling-highcharts


Building with SBT
=================

Gatling uses `SBT <http://www.scala-sbt.org>`__ as its build tool.

You'll need `to install it <http://www.scala-sbt.org/0.13/tutorial/Setup.html>`_ first, or you can use the `sbt script <https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt>`__
from Paul Philips' ``sbt-extras`` for an easier setup.

After SBT has been installed and you're in either one of the project's directories, you can then :

* compile the project : ``sbt compile``
* run the tests : ``sbt test``
* deploy the artifacts in your local Ivy and Maven repositories : ``sbt publishLocal publishM2``

Running Gatling
===============

There are several ways to run Gatling with your locally compiled sources to check that all's working fine.

From the bundle
---------------

After you have successfully built both Gatling and Gatling Highcharts, you can then find in your local Ivy repository the full bundle built by Gatling Highcharts,
which behaves exactly like the bundles published on Sonatype.

The bundle can be found in ``<Ivy repo root/local/io.gatling.highcharts/gatling-charts-highcharts/<version>/zips/gatling-charts-highcharts-bundle.zip``,
``<Ivy repo root>`` usually being ``~/.ivy2/``.

Using Gatling's plugins/integrations
------------------------------------

You can use any of Gatling plugins and integrations with your locally compiled sources.

Just make sure that you change the Gatling's version you're using in your project to the current SNAPSHOT version.
