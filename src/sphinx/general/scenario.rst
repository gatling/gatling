########
Scenario
########

Structure elements
==================

Exec
----

* exec

Pause
-----

* pause
* pace
* rendezVous

Feeds
-----

* feed

Loop statements
---------------

* repeat
* foreach
* during
* forever
* asLongAs

Conditional statements
----------------------

* doIf
* doIfOrElse
* doIfEqualsOrElse
* doSwitch
* doSwitchOrElse
* randomSwitch
* randomSwitchOrElse
* uniformRandomSwitch
* roundRobinSwitch

Errors management
-----------------

* exitBlockOnFail
* tryMax
* exitHereIfFailed

Groups definition
-----------------

* group

.. _injection_api:

Injection API
=============

* def rampUsers(users: Int)
*	def heavisideUsers(users: Int)
*	def atOnceUsers(users: Int)
*	def splitUsers(users: Int)

*	def constantUsersPerSec(rate: Double)
*	def rampUsersPerSec(rate1: Double)

*	def nothingFor(d: FiniteDuration)

Protocol definition
===================

Pause definition
================

Throttling
==========
