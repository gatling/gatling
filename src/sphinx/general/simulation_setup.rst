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

The definition of the injection profile of users is done with the ``inject`` method. This method takes as argument a sequence of injection steps that will be processed sequentially.
E.g.:

.. includecode:: code/SimulationSetup.scala#injection

The building blocks for profile injection the way you want are:

#. ``nothingFor(duration)``: Pause for a given duration.
#. ``atOnceUsers(nbUsers)``: Injects a given number of users at once.
#. ``rampUsers(nbUsers) over(duration)``: Injects a given number of users with a linear ramp over a given duration.
#. ``constantUsersPerSec(rate) during(duration)``: Injects users at a constant rate, defined in users per second, during a given duration. Users will be injected at regular intervals.
#. ``constantUsersPerSec(rate) during(duration) randomized``: Injects users at a constant rate, defined in users per second, during a given duration. Users will be injected at randomized intervals.
#. ``rampUsersPerSec(rate1) to (rate2) during(duration)``: Injects users from starting rate to target rate, defined in users per second, during a given duration. Users will be injected at regular intervals.
#. ``rampUsersPerSec(rate1) to(rate2) during(duration) randomized``: Injects users from starting rate to target rate, defined in users per second, during a given duration. Users will be injected at randomized intervals.
#. ``splitUsers(nbUsers) into(injectionStep) separatedBy(duration)``: Repeatedly execute the defined injection step separated by a pause of the given duration until reaching *nbUsers*, the total number of users to inject.
#. ``splitUsers(nbUsers) into(injectionStep1) separatedBy(injectionStep2)``: Repeatedly execute the first defined injection step (*injectionStep1*) separated by the execution of the second injection step (*injectionStep2*) until reaching *nbUsers*, the total number of users to inject.
#. ``heavisideUsers(nbUsers) over(duration)``: Injects a given number of users following a smooth approximation of the `heaviside step function <http://en.wikipedia.org/wiki/Heaviside_step_function>`__ stretched to a given duration.

.. warning::

  Use the proper injection model that match your use case!

  Your load model is not only about getting the expected throughput (number of requests per second), but also open and close the proper number of connections per second.

  Basic load testing tools such as `wrk <https://github.com/wg/wrk>`_ and `ab <http://httpd.apache.org/docs/2.2/programs/ab.html>`_ only support **closed models**:
  users loop over the scenario so, assuming keep alive is used, you get as many open connections as you have users and you never close them.
  This kind of model is mostly suited for call centers, where new users are queued until an operator hangs up.

  With Gatling's ``constantUsersPerSec`` and ``rampUsersPerSec``, you can build **open models**:
  new users keep on arriving no matter how many users are already there, even if the system over test starts slowing down or crashing.
  This kind of model is the best one for most use cases.

  Then, you have to understand that Gatling's default behavior is mimic human users with browsers, so each virtual user has its own connections.
  If you have a high creation rate of users with a short lifespan, you'll end up opening and closing tons of connections every seconds.
  As a consequence, you might run out of resources (such as ephemeral ports, because your OS can't recycle them fast enough).
  If that's the case, you might:

   * consider scaling out
   * reconsider your injection model: maybe you're testing a webservice that's used by just a few clients, so you should be using a closed model and just few connections
   * tune Gatling's behavior and :ref:`share the connection pool amongst virtual users <http-protocol-connection-sharing>`.

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

If this is not sufficient for some reason then Gatling supports throttling with the ``throttle`` method

.. note::

  * You still have to inject users at the scenario level.
    Throttling tries to ensure a targeted throughput with the given scenarios and their injection profiles (number of users and duration).
    It's a bottleneck, ie an upper limit.
    If you don't provide enough users, you won't reach the throttle.
    If your injection lasts less than the throttle, your simulation will simply stop when all the users are done.
    If your injection lasts longer than the throttle, the simulation will stop at the end of the throttle.
  * Throttling can also be configured :ref:`per scenario <scenario-throttling>`.

.. includecode:: code/SimulationSetup.scala#throttling

This simulation will reach 100 req/s with a ramp of 10 seconds, then hold this throughput for 1 minute, jump to 50 req/s and finally hold this throughput for 2 hours.

The building block for the throttling are:

* ``reachRps(target) in (duration)``: target a throughput with a ramp over a given duration.
* ``jumpToRps(target)``: jump immediately to a given targeted throughput.
* ``holdFor(duration)``: hold the current throughput for a given duration.

.. _simulation-setup-maxduration:

Maximum duration
================

Finally, you can configure the maximum duration of your simulation with the method ``maxDuration``.
It is useful if you need to bound the duration of your simulation when you can't predict it.

.. includecode:: code/SimulationSetup.scala#max-duration
