#########
Migrating
#########

From 1.4.6 to 1.5.0
===================

* the ``gatling.conf`` file format has changed (some blocks have been moved, so fields renamed)
* the custom JsonPath engine has been dropped in favor of `Jayway's one <https://github.com/jayway/JsonPath>`_. Checkout `here <http://goessner.net/articles/JsonPath>`_ for the new syntax. You can also use `this site <http://jsonpath.curiousconcept.com>`_ to check your expressions.
* the underlying CSS Selectors implementation `Jodd's CSSelly <http://jodd.org/doc/csselly>`_ has been replaced with `Jsoup <http://jsoup.org>`_ that is much more performant according to our benchmarks. In most case, migration should be transparent, but you might face minor syntax differences. For example, Jsoup doesn't support ``a[href="api"]`` but only ``a[href=api]``.
* By default, users now have their own HTTP connections. This behavior can be disabled (connections will be shared) with ``.shareConnection`` when setting the HTTP protocol.
* ``doIfOrElse`` has been renamed into ``doIfEqualsOrElse``

From 1.4.5 to 1.4.6
===================

* The maven plugin property named ``simulation`` was causing a clash with ``gatling.conf`` when passed as System property ``-Dgatling.simulation``. As a consequence, it was renamed into ``simulationClass``. Please update your pom.xml or your System properties if you were using it.
* In JsonPath, the attribute axis was redondant with the child one (in JSON, there's no distinction between a child and an attribute, everything is just a property) and was causing bugs. As a consequence, it was dropped. Please use the child axis instead (for example ``foo[bar='baz']`` instead of ``foo[@bar='baz']``)

.. _1.4.X-migration:

From 1.3.X to 1.4.0
===================

* In order to use the new Assertions API, you need to add the following import ::

	import assertions._

* Maven plugin has been rewritten :

  * gatling-charts-highcharts is now a project dependency instead of a plugin dependency
  * Maven plugin supports both fork (fork a new JVM to execute Gatling) and in-process (execute Gatling in the same process as Maven) execution modes

* ``Host`` HTTP header is now automatically computed, so ``.hostHeader`` method on HttpProtocolConfiguration has been removed.
* Simulation classes format has changed:

  * No more apply method that has to return a List
  * New setUp method used to register configured scenarios: ``setUp(scn.users(100))``
  * No more ``.configure`` when configuring a scenario (see example above)

So, a 1.3.X simulation that looked like this one ::

	class GoogleSimulation extends Simulation {
	  val httpConf = httpConfig.baseURL("http://www.google.com")
	  def apply = {
	    val scn = scenario("Google scenario")
	                .exec(http("Google gatling")
	                  .get("/search")
	                  .queryParam("q", "gatling tool"))
	    List(scn.configure.users(10).ramp(10).protocolConfig(httpConf))  
	  }
	}

should now be written like that :: 

	class GoogleSimulation extends Simulation {
	  val httpConf = httpConfig.baseURL("http://www.google.com")
	  val scn = scenario("Google scenario")
	              .exec(http("Google gatling")
	                .get("/search")
	                .queryParam("q", "gatling tool"))
	  setUp(scn.users(10).ramp(10).protocolConfig(httpConf))
	}

.. _1.3.0-migration:

From 1.2.X to 1.3.0
===================

* ``gatling.conf`` file format has changed, beware of not simply replace a 1.3.0 file from a previous 1.2.X settings

* ``doIf`` has been rewritten to look more like a control structure.

``doIf(condition, chain)`` had to be removed due to compatibility issues with the new API.

You should now be using::

	doIf(condition) {
	  // then
	}

	doIfOrElse(condition) {
	 // then
	} {
	 // else
	} 

* Durations should no longer be expressed as ``(value, unit)``. Methods with this kind of signature are now deprecated and will be removed in 1.4.0.

You should now be using the Akka (Scala-to-be) DSL and write them in plain, like in ``5 seconds``.

* Loops have been refactored to look more like control structures.

``loop().times/during/asLongAs`` has been deprecated and will be removed in 1.4.0.

You should now be using::

	repeat(times) {
	  // looped chain
	} 

	during(duration) {
	  // looped chain
	}

	asLongAs(condition) {
	  // looped chain
	}

* ``chain.`` usage has been deprecated and will be removed in 1.4.0. You can now directly bootstrap.

In order to do so, you have to add the following line in existing simulations::

	import boostrap._

* Feeders are now real Iterators. Beware if you've been building your own ones.

.. _1.2.0-migration:

From 1.1.X to 1.2.0
===================

* As the recorder has been ported from Java to Scala, preference serialization has changed. As a consequence, you will have to remove the ``gatling-recorder.ini`` file from your home directory if you've previously saved your recorder preferences.

* ``followRedirect`` is now enabled by default, see wiki :ref:`HTTP chapter <http-follow-redirects>`

* ``maybe`` check condition becomes ``whatever``, see wiki :ref:`Checks chapter <checks-whatever>`

.. _1.1.0-migration:

From 1.0.X to 1.1.0
===================

Txt scenario format has been dropped!
-------------------------------------

No sweat: you can easily convert them into Scala format:

* change extension to .scala
* surround your simulation with the following code to make it a scala class ::

	import com.excilys.ebi.gatling.core.Predef._
	import com.excilys.ebi.gatling.http.Predef._
	import com.excilys.ebi.gatling.jdbc.Predef._

	class YourSimulationName extends Simulation {
	  def apply = {
	    YOUR SIMULATION COMES HERE
	  }
	}

* replace "runSimulations" by "List"

Simulation class is no longer an App
-------------------------------------

Same thing above, you have to wrap your existing code with an apply method and return a List.

Check API changes
-----------------

* ``eq`` becomes ``is``
* ``neq`` becomes ``not``

Launching the recorder
----------------------

The recorder is no longer an ubber jar, so it is now located in the lib directory.
As it depends on other libraries, it is now to be launched from the recorder.sh/bat located in the bin directory.