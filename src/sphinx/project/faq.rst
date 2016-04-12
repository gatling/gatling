.. _faq:

###
FAQ
###

If you are here, chances are that Gatling does not work as expected.
To help you fixing the problem, here is a list of common problems and their solutions.

If you can't find a solution here, consider joining our `Google Group <https://groups.google.com/forum/#!forum/gatling>`_.

.. _faq-gatling-highcharts-split:

* **Why the hell did you move gatling-highcharts into a dedicated project?**

Highcharts and Highstock are javascript libraries whose license is not open-source friendly.
We pay license fees so that we can package and distribute them and let people use them **for free**, but this module can't be open sourced.

We really want to keep as much code as possible under Apache 2, so we move the reports generation library implementation into a separate project `<https://github.com/gatling/gatling-highcharts>`_.

If anyone can come with an Apache 2 licensed solution that's as sexy and plug-and-play as Highcharts and Highstock, we'd gladly make it the default implementation and integrate it into the main project!

See :ref:`License section <license>`

.. _faq-gatling-highcharts-split2:

* **I built from sources and got a java.lang.NoClassDefFoundError: io/gatling/charts/component/impl/ComponentLibraryImpl**

See up here, the Highcharts based charts implementation is hosted in a separate project.
You have to build it too.

.. _faq-xss:

* **Can't compile long scenarios**

Scenarios use method chaining **a lot**.
The longer the chain, the bigger the stack size required by the compiler to compile them.

This parameter can be increased with the ``-Xss`` JVM parameter.
Another solution is of course to split into smaller chains.

Since 2M3, Gatling forks a new process for running so compiler, so that one can tune JVM differently for compiler and running.
The compiler JVM can be tuned with a parameter named ``gatling.core.zinc`` in `gatling.conf <https://github.com/gatling/gatling/blob/master/gatling-core/src/main/resources/gatling-defaults.conf#49>`_.

.. _faq-warmup:

* **I get a "Connection timed out: no further information to http://gatling-tool.org", what happened?**

Since 1.2.0, Gatling has an option for sending a request in order to warm up the engine and have more precise statistics during the run.
This option is enabled by default and http://gatling.io is the default url.

If Gatling can't reach out this url either because you don't have a connection, or because it requires a proxy configuration, you'll get this stacktrace.

Either disable this feature, or change the target url. See documentation :ref:`here <http-protocol-warmup>`.

.. _faq-class-size:

* **The compiler complains my Simulation class is too big**

Scala classes have the same limitations as Java ones.
For example, the amount of code inside a method can't exceed 64Kb.

If you are in this case (for example, you recorded a big maven installation), you should consider refactoring things a little bit.

If you want to achieve modular and maintainable scenarios, you should consider externalizing processes as chains in other Scala objects.

If you don't care about maintainability but just want to quickly play what you recorded, you can move the chains outside of the ``apply`` method.
Also consider changing them from ``val`` to ``lazy val`` if you have multiple Simulations in your directory.

.. _faq-maven-log:

* **How can I override the maven-gatling-plugin log level?**

* either set a JVM param ``-Dlogback.configurationFile=/path/to/config.xml``
* or add a ``logback-test.xml`` to your classpath that will have precedence over the embedded ``logback.xml`` file

.. _faq-http-caching:

* **I don't get the number of HTTP requests I expect?**

Are you sure that some requests are not being cached?
Gatling does its best to simulate real users behavior, so HTTP caching is enabled by default.

Depending on your use case, you might either realize that the number of requests is actually perfectly fine, or you might want to :ref:`disable caching <http-protocol-caching>`.

.. _faq-scheduler:

* **Does Gatling have a scheduler?**

No.
For now, we consider this is not a task for a load testing tool.
We provide a Jenkins plugin and it's easy to call the Gatling launch scripts from the scheduler of your choice.

.. _faq-multiple-simulations:

* **Can Gatling launch several simulations sequentially?**

No.

It was possible in old versions, but it caused tons of problems, so we removed this feature.

However, just like scheduling, that's something very easy to achieve outside Gatling. For example, one can configure `multiple executions <http://maven.apache.org/guides/mini/guide-default-execution-ids.html>`_ of the Gatling maven plugin, or multiple Jenkins jobs.

.. _faq-method_too_large:

* **I have a HUGE simulation and I get a "Method too large" compile error**

In Java and Scala, there's a method size limit. Here, the method is your Simulation constructor.

Typically, you have to move your chains out of your Simulation class, for example into objects:

.. includecode:: code/FAQ.scala#chains

.. _dandling-connections:

* **I have dandling connections that don't get closed after timeout**

This issue has been reported once, and preferring IPv4 fixed it::

  -Djava.net.preferIPv4Stack=true

