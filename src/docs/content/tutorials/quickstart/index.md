---
title: Getting started with the no-code generator
menutitle: No-code quickstart
lead: Learn how to write your first test with the Gatling no-code generator
description: Learn how to set up Gatling Enterprise for the first time
badge:
  type: enterprise
  label: Enterprise
---

This tutorial describes step-by-step instructions for running your first simulation with Gatling Enterprise Cloud.

{{< alert info >}}
**Requirements**
* A Gatling Enterprise Cloud account. Sign up for a [free trial](https://cloud.gatling.io) if you don't already have an account.
{{< /alert >}}

## Introduction

The Gatling no-code generator is the fastest way to discover load testing and how it can improve your application, microservice, or API. The no-code generator is a graphical user interface that lets you:

- [setup your scenario]({{< ref "#setup-your-scenario" >}}),
- [setup the virtual user injection profile]({{< ref "#setup-the-injection-profile" >}}),
- (optional) [apply acceptance criteria]({{< ref "#apply-acceptance-criteria-optional" >}}),
- (optional) [choose your settings]({{< ref "#choose-your-settings-optional" >}}).

Once you start your simulation, the load testing data are displayed in real-time. The following guide assists you in writing and launching your first load test. To keep learning about Gatling and load testing, see these helpful resources:

- [Gatling Community](https://community.gatling.io)
- [Gatling Academy](https://gatling.io/academy/)

## Access Gatling Enterprise Cloud

To access the Gatling no-code generator:

1. Navigate to https://cloud.gatling.io in your web browser.
2. Login or register if you don't have an account. 
3. Click on **Create a simulation without coding** in the _Latest simulation runs_ pane on the landing page. 

The no-code generator is divided into 4 steps and can be exported as a Java-Maven project after you complete your simulation.

{{< img src="no-code-overview.png" alt="An overview of the no-code interface" >}}

## Setup your scenario

The first step is to name your simulation in the **Simulation name** field. 

Next, Setting up a scenario requires defining the user request(s) and any pauses between the user request(s). For this tutorial, we use the Gatling sample website https://computer-database.gatling.io to demonstrate load testing with the no-code generator. To set up the scenario, click the {{< icon link >}} icon to the right of the **Request URLs** field. 

For this example, we are using a `GET` request, but notice that the dropdown menu allows you to pick among all of the common HTTP verbs.  

{{< img src="setup-scenario.png" alt="An example of the scenario description" >}}

{{<alert info>}}
To develop more complex tests:
- You can chain requests together by clicking the **Add request** button and adding subsequent endpoints. 
- Add pauses to simulate user decision times
{{</alert>}}

## Setup the injection profile

The second step in creating a no-code simulation is setting up the injection profile. This is where you have the most options for describing the test. There are 3 broad categories of tests:

- **Capacity tests** tell you how your application performs as resource demand increases.
- **Stress tests** tell you how your application performs when there is a rapid and transient increase in resource demand.
- **Soak tests** tell you how your application performs with a regular load over a long period of time (e.g., test for memory leaks). 

{{< img src="setup-injection.png" alt="An example of the injection profile set up" >}}

Following the test type, inputs describe the test duration and the user injection profile. For this tutorial:

1. Select **Capacity test**.
2. Enter 90 seconds for the total test duration.
3. Enter 1 for the initial user arrival rate.
4. Enter 10 for the final user arrival rate. 

## Choose your settings (optional)

Next, choose the team that owns the simulation and where your traffic originates. 

{{< img src="almost-done.png" alt="An example of the test location and naming step" >}}

Select a team (usually _default_). 

Under the **Location** heading, click the arrow to open the dropdown menu and select a location from the list. The Gatling test web application is hosted near Paris, so this location usually gives the best performance.  

## Apply acceptance criteria (optional) 

Acceptance criteria, also called Assertions, allow you to establish whether or not a simulation result meets your requirements. For example, if you expect 95% or more of your users to experience a response time of 0.25 seconds or faster, you would set the 95th percentile response time to 0.25. 

{{< img src="assertions.png" alt="An example of the assertions option" >}}

To activate acceptance criteria:

1. Click **Global 95th percentile on response time should be lower than** toggle button to enable the criterion.
2. Enter the value 0.25 in the input field.
3. Click **Global success ratio to be higher than** toggle button to enable the criterion.
4. Enter the value 99%.

## Launch the test

Click the **Save and launch** button to launch your no-code simulation. 

Congratulations, you have finished your first load test with Gatling Enterprise. The results are displayed in real-time. At the end of the simulation, you can explore the results. Make sure to visit the **Report** tab to see the detailed results, including:

- response time percentiles,
- connections,
- DNS resolutions.

To keep exploring the no-code generator, click the **Edit simulation** button and change your simulation. Happy testing! 

