#####################
Simulation definition
#####################

Set up
======

The simulation definition, this is where you define the load you want to inject to your server.
For this purpose you have to use the ``setUp`` method, eg::

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpConf)


The ``setUp`` method take a list of scenarios with their injection profile, for a deepest look at injection API see :ref:`here <injection_api>`.

You can configure assertion and protocols with this two methods:

* ``assertions``: set assertions on the simulation, see the dedicated section :ref:`here <assertions>`
* ``protocols``: set protocols definitions, see the dedicated section for http protocol definition :ref:`here <http_protocol>`.

.. _pause_definition:

Pause definition
================

The pauses can be configure on ``Simulation`` with a bunch of methods:

* ``disablePauses``: disable the pauses for the simulation
* ``constantPauses``: the pause durations are precisely those filled in the ``pause(duration)`` element.
* ``exponentialPauses``: the pause durations are on average those filled in the ``pause(duration)`` element and follow an exponential distribution.
* ``customPauses(custom: Expression[Long])``: the pause duration is computed by the provided ``Expression[Long]``.
  In this case the filled duration is bypassed.
* ``uniformPauses(plusOrMinus: Double)`` and ``uniformPauses(plusOrMinus: Duration)``:
  the pause durations are on average those filled in the ``pause(duration)`` element and follow an uniform distribution.

.. note:: Pause definition can also be configure at scenario level.

.. _throttling:

Throttling
==========

If you want reason in term of request per second and not in term of users, Gatling support throttling with the ``throttle`` method, e.g.::

  setUp(...).throttle(reachRps(100) in (10 seconds), holdFor(1 minute), jumpToRps(50), holdFor(2 hours))

Thus simulation will reach 100 req/s with a ramp of 10 seconds, then hold this throughput for 1 minute, jump to 50 req/s and finally hold this throughput for 2 hours.

The building block for the throttling are:

* ``reachRps(target) in (duration)``: target a throughput with a ramp over a given duration.
* ``jumpToRps(target)``: jump immediately to a given targeted throughput.
* ``holdFor(duration)``: hold the current throughput for a given duration.

.. warning:: Define throttling don't prevent you to inject users on the scenario level.
             Throttling try to ensure a targeted throughput on the simulation level with the given scenarios and their injection profiles.

.. note:: Throttling can also be configure at scenario level.

Maximum duration
================

Finally you can configure the maximum duration of your simulation with the methods ``maxDuration``.
It is useful if you need to bound the duration of your simulation when you can't predict it.

