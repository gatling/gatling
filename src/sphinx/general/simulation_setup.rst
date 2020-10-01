.. _simulation-setup:

################
Simulation setup
################

This is where you define the load you want to inject to your server.

You can configure assertions and protocols with these two methods:

* ``assertions``: set assertions on the simulation, see the dedicated section :ref:`here <assertions>`
* ``protocols``: set protocols definitions, see the dedicated section :ref:`here <http-protocol>`.

.. _simulation-inject:

Injection
=========

The definition of the injection profile of users is done with the ``inject`` method. This method takes as an argument a sequence of injection steps that will be processed sequentially.

Open vs Closed Workload Models
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When it comes to load model, systems behave in 2 different ways:

* Closed systems, where you control the concurrent number of users
* Open systems, where you control the arrival rate of users

Make sure to use the proper load model that matches the load your live system experiences.

Closed system are system where the number of concurrent users is capped.
At full capacity, a new user can effectively enter the system only once another exits.

Typical systems that behave this way are:

* call center where all operators are busy
* ticketing websites where users get placed into a queue when the system is at full capacity

On the contrary, open systems have no control over the number of concurrent users: users keep on arriving even though applications have trouble serving them.
Most websites behave this way.

If you're using a closed workload model in your load tests while your system actually is an open one, your test is broken and you're testing some different imaginary behavior.
In such case, when the system under test starts to have some trouble, response times will increase, journey time will become longer, so number of concurrent users will increase
and injector will slow down to match the imaginary cap you've set.

You can read more about open and closed models `here <https://www.usenix.org/legacy/event/nsdi06/tech/full_papers/schroeder/schroeder.pdf>`_ and on `our blog <https://gatling.io/2018/10/04/gatling-3-closed-workload-model-support/>`_.

.. warning::

  Open and closed workload models are antinomical and you can't mix them in the same injection profile.

.. _simulation-inject-open:

Open Model
^^^^^^^^^^

.. includecode:: code/SimulationSetupSample.scala#open-injection

The building blocks for profile injection the way you want are:

#. ``nothingFor(duration)``: Pause for a given duration.
#. ``atOnceUsers(nbUsers)``: Injects a given number of users at once.
#. ``rampUsers(nbUsers) during(duration)``: Injects a given number of users distributed evenly on a time window of a given duration.
#. ``constantUsersPerSec(rate) during(duration)``: Injects users at a constant rate, defined in users per second, during a given duration. Users will be injected at regular intervals.
#. ``constantUsersPerSec(rate) during(duration) randomized``: Injects users at a constant rate, defined in users per second, during a given duration. Users will be injected at randomized intervals.
#. ``rampUsersPerSec(rate1) to (rate2) during(duration)``: Injects users from starting rate to target rate, defined in users per second, during a given duration. Users will be injected at regular intervals.
#. ``rampUsersPerSec(rate1) to(rate2) during(duration) randomized``: Injects users from starting rate to target rate, defined in users per second, during a given duration. Users will be injected at randomized intervals.
#. ``heavisideUsers(nbUsers) during(duration)``: Injects a given number of users following a smooth approximation of the `heaviside step function <http://en.wikipedia.org/wiki/Heaviside_step_function>`__ stretched to a given duration.

.. _simulation-inject-closed:

Closed Model
^^^^^^^^^^^^

.. includecode:: code/SimulationSetupSample.scala#closed-injection

#. ``constantConcurrentUsers(nbUsers) during(duration)``: Inject so that number of concurrent users in the system is constant
#. ``rampConcurrentUsers(fromNbUsers) to(toNbUsers) during(duration)``: Inject so that number of concurrent users in the system ramps linearly from a number to another


.. warning::

  You have to understand that Gatling's default behavior is to mimic human users with browsers so, each virtual user has its own connections.
  If you have a high creation rate of users with a short lifespan, you'll end up opening and closing tons of connections every second.
  As a consequence, you might run out of resources (such as ephemeral ports, because your OS can't recycle them fast enough).
  This behavior makes perfect sense when the load you're modeling is internet traffic. You then might consider scaling out, for example with FrontLine, our Enterprise product.

  If you're actually trying to model a small fleet of webservice clients with connection pools, you might want to fine-tune Gatling's behavior and :ref:`share the connection pool amongst virtual users <http-protocol-connection-sharing>`.

.. warning::

  Setting a smaller number of concurrent users won't force existing users to abort. The only way for users to terminate is to complete their scenario.

.. _simulation-inject-meta:

Meta DSL
^^^^^^^^

It is possible to use elements of Meta DSL to write tests in an easier way.
If you want to chain levels and ramps to reach the limit of your application (a test sometimes called capacity load testing), you can do it manually using the regular DSL and looping using map and flatMap.
But there is now an alternative using the meta DSL.

.. includecode:: code/SimulationSetupSample.scala#incrementUsersPerSec

* ``incrementUsersPerSec(usersPerSecAddedByStage)``

.. includecode:: code/SimulationSetupSample.scala#incrementConcurrentUsers

* ``incrementConcurrentUsers(concurrentUsersAddedByStage)``

``incrementUsersPerSec`` is for open workload and ``incrementConcurrentUsers`` is for closed workload (users/sec vs concurrent users)

``separatedByRampsLasting`` and ``startingFrom`` are both optional.
If you don't specify a ramp, the test will jump from one level to another as soon as it is finished.
If you don't specify the number of starting users the test will start at 0 concurrent user or 0 user per sec and will go to the next step right away.

.. _simulation-inject-concurrent:

Concurrent Scenarios
^^^^^^^^^^^^^^^^^^^^

You can configure multiple scenarios in the same ``setUp`` block to started at the same time and executed concurrently.

.. includecode:: code/SimulationSetupSample.scala#multiple

.. _simulation-inject-seq:

Sequential Scenarios
^^^^^^^^^^^^^^^^^^^^

It's also possible with ``andThen`` to chain scenarios so that children scenarios starts once all the users in the parent scenario terminate.

.. includecode:: code/SimulationSetupSample.scala#andThen


.. _no-shard:

Disabling FrontLine Load Sharding
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, FrontLine will distribute your injection profile amongst all injectors when running a distributed test from multiple node.

This might not be the desirable behavior, typically when running a first initial scenario with one single user in order to fetch some auth token to be used by the actual scenario.
Indeed, only one node would run this user, leaving the other nodes without an initialized token.

You can user ``noShard`` to disable load sharding. In this case, all the node will use the injection and throttling profiles as defined in the Simulation.

.. includecode:: code/SimulationSetupSample.scala#noShard


.. _simulation-setup-pause:

Global Pause configuration
==========================

The pauses can be configured on ``Simulation`` with a bunch of methods:

* ``disablePauses``: disable the pauses for the simulation
* ``constantPauses``: the duration of each pause is precisely that specified in the ``pause(duration)`` element.
* ``exponentialPauses``: the duration of each pause is on average that specified in the ``pause(duration)`` element and follow an exponential distribution.
* ``normalPausesWithStdDevDuration(stdDev: Duration)``: the duration of each pause is on average that specified in the ``pause(duration)`` element and follow an normal distribution. ``stdDev`` is a Duration.
* ``normalPausesWithPercentageDuration(stdDev: Double)``: the duration of each pause is on average that specified in the ``pause(duration)`` element and follow an normal distribution. ``stdDev`` is a percentage of the pause value.
* ``customPauses(custom: Expression[Long])``: the pause duration is computed by the provided ``Expression[Long]``.
  In this case the filled duration is bypassed.
* ``uniformPausesPlusOrMinusPercentage(plusOrMinus: Double)`` and ``uniformPausesPlusOrMinusDuration(plusOrMinus: Duration)``:
  the duration of each pause is on average that specified in the ``pause(duration)`` element and follow a uniform distribution.

.. note:: Pause definition can also be configured at scenario level.

.. _simulation-setup-throttling:

Throttling
==========

If you want to reason in terms of requests per second and not in terms of concurrent users,
consider using constantUsersPerSec(...) to set the arrival rate of users, and therefore requests,
without need for throttling as well as it will be redundant in most cases.

If this is not sufficient for some reason, then Gatling supports throttling with the ``throttle`` method.

Throttling is implemented per protocol with support for regular HTTP and JMS.

.. note::

  * You still have to inject users at the scenario level.
    Throttling tries to ensure a targeted throughput with the given scenarios and their injection profiles (number of users and duration).
    It's a bottleneck, ie an upper limit.
    If you don't provide enough users, you won't reach the throttle.
    If your injection lasts less than the throttle, your simulation will simply stop when all the users are done.
    If your injection lasts longer than the throttle, the simulation will stop at the end of the throttle.
  * Throttling can also be configured :ref:`per scenario <scenario-throttling>`.

.. includecode:: code/SimulationSetupSample.scala#throttling

This simulation will reach 100 req/s with a ramp of 10 seconds, then hold this throughput for 1 minute, jump to 50 req/s and finally hold this throughput for 2 hours.

The building block for the throttling are:

* ``reachRps(target) in (duration)``: target a throughput with a ramp over a given duration.
* ``jumpToRps(target)``: jump immediately to a given targeted throughput.
* ``holdFor(duration)``: hold the current throughput for a given duration.

.. _simulation-setup-maxduration:

Maximum duration
================

Finally, with ``maxDuration`` you can force your run to terminate based on a duration limit, even though some virtual users are still running.

It is useful if you need to bound the duration of your simulation when you can't predict it.

.. includecode:: code/SimulationSetupSample.scala#max-duration
