---
title: "Quickstart"
description: ""
lead: ""
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Introduction

In this section we will use Gatling to load test a simple cloud hosted web server and will introduce you to the basic elements of the DSL.

### Installing

Please check the [installation section](||< ref "docs/installation" >||) to pick a setup that matches your needs.
Non developers are recommended to start with the bundle setup.

### A Word on Encoding

Gatling's **default encoding is UTF-8**. If you want to use a different one, you have to:

* select the proper encoding while using the Recorder
* **configure the proper encoding in the `gatling.conf` file.** It will be used for compiling your simulations, building your requests and your responses.
* make sure your text editor encoding is properly configured to match.

### A Word on Scala

Gatling simulation scripts are written in [Scala](http://www.scala-lang.org/), **but don't panic!**
You can use all the basic functions of Gatling without knowing much about Scala.
In most situations the DSL will cover most of your needs and you'll be able to build your scenarios.

If you are interested in knowing more about Scala, then we recommend that you have a look at [Twitter's Scala School](http://twitter.github.io/scala_school).

{{< alert tip >}}
Feel also free to join our [Google Group](https://groups.google.com/forum/#!forum/gatling) and ask for help **once you've read this documentation**.
{{< /alert >}}

## Test Case

This page will guide you through most of Gatling HTTP features. You'll learn about *simulations*, *scenarios*, *feeders*, *recorder*, *loops*, etc.

### Application under Test

In this tutorial, we will use an application named *Computer-Database* deployed at the URL: [http://computer-database.gatling.io](http://computer-database.gatling.io).

This application is a simple CRUD application for managing computer models, and was a sample for the [Play Framework](http://www.playframework.com/) before version 2.3.

### Scenario

To test the performance of this application, we will create scenarios representative of what really happens when users navigate it.

Here is what we think a real user would do with the application:

1. A user arrives at the application.
2. The user searches for 'macbook'.
3. The user opens one of the related models.
4. The user goes back to home page.
5. The user iterates through pages.
6. The user creates a new model.

## Basics

### Using the Recorder

To ease the creation of the scenario, we will use the *Recorder*, a tool provided with Gatling that allows you to record your actions on a web application and export them as a Gatling scenario.

This tool is launched with a script located in the *bin* directory:

* On Linux/Unix:

```shell
$GATLING_HOME/bin/recorder.sh
```

* On Windows:

```shell
%GATLING_HOME%\bin\recorder.bat
```

Once launched, the following GUI lets you configure how requests and responses will be recorded.

Set it up with the following options:

* *computerdatabase* package
* *BasicSimulation* name
* *Follow Redirects?* checked
* *Automatic Referers?* checked
* *Black list first* filter strategy selected
* `.*\.css`, `.*\.js` and `.*\.ico` in the black list filters

{{< img src="recorder.png" alt="recorder.png" >}}

After configuring the recorder, all you have to do is to start it and configure your browser to use Gatling Recorder's proxy.

{{< alert tip >}}
For more information regarding Recorder and browser configuration, please check out [Recorder reference page](||< ref "docs/recorder" >||).
{{< /alert >}}

### Recording the scenario

Now simply browse the application:

1. Enter 'Search' tag.
2. Go to the website: [http://computer-database.gatling.io](http://computer-database.gatling.io)
3. Search for models with 'macbook' in their name.
4. Select 'Macbook pro'.
5. Enter 'Browse' tag.
6. Go back to home page.
7. Iterates several times through the model pages by clicking on *Next* button.
8. Enter 'Edit' tag.
9. Click on *Add new computer*.
10. Fill the form.
11. Click on *Create this computer*.

Try to act as a real user would, don't immediately jump from one page to another without taking the time to read.
This will make your scenario closer to real users' behavior.

When you have finished playing the scenario, click on `Stop` in the Recorder interface.

The Simulation will be generated in the folder `user-files/simulations/computerdatabase` of your Gatling installation under the name `BasicSimulation.scala`.

### Gatling scenario explained

Here is the produced output:

```scala
||< include-static "QuickStartSample.scala#quickstart-recorder-output" >||
```

What does it mean?

1. The optional package.
2. The required imports.
3. The class declaration. Note that it extends `Simulation`.
4. The common configuration to all HTTP requests.

{{< alert tip >}}
`val` is the keyword for defining a constant value.
Types are not defined and are inferred by the Scala compiler.
{{< /alert >}}

5. The baseUrl that will be prepended to all relative urls.
6. Common HTTP headers that will be sent with all the requests.
7. The scenario definition.
8. A HTTP request, named *request_1*. This name will be displayed in the final reports.
9. The url this request targets with the *GET* method.
10. Some pause/think time.

{{< alert tip >}}
Duration units default to `seconds`, e.g. `pause(5)` is equivalent to `pause(5 seconds)`.
{{< /alert >}}

11. Where one sets up the scenarios that will be launched in this Simulation.
12. Declaring to inject into scenario named *scn* one single user.
13. Attaching the HTTP configuration declared above.

{{< alert tip >}}
For more details regarding Simulation structure, please check out [Simulation reference page](||< ref "docs/simulation-structure" >||).
{{< /alert >}}

### Running Gatling

Launch the second script located in the *bin* directory:

* On Linux/Unix:

```shell
$GATLING_HOME/bin/gatling.sh
```

* On Windows:

```shell
%GATLING_HOME%\bin\gatling.bat
```

You should see a menu with the simulation examples:

```
Choose a simulation number:
[0] BasicSimulation
```

When the simulation is done, the console will display a link to the HTML reports.

{{< alert tip >}}
If Gatling doesn't work as expected, see our [FAQ](||< ref "docs/faq" >||) or ask on our [Google Group](https://groups.google.com/forum/#!forum/gatling).
{{< /alert >}}

### Going Further

When you're ready to go further, please check out the [Advanced Tutorial](||< ref "docs/advanced_tutorial" >||).
