---
title: "Advanced Tutorial"
description: "Craft a Gatling simulation"
lead: "Isolate process, configure virtual users, use feeders, checks and looping"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 030
---

In this section, we assume that you have already gone through the [Quickstart]({{< ref "../quickstart" >}}) section and that you have a basic simulation to work with.
We will apply a series of refactorings to introduce more advanced concepts and [Domain Specific Language](https://en.wikipedia.org/wiki/Domain-specific_language) constructs.

## Step 1: Isolate processes

Presently our Simulation is one big monolithic scenario.

So first let us split it into composable business processes, akin to the PageObject pattern with Selenium.
This way, you'll be able to easily reuse some parts and build complex behaviors without sacrificing maintenance.

In our scenario we have three separated processes:

  * Search: search models by name
  * Browse: browse the list of models
  * Edit: edit a given model

We are going to extract those chains and store them into constants (static final fields in Java, object attributes in Scala and Kotlin).
You can create those in dedicated files, or directly in the same file as the Simulation.

{{< include-code "isolate-processes" java kt scala >}}

We can now rewrite our scenario using these reusable business processes:

{{< include-code "processes" java kt scala >}}

## Step 2: Configure virtual users

So, this is great, we can load test our server with... one user!
Let's increase the number of users.

Let's define two populations of users:

* *regular* users: they can search and browse computer models.
* *admin* users: they can search, browse and also edit computer models.

Translating into a scenario this gives:

{{< include-code "populations" java kt scala >}}

To increase the number of simulated users, all you have to do is to change the configuration of the simulation as follows:

{{< include-code "setup-users" java kt scala >}}

Here we set only 10 users, because we don't want to flood our test web application. *Please*, be kind and don't crash our server! ;-)

If you want to simulate 3000 users, you might not want them to start at the same time.
Indeed, real users are more likely to connect to your web application gradually.

Gatling provides `rampUsers` to implement this behavior.
The value of the ramp indicates the duration over which the users will be linearly started.

In our scenario let's have 10 regular users and 2 admins, and ramp them over 10 seconds so we don't hammer the server:

{{< include-code "setup-users-and-admins" java kt scala >}}

## Step 3: Use dynamic data with Feeders and Checks

We have set our simulation to run a bunch of users, but they all search for the same model.
Wouldn't it be nice if every user could search a different model name?

We need dynamic data so that all users don't play exactly the same scenario, and so that we don't end up with a behavior completely different from the live system (due to caching, JIT etc.).
This is where Feeders will be useful.

Feeders are data sources containing all the values you want to use in your scenarios.
There are several types of Feeders, the most simple being the CSV Feeder: this is the one we will use in our test.

First let's create a file named *search.csv* and place it in the `user-files` folder.

This file contains the following lines:

```text
searchCriterion,searchComputerName
Macbook,MacBook Pro
eee,ASUS Eee PC 1005PE
```

Let's then declare a feeder and use it to feed our users with the above data:

{{< include-code "feeder" java kt scala >}}

Explanations:

1. First we create a Feeder from a csv file with the following columns: *searchCriterion*, *searchComputerName*.
2. As the default Feeder strategy is *queue*, we will use the *random* strategy for this test to avoid feeder starvation.
3. Every time a user reaches the feed step, it picks a random record from the Feeder.
   This user has two new Session attributes named *searchCriterion*, *searchComputerName*.
4. We use Session data through [Gatling Expression Language]({{< ref "../../reference/current/core/session/el" >}}) to parameterize the search.
5. We use a [CSS selector check]({{< ref "../../reference/current/core/check#css" >}}) (also parameterized with Gatling Expression Language to capture a part of the HTML response, here a hyperlink, and save it in the user Session under the name *computerUrl*.
6. We use the previously saved hyperlink to get a specific page.

{{< alert tip >}}
For more details regarding *Feeders*, please check out the [Feeder reference page]({{< ref "../../reference/current/core/session/feeder" >}}).

For more details regarding *HTTP Checks*, please check out the [Checks reference page]({{< ref "../../reference/current/http/check" >}}).
{{< /alert >}}

## Step 4: Looping

In the *browse* process we have a lot of repetition when iterating through the pages.
We have four times the same request with a different query param value. Can we change this to not violate the DRY principle?

First we will extract the repeated `exec` block to a function.
Indeed, `Simulation`'s are plain classes, so we can use all the power of the language if needed:

{{< include-code "loop-simple" java kt scala >}}

We can now call this function and pass the desired page number.
But we still have repetitions, it's time to introduce another builtin structure:

{{< include-code "loop-for" java kt scala >}}

Explanations:

1. The `repeat` builtin is a loop resolved at **runtime**.
   It takes the number of repetitions and, optionally, the name of the counter that's stored in the user's Session.
2. As we set the counter name we can use it in Gatling EL and access the nth page.

{{< alert tip >}}
For more details regarding loops, please check out the [Loops reference page]({{< ref "../../reference/current/core/scenario#loops" >}}).
{{< /alert >}}

## Step 5: Check and failure management

Up until now we have only used `check` to extract some data from the html response and store it in the session.
But `check` is also handy to check properties of the response.
By default Gatling checks if the http response status is *20x* or *304*.

To demonstrate failure management we will introduce a `check` on a condition that fails randomly:

{{< include-code "check" java kt scala >}}

Explanations:

1. First we import `ThreadLocalRandom`, to generate random values.
2. We do a check on a condition that's been customized with a lambda.
   It will be evaluated every time a user executes the request and randomly return *200* or *201*.
   As response status is 200, the check will fail randomly.

To handle this random failure we use the `tryMax` and `exitHereIfFailed` constructs as follow:

{{< include-code "tryMax-exitHereIfFailed" java kt scala >}}

Explanations:

1. `tryMax` tries a given block up to n times.
   Here we try a maximum of two times.
2. If all tries fail, the user exits the whole scenario due to `exitHereIfFailed`.

{{< alert tip >}}
For more details regarding conditional blocks, please check out the [Conditional Statements reference page]({{< ref "../../reference/current/core/scenario#conditional-statements" >}}).
{{< /alert >}}

That's all Folks!

{{< alert tip >}}
The files for this tutorial can be found in the distribution in the `user-files/simulations` directory, and on Github [here](https://github.com/gatling/gatling/tree/main/gatling-bundle/src/main/scala/computerdatabase).
{{< /alert >}}
