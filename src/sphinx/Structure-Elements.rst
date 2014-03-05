******************
Structure Elements
******************

Reference of the different components available to write scenarios with
Gatling.

scenario
--------

``scenario`` is the way to bootstrap a new scenario.

.. code:: scala

    scenario("My Scenario")

You can use any character in the name of the scenario **except**
tabulations: **\t**.

``bootstrap`` *(since 1.3.0)*
-----------------------------

Import bootstrap methods with ``import bootstrap._`` let you directly
call DSL methods without having to use ``chain`` like in old Gatling
versions.

``bootstrap`` is actually an empty chain, so it can be used for example
for bootstrapping a Scala foldLeft.

exec
----

This method is used to execute an action. Actions are usually requests
(HTTP, LDAP, POP, IMAP, etc) that will be sent during the simulation.
Any action that will be executed will be called with exec.

For example, one using Gatling HTTP module would write the following
line:

.. code:: scala

    scenario("My Scenario")
        .exec( http("Get Homepage").get("http://github.com/excilys/gatling") )

Since 1.3.0:

-  ``exec`` can be called directly without requiring a ``chain`` element
-  ``exec`` can be passed multiple chains used for inserting multiple
   actions or chains.

.. code:: scala

    // build a chain with actions being inserted one by one
    val chain1 = exec(http("Get Homepage").get("http://github.com/excilys/gatling"))
                 .exec(http("Get Wiki").get("http://github.com/excilys/gatling/wiki"))

    val chain2 = exec(http("Get Homepage").get("http://github.com/excilys/gatling"))
                 .exec(http("Get Issues").get("http://github.com/excilys/gatling/issues"))

    scenario("My Scenario")
        .exec(chain1, chain2)

Session manipulation
~~~~~~~~~~~~~~~~~~~~

Apart from actions, exec can take a function (Session => Session) as
argument. Using this function, you can set a value in the session
between two actions, eg:
``exec(session => session.setAttribute("myKey", "myValue") )``

    Be advised that Sessions are immutable in Gatling. That's why the
    signature of the function is ``Session => Session``. Don't forget to
    return the session if needed.

pause
-----

When a user sees a page he/she often reads what is shown and then
chooses to click on another link. To reproduce this behavior, the pause
method is used.

There are several ways of using it:

**With fixed duration**

.. code:: scala

    pause(4)           // will pause for 4 seconds
    pause(4 seconds)   // new syntax since 1.3.0

**With uniform random duration**

.. code:: scala

    .pause(4, 5)          // will pause between 4 and 5 seconds
    .pause(4 seconds, 5 seconds)

**With exponential random duration *(since 1.2.0)***

.. code:: scala

    .pauseExp(4)          // will pause with a mean value of 4 seconds
    .pauseExp(4 seconds)  // new syntax since 1.3.0

**With custom duration generator *(since 1.3.0)***

.. code:: scala

    .pauseCustom(() => Long)

    Available units are: nanosecond(s), microsecond(s), millisecond(s),
    second(s), minute(s), hour(s), day(s). Note that you'll need to
    import ``akka.util.duration._``. This import is automatically added
    when using the Recorder.

doIf
----

Gatling's DSL has conditional execution support. If you want to execute
a specific chain of actions only when some condition is satisfied, you
can do so using the doIf method. It will check if a value in the session
equals the one you specified:

.. code:: scala

    .doIf("${myKey}", "myValue") {
       exec( http("...") ... ) // executed if the session value stored in "myKey" equals "myValue"
    }

As you can see, the executed actions if the condition is false are
optional.

If you want to test other conditions than equality, you'll have to use a
scala function to write it:

.. code:: scala

    .doIf(session => session.getTypedAttribute[String]("myKey").startsWith("admin")) {
       exec( http("if true") ... ) // executed if the session value stored in "myKey" starts with "admin"
    }

Scala functions may seem complex, but they are really useful. Their
usage is covered in `Advanced usage of
Gatling <Advanced-Usage#wiki-scala-functions>`__.

doIfOrElse
----------

Similar to ``doIf``, but with a fallback if the condition evaluates to
false.

.. code:: scala

    .doIfOrElse("${myKey}", "myValue") {
       exec( http("if true") ... ) // executed if the session value stored in "myKey" is equal to "myValue"
    } {
       exec( http("if false") ... ) // executed if the session value stored in "myKey" is not equal to "myValue"
    }

    .doIfOrElse(session => session.getTypedAttribute[String]("myKey").startsWith("admin")) {
       exec( http("if true") ... ) // executed if the session value stored in "myKey" starts with "admin"
    } {
       exec( http("if false") ... ) // executed if the session value stored in "myKey" does not start with "admin"
    }

randomSwitch
------------

randomSwitch can be used to emulate simple `Markov
chains <http://en.wikipedia.org/wiki/Markov_chain>`__. Simple means
cyclic graphs are not currently supported.

.. code:: scala

    .randomSwitch( // beware: use parentheses, not brackets!
        percentage1 -> chain1,
        percentage2 -> chain2
    )

Percentages sum can't exceed 100%. If sum is inferior to 100%, users
that won't fall into one of the chains will simply exit the switch and
continue. Once users are done with the switch, they simply continue with
the rest of the scenario.

One can omit the percentages. In this case, percentages will be equally
distributed amongst chains.

.. code:: scala

    .randomSwitch(
       chain1,
       chain2
    )

roundRobinSwitch
----------------

Quite similar to ``randomSwitch`` except dispatch uses a round-robin
strategy.

.. code:: scala

    .roundRobinSwitch( // beware: use parentheses, not brackets!
       chain1,
       chain2
    )

repeat *(since 1.3.0)*
----------------------

.. code:: scala

    .repeat(times, counterName) {
        myChain
    }

``times`` can be an Int, an EL pointing to an Int Session attribute, or
a function.

``counterName`` is optional and can be used to force the name of the
loop counter. Current value can be retrieved on the Session as an
attribute with a ``counterName`` name.

    Don't forget that the counter starts at 0!

.. code:: scala

    .repeat(20) {myChain}     // will loop on myChain 20 times
    .repeat("${myKey}") {}    // will loop on myChain as many times as the Int value of the Session attribute myKey
    .repeat(session => /* something that returns an Int*/) {}

during *(since 1.3.0)*
----------------------

.. code:: scala

    .during(duration, counterName) {
        myChain
    }

``duration`` can be an Int for a duration in seconds, or a duration
expressed like ``500 milliseconds``.

``counterName`` is optional.

.. code:: scala

    .during(20) {myChain}     // will loop on myChain 20 seconds
    .during(20 minutes) {myChain}     // will loop on myChain 20 minutes

asLongAs *(since 1.3.0)*
------------------------

.. code:: scala

    .asLongAs(condition, counterName) {
        myChain
    }

``condition`` is session function that returns a boolean.

``counterName`` is optional.

.. code:: scala

    .asLongAs(true) {myChain}     // will loop forever

foreach *(since 1.4.4)*
-----------------------

.. code:: scala

    .foreach(sequenceName, elementName, counterName) {
        myChain
    }

``sequenceName`` is the name of a sequence attribute in the Session.

``elementName`` is a the name of the Session attribute that will hold
the current element.

``counterName`` is optional.

tryMax *(since 1.3.0)*
----------------------

.. code:: scala

    .tryMax(times, counterName) {
        myChain
    }

myChain is expected to succeed as a whole. If an error happens (a
technical exception such as a time out, or a failed check), the user
will bypass the rest of the chain and start over from the beginning.

``times`` is the maximum number of attempts.

``counterName`` is optional.

exitBlockOnFail *(since 1.3.0)*
-------------------------------

.. code:: scala

    .exitBlockOnFail {
        myChain
    }

Quite similar to ``tryMax``, but without looping on failure.

exitHereIfFailed *(since 1.3.0)*
--------------------------------

.. code:: scala

    . exitHereIfFailed

Make the user exit the scenario from this point if it previously had an
error.

group *(since 1.4.0)*
---------------------

.. code:: scala

    .group(groupName) {
        myChain
    }

Create group of requests to model process or requests in a same page.
Groups can be imbricated into another.

When using groups, statistics calculated for each request are aggregated
in the parent group. Aggregated statistics are displayed on the report
like request statistics.

Computed cumulated times currently include pauses.
