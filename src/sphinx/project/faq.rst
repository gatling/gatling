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

We really want to keep as much code as possible under Apache 2, so we move the reports generation library implementation into a separate project `<https://github.com/excilys/gatling-highcharts>`_.

If anyone can come with an Apache 2 licensed solution that's as sexy and plug-and-play as Highcharts and Highstock, we'd gladly make it the default implementation and integrate it into the main project!

See :ref:`License section <license>`

.. _faq-gatling-highcharts-split2:

* **I built from sources and got a java.lang.NoClassDefFoundError: com/excilys/ebi/gatling/charts/component/impl/ComponentLibraryImpl**

See up here, the Highcharts based charts implementation is hosted in a separate project.
You have to build it too.

.. _faq-xss:

* **Can't compile long scenarios**

Scenarios use method chaining **a lot**.
The longer the chain, the bigger the stack size required by the compiler to compile them.

This parameter can be increased with the ``-Xss`` JVM parameter.
Another solution is of course to split into smaller chains.

Since 2M3, Gatling forks a new process for running so compiler, so that one can tune JVM differently for compiler and running.
The compiler JVM can be tuned with a parameter named ``gatling.core.zinc`` in `gatling.conf <https://github.com/excilys/gatling/blob/2.0.0-M3a/gatling-core/src/main/resources/gatling-defaults.conf#L44>`_.

.. _faq-warmup:

* **I get a "Connection timed out: no further information to http://gatling-tool.org", what happened?**

Since 1.2.0, Gatling has an option for sending a request in order to warm up the engine and have more precise statistics during the run.
This option is enabled by default and http://gatling-tool.org is the default url.

If Gatling can't reach out this url either because you don't have a connection, or because it requires a proxy configuration, you'll get this stacktrace.

Either disable this feature, or change the target url. See documentation :ref:`here <http-protocol-warmup>`.

.. _faq-class-size:

* **The compiler complains my Simulation class is too big**

Scala classes have the same limitations as Java ones.
For example, the amount of code inside a method can't exceed 64ko.

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

For now, We consider this is not a task for a stress tool.

We provide a Jenkins plugin, and it's easy to call the Gatling launch scripts from the scheduler of your choice.

.. _faq-multiple-simulations:

* **Can Gatling launch several simulations sequentially?**

No.

It was possible in old versions, but it caused tons of problems, so we removed this feature.

However, just like scheduling, that's something very easy to achieve outside Gatling. For example, one can configure `multiple executions <http://maven.apache.org/guides/mini/guide-default-execution-ids.html>`_ of the Gatling maven plugin, or multiple Jenkins jobs.

.. _faq-simulation-log:

* **What's the meaning of the 4 longs in simulation.log file?**

Those are currently:

    1. first byte sent timestamp
    2. last byte sent timestamp
    3. first byte received timestamp
    4. last byte received timestamp

A timestamp is of course the number of milliseconds since epoch.

    * response time (in ms) = 4 - 1
    * latency (in ms) = 3 - 2

Beware that the format of this file is an implementation details of the ``FileDataWriter``/``FileDataReader`` combo, so it might be subject to changes.

.. _faq-elb:

* **Using Amazon Elastic Load Balancing?**

See `this page <http://aws.amazon.com/articles/1636185810492479>`_.

.. note::
    DNS Resolution
    If clients do not re-resolve the DNS at least once per minute, then the new resources Elastic Load Balancing adds to DNS will not be used by clients. This can mean that clients continue to overwhelm a small portion of the allocated Elastic Load Balancing resources, while overall Elastic Load Balancing is not being heavily utilized. This is not a problem that can occur in real-world scenarios, but it is a likely problem for load testing tools that do not offer the control needed to ensure that clients are re-resolving DNS frequently.

Basically, Gatling/JVM's DNS cache has to tuned. A solution is to add ``-Dsun.net.inetaddr.ttl=0`` to the command line.

.. _faq-download:

* **I don't have permissions to download binaries on Google Drive**

The files are definitively public.

There's 99,99% chances that you're logged with a Google enterprise account and Google permissions get messed up.

Just log off and you'll be able to download just fine.
