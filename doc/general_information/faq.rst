.. _faq:

###
FAQ
###

If you are here, chances are that Gatling does not work as expected. To help you fixing the problem, here is a list of common problems and their solutions.

**Q: Why the hell did you move gatling-highcharts into a dedicated project?**
=============================================================================

Highcharts and Highstock are javascript libraries whose licence is not open-source friendly. We pay licence fees so that we can package and distribute them and let people use them **for free**, but this module can't be open sourced.

We really want to keep as much code as possible under Apache 2, so we move the reports generation library implementation into a `separate project <https://github.com/excilys/gatling-highcharts>`_.

If anyone can come with an Apache 2 licenced solution that's as sexy and plug-and-play as Highcharts and Highstock, we'd gladly make it the default implementation and integrate it into the main project!

See :ref:`Licence section <license>`

**Q: I built from sources and got a java.lang.NoClassDefFoundError**
====================================================================

See up here, the Highcharts based charts implementation is hosted in a separate project. You have to build it too, and what you are trying to run is probably the ``gatling-charts-highcharts`` bundle.

.. _faq-stack-overflow:

**Q: When I launch my simulation, I get a StackOverflowError, What can I do?**
==============================================================================

It is likely that your simulation contains a very long method chain. You can follow this `advice <https://github.com/excilys/gatling/issues/345#issuecomment-3449721>`_ and cut your simulation or increase in the launch script the stack memory size with the ``-Xss`` switch.

**Q: How do I set up the Grizzly provider instead of the Netty one?**
=====================================================================

Gatling comes by default with Netty, but one can configure it to use Grizzly instead:

* download the jars for Grizzly, for exemple on `maven central <http://search.maven.org>`_:

  * org.glassfish.grizzly/grizzly-framework
  * org.glassfish.grizzly/grizzly-http
  * org.glassfish.grizzly/grizzly-websockets
  * org.glassfish.gmbal/gmbal-api-only
  * org.glassfish.external/management-api

* drop those jars in Gatling lib dir
* configure Gatling to use Grizzly instead of Netty: change Netty into Grizzly in gatling.conf in conf directory

**Q: I get a "Connection timed out: no further information to http://gatling-tool.org", what happens?**
=======================================================================================================

Since 1.2.0, Gatling has an option for sending a request in order to warm up the engine and have more precise statistics during the run. This option is enabled by default and ``http://gatling-tool.org`` is the default url.

If Gatling can't reach out this url either because you don't have a connection, or because it requires a proxy configuration, you'll get this stacktrace.

Either disable this feature, or change the target url. See documentation :ref:`here <http-warmup>`.

**Q: The compiler complains my Simulation class is too big**
============================================================

Scala classes have the same limitations as Java ones. For example, the amount of code inside a method can't exceed 64KB.

If you are in this case (for example, you recorded a big maven installation), you should consider refactoring things a little bit.

If you want to achieve modular and maintainable scenarios, you should consider externalizing processes as chains in other Scala objects.

If you don't care about maintainability but just want to quickly play what you recorded, you can move the chains outside of the ``apply`` method. Also consider changing them from ``val`` to ``lazy val`` if you have multiple Simulations in your directory.

**Q: How can I override the maven-gatling-plugin log level?**
=============================================================

* either set a JVM param ``-Dlogback.configurationFile=/path/to/config.xml``
* or add a ``logback-test.xml`` or ``logback.groovy`` to your classpath that will have precedence over the embedded ``logback.xml`` file

**Q: I don't get the number of HTTP requests I expect?**
========================================================

Are you sure that some requests are not being cached? Gatling does its best to simulate real users behavior, so HTTP caching is enabled by default.

Depending on your use case, you might either realize that the number of requests is actually perfectly fine, or you might want to :ref:`disable caching <http-caching>`.

**Q: Does Gatling have a scheduler?**
=====================================

No, and it won't. We consider this is not a task for a stress tool. We provide a Jenkins plugin, and it's easy to call the Gatling launch scripts from the scheduler of your choice.

**Q: Can Gatling launch several simulations sequentially?**
===========================================================

No. It was possible in old versions, but it caused tons of problems, so we removed this feature. However, just like scheduling, that's something very easy to achieve outside Gatling. For example, one can configure `multiple executions <http://maven.apache.org/guides/mini/guide-default-execution-ids.html>`_ of the Gatling maven plugin, or multiple Jenkins jobs. 

**Q: What's the meaning of the 4 longs in simulation.log file?**
================================================================

Those are currently:

1. first byte sent timestamp
2. last byte sent timestamp
3. first byte received timestamp
4. last byte received timestamp

A timestamp is of course the number of milliseconds since epoch.

* response time (in ms) = 4 - 1
* latency (in ms) = 3 - 2

Beware that the format of this file is an implementation details of the FileDataWriter/FileDataReader combo, so it might be subject to changes.

**Q: I haven't found my problem listed, What can I do?**
========================================================

Your problem might be new, you can ask for a solution on our `Google Group <https://groups.google.com/forum/#!forum/gatling>`_