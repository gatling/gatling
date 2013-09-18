.. _license:

#######
License
#######

Developer motivations
=====================

We try as much as possible to distribute Gatling and its side projects under Apache Licence v2.0.
As much as possible means that we sometimes have to cope with some dependencies that are not Apache Licence v2.0 compatible or even not open source friendly.

Copyright of the source code and the Gatling logos belongs to `eBusiness Information <http://www.ebusinessinformation.fr>`_, an `Excilys Group <http://www.excilys.com>`_ company.

Main Gatling project
====================

Gatling is licensed under the Apache License v2.0 available here: `HTML <http://www.apache.org/licenses/LICENSE-2.0.html>`_ | `TXT <http://www.apache.org/licenses/LICENSE-2.0.txt>`_

Highcharts based modules
========================

The default report generation module makes use of Highcharts and Highstock, two great javascript librairies developed by Highsoft. These libraries are neither open source, nor free for our use case.

Our sponsor, eBusiness Information purchased developer licences, but as a consequence, this module cannot be open sourced and has to be developed in a separated project : `gatling-highcharts <https://github.com/excilys/gatling-highcharts>`_.
However, for convenience reasons, we distribute a ready to use bundle.

Making it short, we distribute this module for free with the following restrictions :

* one can not edit the source code without buying a Highcharts licence for working on Gatling
* the shipped Highcharts or Highstock copies cannot be used outside of Gatling standard usage

For more information, please refer to the `gatling-highcharts licence <https://github.com/excilys/gatling-highcharts/blob/1.5.X/gatling-charts-highcharts/src/main/resources/META-INF/LICENCE>`_ or feel free to `ask <https://groups.google.com/forum/#!forum/gatling>`_.

If Highsoft happens to change its licensing policy in a more open-source friendly manner, we'd gladly change our own.

Third-party libraries
=====================

Gatling uses several existing libraries. They are listed below along with their respective licenses.

* Scala - `Scala License <http://www.scala-lang.org/node/146>`_ (BSD-like License)
* Async Http Client - `Apache License v2.0 <http://www.apache.org/licenses/LICENSE-2.0.txt>`_
* Netty - `Apache License v2.0 <http://www.apache.org/licenses/LICENSE-2.0.txt>`_
* Akka - `Apache License v2.0 <http://www.apache.org/licenses/LICENSE-2.0.txt>`_
* Logback - `LGPL 2.1 <http://www.gnu.org/licenses/lgpl-2.1.txt>`_
* SLF4J - `MIT License <http://www.opensource.org/licenses/mit-license.php>`_
* Joda Time - `Apache License v2.0 <http://www.apache.org/licenses/LICENSE-2.0.txt>`_
* Scalate - `Apache License v2.0 <http://www.apache.org/licenses/LICENSE-2.0.txt>`_
* Jaxen - `Jaxen License <http://jaxen.codehaus.org/license.html>`_
* Grizzled - `BSD-like License <http://software.clapper.org/grizzled-scala/license.html>`_
* Jackson Mapper - `Apache License v2.0 <http://www.apache.org/licenses/LICENSE-2.0.txt>`_
* Specs - `Specs License <https://raw.github.com/etorreborre/specs2/1.8.2/LICENSE.txt>`_