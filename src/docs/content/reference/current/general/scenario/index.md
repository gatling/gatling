---
title: "Scenario"
description: "Learn all about the DSL specific to scenarios"
lead: "Learn how to execute requests, pause, loops, conditions, throttling and protocols"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

This is the reference of the different components available to write scenarios with Gatling.

## Bootstrapping

`scenario` is the way to bootstrap a new scenario.

{{< include-code "ScenarioSample.scala#bootstrapping" scala >}}

You can use any character in the name of the scenario **except** tabulations: **\t**.

## Structure elements

### Exec

The `exec` method is used to execute an action.
Actions are usually requests (HTTP, LDAP, POP, IMAP, etc) that will be sent during the simulation.
Any action that will be executed will be called with `exec`.

For example, when using the Gatling HTTP module you would write the following line:

{{< include-code "ScenarioSample.scala#exec-example" scala >}}

`exec` can also be passed an [Expression]({{< ref "../../session/expression_el#expression" >}}) function.

This can be used for manually debugging or editing the [Session]({{< ref "../../session/session_api#session" >}}), e.g.:

{{< include-code "ScenarioSample.scala#session-lambda" scala >}}

{{< alert tip >}}
For those who wonder how the plumbing works and how you can return a `Session` instead of `Validation[Session]` in the above examples,
that's thanks to an implicit conversion.
{{< /alert >}}

{{< alert warning >}}
Gatling DSL components are immutable `ActionBuilder`\(s) that have to be chained altogether and are only built once on startup.
The results is a workflow chain of `Action`\(s).
These builders don't do anything by themselves, they don't trigger any side effect, they are just definitions.
As a result, creating such DSL components at runtime in functions is completely meaningless.
If you want conditional paths in your execution flow, use the proper DSL components (`doIf`, `randomSwitch`, etc)
{{< /alert >}}

{{< include-code "ScenarioSample.scala#session-improper" scala >}}

`flattenMapIntoAttributes` is a built-in Session Expression as mentioned above.

It exposes the content of a Map into attributes, e.g.:

{{< include-code "ScenarioSample.scala#flattenMapIntoAttributes" scala >}}

### Pause

#### `pause`

When a user sees a page he/she often reads what is shown and then chooses to click on another link.
To reproduce this behavior, the pause method is used.

There are several ways of using it:

* Fixed pause duration:
  * `pause(duration: Duration)`
  * `pause(duration: String, unit: TimeUnit = TimeUnit.SECONDS)`
  * `pause(duration: Expression[Duration])`
* Uniform random pause duration:
  * `pause(min: Duration, max: Duration)`
  * `pause(min: String, max: String, unit: TimeUnit)`
  * `pause(min: Expression[Duration], max: Expression[Duration])`

{{< alert tip >}}
All those methods also have an optional force parameter that overrides the pause type defined in the set up.
Possible values are the [same ones than for global definition]({{< ref "../simulation_setup#global-pause-configuration" >}}).
{{< /alert >}}

#### `pace`

If you want to control how frequently an action is executed, to target *iterations per hour* type volumes.
Gatling support a dedicated type of pause: `pace`, which adjusts its wait time depending on how long the chained action took.
E.g.:

{{< include-code "ScenarioSample.scala#pace" scala >}}

There are several ways of using it:

* Fixed pace duration:
  * `pace(duration: Duration)`
  * `pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS)`
  * `pace(duration: Expression[Duration])`
* Uniform random pace duration:
  * `pace(min: Duration, max: Duration)`
  * `pace(min: String, max: String, unit: TimeUnit)`
  * `pace(min: Expression[Duration], max: Expression[Duration])`

#### `rendezVous`

In some cases, you may want to run some requests, then pause users until all other users have reached a *rendez-vous point*.
For this purpose Gatling has the `rendezVous(users: Int)` method which takes the number of users to wait.

### Loop statements

{{< alert warning >}}
When using the `counterName` parameter to force loop index attribute name, be careful to only use it in a read-only way.
Otherwise, you might break Gatling underlying component's internal logic.
{{< /alert >}}

#### `repeat`

Repeat the loop a specified amount of times.

{{< include-code "ScenarioSample.scala#repeat-example" scala >}}

*times* can be an Int, an EL string pointing to an Int Session attribute, or an `Expression[Int]`.

*counterName* is optional and can be used to force the name of the loop counter.
Current value can be retrieved on the Session as an attribute with a *counterName* name.

{{< include-code "ScenarioSample.scala#repeat-variants" scala >}}

{{< alert warning >}}
Don't forget that the counter starts at 0!
{{< /alert >}}

#### `foreach`

Repeat the loop for each element in the specified sequence.

{{< include-code "ScenarioSample.scala#foreach" scala >}}

*sequenceName* can be a sequence, an EL string pointing to a `Seq[Any]` Session attribute, or an `Expression[Seq[Any]]`

*elementName* is a the name of the Session attribute that will hold the current element.

*counterName* is optional.

#### `during`

Iterate over the loop during the specified amount of time.

{{< include-code "ScenarioSample.scala#during" scala >}}

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

*exitASAP* is optional and defaults to true. If true, the condition will be evaluated for each element inside the loop, possibly causing to exit before reaching the end of the iteration.

#### `asLongAs`

Iterate over the loop as long as the condition is satisfied.

{{< include-code "ScenarioSample.scala#asLongAs" scala >}}

*condition* is a session function that returns a boolean.

*counterName* is optional.

*exitASAP* is optional and defaults to false. If true, the condition will be evaluated for each element inside the loop, possibly causing to exit before reaching the end of the iteration.

#### `doWhile`

Similar to `asLongAs` but the condition is evaluated after the loop.

{{< include-code "ScenarioSample.scala#doWhile" scala >}}

*condition* is a session function that returns a boolean.

*counterName* is optional.

#### `asLongAsDuring`

Iterate over the loop as long as the condition is satisfied and the duration hasn't been reached.

{{< include-code "ScenarioSample.scala#asLongAsDuring" scala >}}

*condition* is a session function that returns a boolean.

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

#### `doWhileDuring`

Similar to `asLongAsDuring` but the condition is evaluated after the loop.

{{< include-code "ScenarioSample.scala#doWhileDuring" scala >}}

*condition* is a session function that returns a boolean.

*duration* can be an Int for a duration in seconds, or a duration expressed like 500 milliseconds.

*counterName* is optional.

#### `forever`

Iterate over the loop forever.

{{< include-code "ScenarioSample.scala#forever" scala >}}

*counterName* is optional.

### Conditional statements

#### `doIf`

Gatling's DSL has conditional execution support.
If you want to execute a specific chain of actions only when some condition is satisfied, you can do so using the `doIf` method.

{{< include-code "ScenarioSample.scala#doIf" scala >}}

If you want to test complex conditions, you'll have to pass an `Expression[Boolean]`:

{{< include-code "ScenarioSample.scala#doIf-session" scala >}}

#### `doIfEquals`

Îf your test condition is simply to compare two values, you can simply use `doIfEquals`:

{{< include-code "ScenarioSample.scala#doIfEquals" scala >}}

#### `doIfOrElse`

Similar to `doIf`, but with a fallback if the condition evaluates to false.

{{< include-code "ScenarioSample.scala#doIfOrElse" scala >}}

{{< alert warning >}}
`doIfOrElse` only takes an `Expression[Boolean]`, not the key/value signature.
{{< /alert >}}

#### `doIfEqualsOrElse`

Similar to `doIfEquals` but with a fallback if the condition evaluates to false.

{{< include-code "ScenarioSample.scala#doIfEqualsOrElse" scala >}}

#### `doSwitch`

Add a switch in the chain. Every possible sub-chain is defined with a key.
Switch is selected through the matching of a key with the evaluation of the passed expression.
If no switch is selected, the switch is bypassed.

{{< include-code "ScenarioSample.scala#doSwitch" scala >}}

{{< alert warning >}}
When using any kind of switch component, make sure to use parentheses, not curly braces!
{{< /alert >}}

#### `doSwitchOrElse`

Similar to `doSwitch`, but with a fallback if no switch is selected.

{{< include-code "ScenarioSample.scala#doSwitchOrElse" scala >}}

#### `randomSwitch`

`randomSwitch` can be used to emulate simple Markov chains.
Simple means cyclic graphs are not currently supported.

{{< include-code "ScenarioSample.scala#randomSwitch" scala >}}

Percentages sum can't exceed 100%.
If sum is less than 100%, users that won't fall into one of the chains will simply exit the switch and continue.
Once users are done with the switch, they simply continue with the rest of the scenario.

{{< alert tip >}}
Percentages should be format as following: 50% -> 50, 33.3% -> 33.3 and so on.
{{< /alert >}}

#### `randomSwitchOrElse`

Similar to `randomSwitch`, but with a fallback if no switch is selected (i.e.: random number exceeds percentages sum).

{{< include-code "ScenarioSample.scala#randomSwitchOrElse" scala >}}

#### `uniformRandomSwitch`

Similar to `randomSwitch`, but with an uniform distribution amongst chains.

{{< include-code "ScenarioSample.scala#uniformRandomSwitch" scala >}}

#### `roundRobinSwitch`

Similar to `randomSwitch`, but dispatch uses a round-robin strategy.

{{< include-code "ScenarioSample.scala#roundRobinSwitch" scala >}}

### Errors handling

#### `tryMax`

{{< include-code "ScenarioSample.scala#tryMax" scala >}}

*myChain* is expected to succeed as a whole.
If an error happens (a technical exception such as a timeout, or a failed check), the user will bypass the rest of the chain and start over from the beginning.

*times* is the maximum number of attempts.

*counterName* is optional.

#### `exitBlockOnFail`

{{< include-code "ScenarioSample.scala#exitBlockOnFail" scala >}}

Quite similar to tryMax, but without looping on failure.

#### `exitHere`

{{< include-code "ScenarioSample.scala#exitHere" scala >}}

Make the user exit the scenario from this point.

#### `exitHereIf`

{{< include-code "ScenarioSample.scala#exitHereIf" scala >}}

Make the user exit the scenario from this point if the condition holds.
Condition parameter is an `Expression[Boolean]`.

#### `exitHereIfFailed`

{{< include-code "ScenarioSample.scala#exitHereIfFailed" scala >}}

Make the user exit the scenario from this point if it previously had an error.

### Groups definition

{{< include-code "ScenarioSample.scala#group" scala >}}

Create group of requests to model process or requests in a same page.
Groups can be nested.

{{< alert warning >}}
Beware that group names mustn't contain commas.
{{< /alert >}}

## Protocol definition

You can configure protocols at scenario level with `protocols` method:

{{< include-code "ScenarioSample.scala#protocol" scala >}}

See the dedicated section for http protocol definition [here]({{< ref "../../http/protocol" >}}).

## Pause definition

You can configure pause definition at scenario level, see [here]({{< ref "../simulation_setup#global-pause-configuration" >}}) for more information.

## Throttling

You can also configure throttling at scenario level with `throttle` method.

This way, you can configure different throttling profiles for different scenarios running in the same simulation.

{{< include-code "ScenarioSample.scala#throttling" scala >}}

For further information see the dedicated section [here]({{< ref "../simulation_setup#throttling" >}}).
