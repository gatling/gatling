.. _simulation-setup:

################
Simulation setup
################

This is where you define the load you want to inject to your server.

You can configure assertions and protocols with this two methods:

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
      rampUsersPerSec(10) to(20) during(10 minutes), // 5
      splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(10 seconds), // 6
      splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(atOnceUsers(30)), // 7
      heavisideUsers(1000) over(20 seconds) // 8
      ).protocols(httpConf)
    )

The building blocks for profile injection the way you want are:

1. ``nothingFor(duration)``: Pause for a given duration.
2. ``atOnceUsers(nbUsers)``: Injects a given number of user at once.
3. ``rampUsers(nbUsers) over(duration)``: Injects a given number of users with a linear ramp over a given duration.
4. ``constantUsersPerSec(rate) during(duration)``: Injects users at a constant rate, defined in users per second, during a given duration.
5. ``rampUsersPerSec(rate1) to (rate2) during(duration)``: Injects users from starting rate to target rate, defined in users per second, during a given duration.
6. ``splitUsers(nbUsers) into(injectionStep) separatedBy(duration)``: Repeatedly execute the defined injection step separated by a pause of the given duration until reaching *nbUsers*, the total number of users to inject.
7. ``splitUsers(nbUsers) into(injectionStep1) separatedBy(injectionStep2)``: Repeatedly execute the first defined injection step (*injectionStep1*) separated by the execution of the second injection step (*injectionStep2*) until reaching *nbUsers*, the total number of users to inject.
8. ``heavisideUsers(nbUsers) over(duration)``: Injects a given number of users following a smooth approximation of the `heaviside step function <http://en.wikipedia.org/wiki/Heaviside_step_function>`__ stretched to a given duration.

.. _simulation-setup-pause:

Global Pause configuration
==========================

The pauses can be configure on ``Simulation`` with a bunch of methods:

* ``disablePauses``: disable the pauses for the simulation
* ``constantPauses``: the pause durations are precisely those filled in the ``pause(duration)`` element.
* ``exponentialPauses``: the pause durations are on average those filled in the ``pause(duration)`` element and follow an exponential distribution.
* ``customPauses(custom: Expression[Long])``: the pause duration is computed by the provided ``Expression[Long]``.
  In this case the filled duration is bypassed.
* ``uniformPauses(plusOrMinus: Double)`` and ``uniformPauses(plusOrMinus: Duration)``:
  the pause durations are on average those filled in the ``pause(duration)`` element and follow an uniform distribution.

.. note:: Pause definition can also be configure at scenario level.

.. _simulation-setup-throttling:

Throttling
==========

If you want to reason in terms of request per second and not in terms of users, Gatling support throttling with the ``throttle`` method, e.g.::

  setUp(...).throttle(reachRps(100) in (10 seconds), holdFor(1 minute), jumpToRps(50), holdFor(2 hours))

Thus simulation will reach 100 req/s with a ramp of 10 seconds, then hold this throughput for 1 minute, jump to 50 req/s and finally hold this throughput for 2 hours.

The building block for the throttling are:

* ``reachRps(target) in (duration)``: target a throughput with a ramp over a given duration.
* ``jumpToRps(target)``: jump immediately to a given targeted throughput.
* ``holdFor(duration)``: hold the current throughput for a given duration.

.. warning:: Define throttling don't prevent you to inject users on the scenario level.
             Throttling try to ensure a targeted throughput on the simulation level with the given scenarios and their injection profiles.

.. note:: Throttling can also be configure at scenario level.

.. _simulation-setup-maxduration:

Maximum duration
================

Finally you can configure the maximum duration of your simulation with the methods ``maxDuration``.
It is useful if you need to bound the duration of your simulation when you can't predict it.

