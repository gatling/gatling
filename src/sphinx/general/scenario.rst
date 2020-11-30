:tocdepth: 3

.. _scenario:

########
Scenario
########

This is the reference of the different components available to write scenarios with Gatling.

Bootstrapping
=============

``scenario`` is the way to bootstrap a new scenario.

.. includecode:: code/ScenarioSample.scala#bootstrapping

You can use any character in the name of the scenario **except** tabulations: **\t**.

Structure elements
==================

.. _scenario-exec:

Exec
----

The ``exec`` method is used to execute an action.
Actions are usually requests (HTTP, LDAP, POP, IMAP, etc) that will be sent during the simulation.
Any action that will be executed will be called with ``exec``.

For example, when using the Gatling HTTP module you would write the following line:

.. includecode:: code/ScenarioSample.scala#exec-example

.. _scenario-exec-session-expression:

``exec`` can also be passed an :ref:`Expression <expression>` function.

This can be used for manually debugging or editing the :ref:`Session <expression>`, e.g.:

.. includecode:: code/ScenarioSample.scala#session-lambda

.. note::
  For those who wonder how the plumbing works and how you can return a ``Session`` instead of ``Validation[Session]`` in the above examples,
  that's thanks to an implicit conversion.

.. warning::
  Gatling DSL components are immutable ``ActionBuilder``\(s) that have to be chained altogether and are only built once on startup.
  The results is a workflow chain of ``Action``\(s).
  These builders don't do anything by themselves, they don't trigger any side effect, they are just definitions.
  As a result, creating such DSL components at runtime in functions is completely meaningless.
  If you want conditional paths in your execution flow, use the proper DSL components (``doIf``, ``randomSwitch``, etc)

.. includecode:: code/ScenarioSample.scala#session-improper

.. _scenario-exec-function-flatten:

``flattenMapIntoAttributes`` is a built-in Session Expression as mentioned above.

It exposes the content of a Map into attributes, e.g.:

.. includecode:: code/ScenarioSample.scala#flattenMapIntoAttributes

Pause
-----

.. _scenario-pause:

``pause``
^^^^^^^^^

When a user sees a page he/she often reads what is shown and then chooses to click on another link.
To reproduce this behavior, the pause method is used.

There are several ways of using it:

* Fixed pause duration:

  * ``pause(duration: Duration)``
  * ``pause(duration: String, unit: TimeUnit = TimeUnit.SECONDS)``
  * ``pause(duration: Expression[Duration])``

* Uniform random pause duration:

  * ``pause(min: Duration, max: Duration)``
  * ``pause(min: String, max: String, unit: TimeUnit)``
  * ``pause(min: Expression[Duration], max: Expression[Duration])``

.. note::
  All those methods also have an optional force parameter that overrides the pause type defined in the set up.
  Possible values are the :ref:`same ones than for global definition <simulation-setup-pause>`.

.. _scenario-pace:

``pace``
^^^^^^^^

If you want to control how frequently an action is executed, to target *iterations per hour* type volumes.
Gatling support a dedicated type of pause: ``pace``, which adjusts its wait time depending on how long the chained action took.
E.g.:

.. includecode:: code/ScenarioSample.scala#pace

There are several ways of using it:

* Fixed pace duration:

  * ``pace(duration: Duration)``
  * ``pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS)``
  * ``pace(duration: Expression[Duration])``

* Uniform random pace duration:

  * ``pace(min: Duration, max: Duration)``
  * ``pace(min: String, max: String, unit: TimeUnit)``
  * ``pace(min: Expression[Duration], max: Expression[Duration])``

.. _scenario-rendez-vous:

``rendezVous``
^^^^^^^^^^^^^^

In some cases, you may want to run some requests, then pause users until all other users have reached a *rendez-vous point*.
For this purpose Gatling has the ``rendezVous(users: Int)`` method which takes the number of users to wait.

.. _scenario-loops:

Loop statements
---------------
.. warning::
  When using the ``counterName`` parameter to force loop index attribute name, be careful to only use it in a read-only way.
  Otherwise, you might break Gatling underlying component's internal logic.

.. _scenario-repeat:

``repeat``
^^^^^^^^^^

Repeat the loop a specified amount of times.

.. includecode:: code/ScenarioSample.scala#repeat-example

*times* can be an Int, an EL string pointing to an Int Session attribute, or an ``Expression[Int]``.

*counterName* is optional and can be used to force the name of the loop counter.
Current value can be retrieved on the Session as an attribute with a *counterName* name.

.. includecode:: code/ScenarioSample.scala#repeat-variants

.. warning:: Don't forget that the counter starts at 0!

.. _scenario-foreach:

``foreach``
^^^^^^^^^^^

Repeat the loop for each element in the specified sequence.

.. includecode:: code/ScenarioSample.scala#foreach

*sequenceName* can be a sequence, an EL string pointing to a ``Seq[Any]`` Session attribute, or an ``Expression[Seq[Any]]``

*elementName* is a the name of the Session attribute that will hold the current element.

*counterName* is optional.

.. _scenario-during:

``during``
^^^^^^^^^^

Iterate over the loop during the specified amount of time.

.. includecode:: code/ScenarioSample.scala#during

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

*exitASAP* is optional and defaults to true. If true, the condition will be evaluated for each element inside the loop, possibly causing to exit before reaching the end of the iteration.

.. _scenario-aslongas:

``asLongAs``
^^^^^^^^^^^^

Iterate over the loop as long as the condition is satisfied.

.. includecode:: code/ScenarioSample.scala#asLongAs

*condition* is a session function that returns a boolean.

*counterName* is optional.

*exitASAP* is optional and defaults to false. If true, the condition will be evaluated for each element inside the loop, possibly causing to exit before reaching the end of the iteration.

.. _scenario-doWhile:

``doWhile``
^^^^^^^^^^^

Similar to ``asLongAs`` but the condition is evaluated after the loop.

.. includecode:: code/ScenarioSample.scala#doWhile

*condition* is a session function that returns a boolean.

*counterName* is optional.

.. _scenario-asLongAsDuring:

``asLongAsDuring``
^^^^^^^^^^^^^^^^^^

Iterate over the loop as long as the condition is satisfied and the duration hasn't been reached.

.. includecode:: code/ScenarioSample.scala#asLongAsDuring

*condition* is a session function that returns a boolean.

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

.. _scenario-doWhileDuring:

``doWhileDuring``
^^^^^^^^^^^^^^^^^

Similar to ``asLongAsDuring`` but the condition is evaluated after the loop.

.. includecode:: code/ScenarioSample.scala#doWhileDuring

*condition* is a session function that returns a boolean.

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

.. _scenario-forever:

``forever``
^^^^^^^^^^^

Iterate over the loop forever.

.. includecode:: code/ScenarioSample.scala#forever

*counterName* is optional.

.. _scenario-conditions:

Conditional statements
----------------------

.. _scenario-doif:

``doIf``
^^^^^^^^

Gatling's DSL has conditional execution support.
If you want to execute a specific chain of actions only when some condition is satisfied, you can do so using the ``doIf`` method.

.. includecode:: code/ScenarioSample.scala#doIf

If you want to test complex conditions, you'll have to pass an ``Expression[Boolean]``:

.. includecode:: code/ScenarioSample.scala#doIf-session

.. _scenario-doifequals:

``doIfEquals``
^^^^^^^^^^^^^^

Îf your test condition is simply to compare two values, you can simply use ``doIfEquals``:

.. includecode:: code/ScenarioSample.scala#doIfEquals

.. _scenario-doiforelse:

``doIfOrElse``
^^^^^^^^^^^^^^

Similar to ``doIf``, but with a fallback if the condition evaluates to false.

.. includecode:: code/ScenarioSample.scala#doIfOrElse

.. warning:: ``doIfOrElse`` only takes an ``Expression[Boolean]``, not the key/value signature.

.. _scenario-doifequalsorelse:

``doIfEqualsOrElse``
^^^^^^^^^^^^^^^^^^^^

Similar to ``doIfEquals`` but with a fallback if the condition evaluates to false.

.. includecode:: code/ScenarioSample.scala#doIfEqualsOrElse

.. _scenario-doswitch:

``doSwitch``
^^^^^^^^^^^^

Add a switch in the chain. Every possible sub-chain is defined with a key.
Switch is selected through the matching of a key with the evaluation of the passed expression.
If no switch is selected, the switch is bypassed.

.. includecode:: code/ScenarioSample.scala#doSwitch

.. warning:: When using any kind of switch component, make sure to use parentheses, not curly braces!

.. _scenario-doswitchorelse:

``doSwitchOrElse``
^^^^^^^^^^^^^^^^^^

Similar to ``doSwitch``, but with a fallback if no switch is selected.

.. includecode:: code/ScenarioSample.scala#doSwitchOrElse

.. _scenario-randomswitch:

``randomSwitch``
^^^^^^^^^^^^^^^^

``randomSwitch`` can be used to emulate simple Markov chains.
Simple means cyclic graphs are not currently supported.

.. includecode:: code/ScenarioSample.scala#randomSwitch

Percentages sum can't exceed 100%.
If sum is less than 100%, users that won't fall into one of the chains will simply exit the switch and continue.
Once users are done with the switch, they simply continue with the rest of the scenario.

.. note:: Percentages should be format as following: 50% -> 50, 33.3% -> 33.3 and so on.

.. _scenario-randomswitchorelse:

``randomSwitchOrElse``
^^^^^^^^^^^^^^^^^^^^^^

Similar to ``randomSwitch``, but with a fallback if no switch is selected (i.e.: random number exceeds percentages sum).

.. includecode:: code/ScenarioSample.scala#randomSwitchOrElse

.. _scenario-uniformrandomswitch:

``uniformRandomSwitch``
^^^^^^^^^^^^^^^^^^^^^^^

Similar to ``randomSwitch``, but with an uniform distribution amongst chains.

.. includecode:: code/ScenarioSample.scala#uniformRandomSwitch

.. _scenario-roundrobinswitch:

``roundRobinSwitch``
^^^^^^^^^^^^^^^^^^^^

Similar to ``randomSwitch``, but dispatch uses a round-robin strategy.

.. includecode:: code/ScenarioSample.scala#roundRobinSwitch

.. _scenario-errors:

Errors handling
---------------

.. _scenario-trymax:

``tryMax``
^^^^^^^^^^

.. includecode:: code/ScenarioSample.scala#tryMax

*myChain* is expected to succeed as a whole.
If an error happens (a technical exception such as a timeout, or a failed check), the user will bypass the rest of the chain and start over from the beginning.

*times* is the maximum number of attempts.

*counterName* is optional.

.. _scenario-exitblockonfail:

``exitBlockOnFail``
^^^^^^^^^^^^^^^^^^^

.. includecode:: code/ScenarioSample.scala#exitBlockOnFail

Quite similar to tryMax, but without looping on failure.

.. _scenario-exithere:

``exitHere``
^^^^^^^^^^^^

.. includecode:: code/ScenarioSample.scala#exitHere

Make the user exit the scenario from this point.

.. _scenario-exithereif:

``exitHereIf``
^^^^^^^^^^^^^^

.. includecode:: code/ScenarioSample.scala#exitHereIf

Make the user exit the scenario from this point if the condition holds.
Condition parameter is an `Expression[Boolean]`.

.. _scenario-exithereiffailed:

``exitHereIfFailed``
^^^^^^^^^^^^^^^^^^^^

.. includecode:: code/ScenarioSample.scala#exitHereIfFailed

Make the user exit the scenario from this point if it previously had an error.

.. _scenario-groups:

Groups definition
-----------------

.. includecode:: code/ScenarioSample.scala#group

Create group of requests to model process or requests in a same page.
Groups can be nested.

.. warning:: Beware that group names mustn't contain commas.

.. _scenario-protocols:

Protocol definition
===================

You can configure protocols at scenario level with ``protocols`` method:

.. includecode:: code/ScenarioSample.scala#protocol

See the dedicated section for http protocol definition :ref:`here <http-protocol>`.

.. _scenario-pause-def:

Pause definition
================

You can configure pause definition at scenario level, see :ref:`here <simulation-setup-pause>` for more information.

.. _scenario-throttling:

Throttling
==========

You can also configure throttling at scenario level with ``throttle`` method.

This way, you can configure different throttling profiles for different scenarios running in the same simulation.

.. includecode:: code/ScenarioSample.scala#throttling

For further information see the dedicated section :ref:`here <simulation-setup-throttling>`.
