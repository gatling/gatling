.. _scenario:

########
Scenario
########

Reference of the different components available to write scenarios with Gatling.

``scenario`` is the way to bootstrap a new scenario.

::

  scenario("My Scenario")

You can use any character in the name of the scenario **except** tabulations: **\t**.

Structure elements
==================

.. _scenario-exec:

Exec
----

``exec`` method is used to execute an action.
Actions are usually requests (HTTP, LDAP, POP, IMAP, etc) that will be sent during the simulation.
Any action that will be executed will be called with exec.

For example, one using Gatling HTTP module would write the following line::

  scenario("My Scenario")
      .exec( http("Get Homepage").get("http://github.com/excilys/gatling") )

Pause
-----

.. _scenario-pause:

``pause``
^^^^^^^^^

When a user sees a page he/she often reads what is shown and then chooses to click on another link.
To reproduce this behavior, the pause method is used.

There are several ways of using it:

* Fixed pause duration:

  * pause(duration: Duration)
  * pause(duration: String, unit: TimeUnit = TimeUnit.SECONDS)
  * pause(duration: Expression[Duration])

* Uniform random pause duration:

  * pause(min: Duration, max: Duration)
  * pause(min: String, max: String, unit: TimeUnit)
  * pause(min: Expression[Duration], max: Expression[Duration])

.. _scenario-pace:

``pace``
^^^^^^^^

If you want to control how frequently an action is executed, to target "iterations per hour" type volumes.
Gatling support a dedicated type of pause: ``pace``, which adjusts its wait time depending on how long the chained action took.
E.g.::

  forever(
    pace(5 seconds)
    .exec(
      pause(1 second, 4 seconds) // Will be run every 5 seconds, irrespective of what pause time is used
    )
  )

There are several ways of using it:

* Fixed pause duration:

  * pace(duration: Duration)
  * pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS)
  * pace(duration: Expression[Duration])

* Uniform random pause duration:

  * pace(min: Duration, max: Duration)
  * pace(min: String, max: String, unit: TimeUnit)
  * pace(min: Expression[Duration], max: Expression[Duration])

.. _scenario-rendez-vous:

``rendezVous``
^^^^^^^^^^^^^^

In some cases, you may want to run some requests, then pause users until all other users have reached a *rendezvous point*.
For this purpose Gatling has the ``rendezVous(users: Int)`` method which takes the number of users to wait.

.. _scenario-loops:

Loop statements
---------------

.. _scenario-repeat:

``repeat``
^^^^^^^^^^

::

  .repeat(times, counterName) {
    myChain
  }

*times* can be an Int, an EL pointing to an Int Session attribute, or an ``Expresion[Int]``.

*counterName* is optional and can be used to force the name of the loop counter.
Current value can be retrieved on the Session as an attribute with a *counterName* name.

.. warning:: Don't forget that the counter starts at 0!

::

  .repeat(20) {myChain}     // will loop on myChain 20 times
  .repeat("${myKey}") {}    // will loop on myChain as many times as the Int value of the Session attribute myKey
  .repeat(session => /* something that returns an Int*/) {}

.. _scenario-foreach:

``foreach``
^^^^^^^^^^^

::

  .foreach(sequenceName, elementName, counterName) {
    myChain
  }

*sequenceName* is the name of a sequence attribute in the ``Session``.

*elementName* is a the name of the Session attribute that will hold the current element.

*counterName* is optional.

.. _scenario-during:

``during``
^^^^^^^^^^

::

  .during(duration, counterName) {
    myChain
  }

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

.. _scenario-forever:

``forever``
^^^^^^^^^^^

::

  .forever(counterName) {
    myChain
  }

*counterName* is optional.

.. _scenario-aslongas:

``asLongAs``
^^^^^^^^^^^^

::

  .asLongAs(condition, counterName) {
    myChain
  }

*condition* is session function that returns a boolean.

*counterName* is optional.

.. _scenario-conditions:

Conditional statements
----------------------

.. _scenario-doif:

``doIf``
^^^^^^^^

Gatling's DSL has conditional execution support.
If you want to execute a specific chain of actions only when some condition is satisfied, you can do so using the doIf method.
It will check if a value in the session equals the one you specified::

  .doIf("${myKey}", "myValue") {
     exec( http("...") ... ) // executed if the session value stored in "myKey" equals "myValue"
  }

As you can see, the executed actions if the condition is false are optional.

If you want to test other conditions than equality, you'll have to use an ``Expression[Boolean]`` to write it::

  .doIf(session => session.getTypedAttribute[String]("myKey").startsWith("admin")) {
    exec( http("if true") ... ) // executed if the session value stored in "myKey" starts with "admin"
  }

.. _scenario-doiforelse:

``doIfOrElse``
^^^^^^^^^^^^^^

Similar to ``doIf``, but with a fallback if the condition evaluates to false.
::

  .doIfOrElse(session => session.getTypedAttribute[String]("myKey").startsWith("admin")) {
     exec( http("if true") ... ) // executed if the session value stored in "myKey" starts with "admin"
  } {
     exec( http("if false") ... ) // executed if the session value stored in "myKey" does not start with "admin"
  }

.. warning:: ``doIfOrElse`` only takes an ``Expression[Boolean]``, not the key/value signature.

.. _scenario-doifequalsorelse:

``doIfEqualsOrElse``
^^^^^^^^^^^^^^^^^^^^

Similar to ``doIfOrElse`` but test the equality of an expected and an actual value.
::

  .doIfOrElse(session => session.getTypedAttribute[String]("myKey"), "expectedValue") {
     exec( http("if true") ... ) // executed if the session value stored in "myKey" equals to "expectedValue"
  } {
     exec( http("if false") ... ) // executed if the session value stored in "myKey" not equals to "expectedValue"
  }

.. _scenario-doswitch:

``doSwitch``
^^^^^^^^^^^^

Add a switch in the chain. Every possible subchain is defined with a key.
Switch is selected through the matching of a key with the evaluation of the passed expression.
If no switch is selected, switch is bypassed.
::

  .doSwitch("${myKey}"){
    key1 -> chain1,
    key1-> chain2
  }

.. _scenario-doswitchorelse:

``doSwitchOrElse``
^^^^^^^^^^^^^^^^^^

Similar to ``doSwitch``, but with a fallback if no switch is selected.
::

  .doSwitchOrElse("${myKey}"){
    key1 -> chain1,
    key1-> chain2
  }{
    fallbackChain
  }

.. _scenario-randomswitch:

``randomSwitch``
^^^^^^^^^^^^^^^^

``randomSwitch`` can be used to emulate simple Markov chains.
Simple means cyclic graphs are not currently supported.
::

  .randomSwitch( // beware: use parentheses, not brackets!
      percentage1 -> chain1,
      percentage2 -> chain2
  )

Percentages sum can't exceed 100%.
If sum is inferior to 100%, users that won't fall into one of the chains will simply exit the switch and continue.
Once users are done with the switch, they simply continue with the rest of the scenario.

.. note:: Percentages should be format as following: 50% -> 50, 33.3% -> 33.3 and so on.

.. _scenario-randomswitchorelse:

randomSwitchOrElse
^^^^^^^^^^^^^^^^^^

Similar to ``randomSwitch``, but with a fallback if no switch is selected (ie: random number exceeds percentages sum).
::

  .randomSwitchOrElse( // beware: use parentheses, not brackets!
      percentage1 -> chain1,
      percentage2 -> chain2
  ) {
    myFallbackChain
  }

.. _scenario-uniformrandomswitch:

``uniformRandomSwitch``
^^^^^^^^^^^^^^^^^^^^^^^

Similar to ``randomSwitch``, but with an uniform distribution amongst chains.
::

  .uniformRandomSwitch( // beware: use parentheses, not brackets!
    chain1,
    chain2
  )

.. _scenario-roundrobinswitch:

``roundRobinSwitch``
^^^^^^^^^^^^^^^^^^^^

Similar to ``randomSwitch``, but dispatch uses a round-robin strategy.
::

  .roundRobinSwitch( // beware: use parentheses, not brackets!
    chain1,
    chain2
  )

.. _scenario-errors:

Errors management
-----------------

.. _scenario-trymax:

``tryMax``
^^^^^^^^^^

::

  .tryMax(times, counterName) {
      myChain
  }

*myChain* is expected to succeed as a whole.
If an error happens (a technical exception such as a time out, or a failed check), the user will bypass the rest of the chain and start over from the beginning.

*times* is the maximum number of attempts.

*counterName* is optional.

.. _scenario-exitblockonfail:

``exitBlockOnFail``
^^^^^^^^^^^^^^^^^^^

::

  .exitBlockOnFail {
      myChain
  }

Quite similar to tryMax, but without looping on failure.

.. _scenario-exithereiffailed:

``exitHereIfFailed``
^^^^^^^^^^^^^^^^^^^^

::
  .exitHereIfFailed

Make the user exit the scenario from this point if it previously had an error.

.. _scenario-groups:

Groups definition
-----------------

::

  .group(groupName) {
    myChain
  }

Create group of requests to model process or requests in a same page.
Groups can be imbricated into another.

When using groups, statistics calculated for each request are aggregated in the parent group.
Aggregated statistics are displayed on the report like request statistics.

Computed cumulated times currently include pauses.

.. _scenario-protocols:

Protocol definition
===================

You can configure protocols at scenario level with ``protocols`` method::

  scn.protocols(httpConf)

See the dedicated section for http protocol definition :ref:`here <http-protocol>`.

.. _scenario-pause-def:

Pause definition
================

You can configure pause definition at scenario level, see :ref:`here <simulation-setup-pause>` for more information.

.. _scenario-throttling:

Throttling
==========

You can also configure throttling at scenario level with ``throttle`` method::

  scn.throttle(reachRps(100) in (10 seconds), holdFor(10 minute))

For further information see the dedicated section :ref:`here <simulation-setup-throttling>`.