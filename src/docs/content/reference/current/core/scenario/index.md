---
title: "Scenario"
description: "Learn about the DSL specific to scenarios"
lead: "Learn how to execute requests, pauses, loops and conditions"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 2030400
---

This is the reference of the different components available to write scenarios with Gatling.

## Bootstrapping

`scenario` is the way to bootstrap a new scenario.

{{< include-code "bootstrapping" java kt scala >}}

You can use any character in the name of the scenario **except** tabulations: **\t**.

## Structure Elements

All the components in this section can be either:
* attached to a scenario
* directly created, so they can be passed as parameters, stored in constants, etc
* attached to another component in this section

### Exec

The `exec` method is used to execute an action.
Actions are usually requests (HTTP, LDAP, POP, IMAP, etc) that will be sent during the simulation.
Any action that will be executed will be called with `exec`.

For example, when using the Gatling HTTP module you would write the following line:

{{< include-code "exec" java kt scala >}}

`exec` can also be passed a [Function]({{< ref "../session/function" >}}).

This can be used for manual debugging or to edit the [Session]({{< ref "../session/session_api#session" >}}), e.g.:

{{< include-code "session-lambda" java kt scala >}}

{{< alert warning >}}
Those functions are executed in Gatling's shared threads, so you must absolutely avoid performing long blocking operations in there, such as remote API calls.
{{< /alert >}}

{{< alert warning >}}
Remember that the [Gatling DSL components are merely definitions]({{< ref "../concepts#dsl" >}}). They have absolutely no effect when not chained with other DSL components. **Typically, you can't use them in functions.**
{{< /alert >}}

{{< include-code "session-lambda-bad" java kt scala >}}

### Pause

#### `pause`

When a user sees a page he/she often reads what is shown before choosing to click on another link.
To reproduce this behavior, the pause method is used.

There are several ways to use it.

Fixed pause takes a single parameter:
* `duration`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function

{{< include-code "pause-fixed" java kt scala >}}

Uniform random pause takes 2 parameters:
* `min`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function
* `max`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function

{{< include-code "pause-uniform" java kt scala >}}

{{< alert tip >}}
All those methods also have an optional force parameter that overrides the pause type defined in the setUp.
Possible values are the [same ones than for global definition]({{< ref "../simulation#global-pause-configuration" >}}).
{{< /alert >}}

#### `pace`

You could want to control how frequently an action is executed, to target *iterations per time* type volumes.
Gatling supports a dedicated type of pause: `pace`, which adjusts its wait time depending on the elapsed time since the virtual user last reached this action.
E.g.:

{{< include-code "pace" java kt scala >}}

There are several ways to use it.

Fixed pace takes a single parameter:
* `duration`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function

{{< include-code "pace-fixed" java kt scala >}}

Uniform random pace takes 2 parameters:
* `min`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function
* `max`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function

{{< include-code "pace-uniform" java kt scala >}}

#### `rendezVous`

In some cases, you may want to run some requests, then pause users until all other users have reached a *rendez-vous point*.

It takes a single parameter:
* `users`: the number of users to wait before lifting the waiting point, an int

{{< include-code "rendezVous" java kt scala >}}

### Loop statements

{{< alert warning >}}
When using the `counterName` parameter to force the loop index attribute name, be careful to use it in a read-only way.
Otherwise, you might break the Gatling underlying component's internal logic.
{{< /alert >}}

#### `repeat`

Repeat the loop a specified amount of times.

It takes 2 parameters:
* `times`: the number of times to repeat the loop content, can be an int, a Gatling EL String or a function
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0

{{< include-code "repeat" java kt scala >}}

#### `foreach`

Repeat the loop for each element in the specified sequence.

It takes 3 parameters:
* `seq`: the list of elements to iterate over, can be a List, a Gatling EL String or a function
* `elementName`: the key to the current element in the `Session`
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0

{{< include-code "foreach" java kt scala >}}

#### `during`

Iterate over the loop during the specified amount of time.

It takes 3 parameters:
* `duration`: can be an Int for a duration in seconds, a duration, a Gatling EL String or a function
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0
* `exitASAP` (optional, default true): if true, the condition will be evaluated for each element inside the loop, possibly causing to exit the loop before reaching the end of the iteration.

{{< include-code "during" java kt scala >}}

#### `asLongAs`

Iterate over the loop as long as the condition is satisfied.

It takes 3 parameters:
* `condition`: can be a boolean, a Gatling EL String resolving a boolean or a function
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0
* `exitASAP` (optional, default true): if true, the condition will be evaluated for each element inside the loop, possibly causing to exit the loop before reaching the end of the iteration.

{{< include-code "asLongAs" java kt scala >}}

#### `doWhile`

Similar to `asLongAs` but the condition is evaluated after the loop.

It takes 2 parameters:
* `condition` can be a boolean, a Gatling EL String resolving a boolean or a function
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0

{{< include-code "doWhile" java kt scala >}}

#### `asLongAsDuring`

Iterate over the loop as long as the condition is satisfied and the duration hasn't been reached.

It takes 4 parameters:
* `condition` can be a boolean, a Gatling EL String resolving a boolean or a function
* `duration` can be an Int for a duration in seconds, a duration, a Gatling EL String or a function
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0
* `exitASAP` (optional, default true). If true, the condition will be evaluated for each element inside the loop, possibly causing to exit the loop before reaching the end of the iteration.

{{< include-code "asLongAsDuring" java kt scala >}}

#### `doWhileDuring`

Similar to `asLongAsDuring` but the condition is evaluated after the loop.

It takes 3 parameters:
* `condition` can be a boolean, a Gatling EL String resolving a boolean or a function
* `duration` can be an Int for a duration in seconds, a duration, a Gatling EL String or a function
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0

{{< include-code "doWhileDuring" java kt scala >}}

#### `forever`

Iterate over the loop content forever.

{{< include-code "forever" java kt scala >}}

*counterName* is optional.

### Conditional statements

Gatling's DSL has conditional execution support.

#### `doIf`

Used to execute a specific chain of actions only when some condition is satisfied.

It takes one single parameter:
* `condition` can be a boolean, a Gatling EL String resolving a boolean or a function

{{< include-code "doIf" java kt scala >}}

#### `doIfEquals`

If your test condition is simply to compare two values, you can simply use `doIfEquals`:

It takes 2 parameters:
* `actual` can be a static value, a Gatling EL String or a function
* `expected` can be a static value, a Gatling EL String or a function

{{< include-code "doIfEquals" java kt scala >}}

#### `doIfOrElse`

Similar to `doIf`, but with a fallback if the condition evaluates to false.

It takes one single parameter:
* `condition` can be a boolean, a Gatling EL String resolving a boolean or a function

{{< include-code "doIfOrElse" java kt scala >}}

#### `doIfEqualsOrElse`

Similar to `doIfEquals` but with a fallback if the condition evaluates to false.

It takes 2 parameters:
* `actual` can be a static value, a Gatling EL String or a function
* `expected` can be a static value, a Gatling EL String or a function

{{< include-code "doIfEqualsOrElse" java kt scala >}}

#### `doSwitch`

Add a switch in the chain. Every possible sub-chain is defined with a key.
Switch is selected through the matching of a key with the evaluation of the passed expression.
If no switch is selected, the switch is bypassed.

{{< include-code "doSwitch" java kt scala >}}

#### `doSwitchOrElse`

Similar to `doSwitch`, but with a fallback if no switch is selected.

{{< include-code "doSwitchOrElse" java kt scala >}}

#### `randomSwitch`

`randomSwitch` can be used to emulate simple Markov chains.
Simple means cyclic graphs are not currently supported.

{{< include-code "randomSwitch" java kt scala >}}

Percentages sum can't exceed 100%.
If sum is less than 100%, users that won't fall into one of the chains will simply exit the switch and continue.
Once users are done with the switch, they simply continue with the rest of the scenario.

{{< alert tip >}}
Percentages should be formatted as following: 50% -> 50, 33.3% -> 33.3 and so on.
{{< /alert >}}

#### `randomSwitchOrElse`

Similar to `randomSwitch`, but with a fallback if no switch is selected (i.e.: random number exceeds percentages sum).

{{< include-code "randomSwitchOrElse" java kt scala >}}

#### `uniformRandomSwitch`

Similar to `randomSwitch`, but with an uniform distribution amongst chains.

{{< include-code "uniformRandomSwitch" java kt scala >}}

#### `roundRobinSwitch`

Similar to `randomSwitch`, but dispatch uses a round-robin strategy.

{{< include-code "roundRobinSwitch" java kt scala >}}

### Errors handling

#### `tryMax`

Any error (a technical exception such as a timeout, or a failed check) in the wrapped chain would cause the virtual user to interrupt and start over from the beginning, up to a maximum number of times.
All requests performed in failing iterations will be logged, including the failing one.

It takes 2 parameters:
* `times`: the maximum number of attempts, an int
* `counterName` (optional): the key to store the loop counter in the `Session`, starting at 0

{{< include-code "tryMax" java kt scala >}}

#### `exitBlockOnFail`

Similar to tryMax, but without retrying on failure.

{{< include-code "exitBlockOnFail" java kt scala >}}

#### `exitHere`

Make the user exit the scenario from this point.

{{< include-code "exitHere" java kt scala >}}

#### `exitHereIf`

Make the user exit the scenario from this point if the condition holds.

In takes one single parameter:
* `condition`: can be a boolean, a Gatling EL String resolving a boolean or a function

{{< include-code "exitHereIf" java kt scala >}}

#### `exitHereIfFailed`

Make the user exit the scenario from this point if it previously had an error.

{{< include-code "exitHereIfFailed" java kt scala >}}

#### `stopInjector`

Make the user exit abruptly stop the injector.

In takes one single parameter:
* `message`: can be a static String, a Gatling EL String resolving a String or a function, used to log an ERROR message

{{< include-code "stopInjector" java kt scala >}}

### Groups

Create a group of requests to model processes or requests in the same page.
Groups can be nested.

{{< include-code "group" java kt scala >}}

{{< alert warning >}}
Beware that group names mustn't contain commas.
{{< /alert >}}
