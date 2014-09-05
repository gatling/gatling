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
E.g.::

  setUp(
    scn.inject(
      nothingFor(4 seconds), // 1
      atOnceUsers(10), // 2
      rampUsers(10) over(5 seconds), // 3
      constantUsersPerSec(20) during(15 seconds), // 4
      constantUsersPerSec(20) during(15 seconds) randomized, // 5
      rampUsersPerSec(10) to(20) during(10 minutes), // 6
      rampUsersPerSec(10) to(20) during(10 minutes) randomized, // 7
      splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(10 seconds), // 8
      splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(atOnceUsers(30)), // 9
      heavisideUsers(1000) over(20 seconds) // 10
      ).protocols(httpConf)
    )

The building blocks for profile injection the way you want are:

#. ``nothingFor(duration)``: Pause for a given duration.
#. ``atOnceUsers(nbUsers)``: Injects a given number of users at once.
#. ``rampUsers(nbUsers) over(duration)``: Injects a given number of users with a linear ramp over a given duration.
#. ``constantUsersPerSec(rate) during(duration)``: Injects users at a constant rate, defined in users per second, during a given duration. Users will be injected at regular intervals.
#. ``constantUsersPerSec(rate) during(duration) randomized``: Injects users at a constant rate, defined in users per second, during a given duration. Users will be injected at randomized intervals.
#. ``rampUsersPerSec(rate1) to (rate2) during(duration)``: Injects users from starting rate to target rate, defined in users per second, during a given duration. Users will be injected at regular intervals.
#. ``rampUsersPerSec(rate1) to(rate2) during(duration) randomized``: Injects users from starting rate to target rate, defined in users per second, during a given duration. Users will be injected at randomized intervals.
#. ``heavisideUsers(nbUsers) over(duration)``: Injects a given number of users following a smooth approximation of the `heaviside step function <http://en.wikipedia.org/wiki/Heaviside_step_function>`__ stretched to a given duration.
#. ``splitUsers(nbUsers) into(injectionStep) separatedBy(duration)``: Repeatedly execute the defined injection step separated by a pause of the given duration until reaching *nbUsers*, the total number of users to inject.
#. ``splitUsers(nbUsers) into(injectionStep1) separatedBy(injectionStep2)``: Repeatedly execute the first defined injection step (*injectionStep1*) separated by the execution of the second injection step (*injectionStep2*) until reaching *nbUsers*, the total number of users to inject.

.. _simulation-setup-pause:

Global Pause configuration
==========================

The pauses can be configure on ``Simulation`` with a bunch of methods:

* ``disablePauses``: disable the pauses for the simulation
* ``constantPauses``: the duration of each pause is precisely that specified in the ``pause(duration)`` element.
* ``exponentialPauses``: the duration of each pause is on average that specified in the ``pause(duration)`` element and follow an exponential distribution.
* ``customPauses(custom: Expression[Long])``: the pause duration is computed by the provided ``Expression[Long]``.
  In this case the filled duration is bypassed.
* ``uniformPauses(plusOrMinus: Double)`` and ``uniformPauses(plusOrMinus: Duration)``:
  the duration of each pause is on average that specified in the ``pause(duration)`` element and follow a uniform distribution.

.. note:: Pause definition can also be configured at scenario level.

.. _simulation-setup-throttling:

Throttling
==========

If you want to reason in terms of requests per second and not in terms of concurrent users,
consider using constantUsersPerSecond() to set the arrival rate of users, and therefore requests,
without need for throttling as well as it will be redundant in most cases.

If this is not sufficient for some reason then Gatling supports throttling with the ``throttle`` method

.. note::

  * You still have to inject users at the scenario level.
    Throttling tries to ensure a targeted throughput with the given scenarios and their injection profiles, it's a bottleneck.
  * Throttling can also be configured :ref:`per scenario <scenario-throttling>`.

::

  setUp(...).throttle(
    reachRps(100) in (10 seconds),
    holdFor(1 minute),
    jumpToRps(50),
    holdFor(2 hours)
  )

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

