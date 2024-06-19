---
menutitle: Introduction to JavaScript scripting 
title: Introduction to Gatling scripting with JavaScript
description: "Get started with Gatling and JavaScript: install, write your first load test, and execute it."
lead: Learn how to get started with Gatling and create a Gatling simulation with JavaScript.
date: 2023-12-16T18:30:56+02:00
lastmod: 2023-12-16T21:30:56+02:00
---

{{< alert warning >}}
This guide is only intended for the  Gatling JavaScript SDK version {{< var gatlingJsVersion >}}.
{{< /alert >}}

Gatling is a highly flexible load-testing platform. You can write load tests in Java, Kotlin, Scala, JavaScript|TypeScript, or use our [no-code feature](https://gatling.io/features/no-code-generator/) with Gatling Enterprise. In this guide, we cover a "Hello world"-style example for JavaScript of how to:

 - [install and setup your local dev environment]({{< ref "#install-gatling" >}}),
 - [write your first simulation]({{< ref "#simulation-construction" >}}),
 - [run your simulation locally]({{< ref "#run-the-simulation-locally" >}}).

{{< alert tip >}}
Join the [Gatling Community Forum](https://community.gatling.io) to discuss load testing with other users. Please try to find answers in the Documentation before asking for help.
{{< /alert >}}

## Setup

This section guides you through installation and setting up your developer environment. This guide uses JavaScript and the `gatling-js-demo` project. The JavaScript SDK is currently available for the `HTTP` protocol only. 

### Install Gatling 

{{< alert info >}}
**Prerequisites**  
[Node.js](https://nodejs.org/) v18 or later (LTS versions only) and npm v8 or later.
{{< /alert >}}

Then, use the following procedure to install Gatling:

1. Download the Gatling JS demo project zip file using the following download button:
{{< button title="Download Gatling for JavaScript" >}}
https://github.com/gatling/gatling-js-demo/archive/refs/heads/main.zip{{< /button >}}  

2. Unzip and open the project in your IDE or terminal.
3. navigate to the `/javascript` folder for JavaScript projects in your terminal. 
4. Run `npm install` to install the packages and dependencies including the `gatling` command. 

## Simulation construction 

This guide introduces the basic Gatling HTTP features. Gatling provides a cloud-hosted web application
[https://computer-database.gatling.io](https://computer-database.gatling.io) for running sample simulations. You'll learn how to construct simulations
using the JavaScript SDK. 

### Learn the simulation components

A Gatling simulation consists of the following:

- importing Gatling functions, 
- configuring the protocol (commonly HTTP),
- describing a scenario, 
- setting up the injection profile (virtual users profile).

The following procedure teaches you to develop the simulation from each constituent component. If you want to skip ahead
and copy the final simulation, jump to [Test execution]({{< ref "#test-execution" >}}). Learn more about simulations in the
[Documentation]({{< ref "/reference/script/core/simulation" >}}). 

#### Setup the file 

To set up the test file use the following procedure: 

1. In your IDE create the `myfirstsimulation.gatling.js` file in the `javascript/src/` folder.
2. Copy the following import statements and past them in the `myfirstsimulation.gatling.js` file.

{{< include-code "ScriptingIntro1Sample#setup-the-file" js >}}

#### Define the `Simulation` function 

The `simulation` function takes the `setUp` function as an argument, which is used to write a script. To add the `simulation` function, after the import statements, add: 

{{< include-code "ScriptingIntro1Sample#extend-the-simulation-function" js >}}

#### Define an HTTP protocol

Inside the `simulation` function, define an HTTP protocol. Learn about all of the
`HttpProtocolBuilder` options in the [Documentation]({{< ref "/reference/script/protocols/http/protocol" >}}). For
this example, the `baseUrl` property is hardcoded as the Gatling computer database test site, and the `acceptHeader` and
`contentTypeHeader` properties are set to `application/json`. Add the HTTP protocol: 

{{< include-code "ScriptingIntro2Sample#define-the-protocol-class" js >}}

#### Write the scenario

The next step is to describe the user journey. For a web application, this usually consists of a user arriving at the
application and then a series of interactions with the application. The following scenario mocks a user arriving on the
home page of the [Gatling sample application](https://computer-database.gatling.io). Add the scenario:

{{< include-code "ScriptingIntro3Sample#write-the-scenario" js >}}

See the [Documentation]({{< ref "/reference/script/core/scenario" >}}) for the available scenario
components. 

#### Define the injection profile

The final component of a Gatling simulation is the injection profile. In your simulation you must call the `setUp` function exactly once to configure the injection profile. If you have several scenarios, each needs its own injection profile. 

The following example adds 2 users per second for 60 seconds and each user executes the scenario we defined in [Write the Scenario]({{< ref="#write-the-scenario" >}}). See the [Documentation]({{< ref "/reference/script/core/injection" >}}) for all of the injection profile options. 

{{< include-code "ScriptingIntro4Sample#define-the-injection-profile" js >}}

Congrats! You have written your first Gatling simulation. The next step is to learn how to run the simulation locally. 

## Test execution

Now, you should have a completed simulation that looks like the following: 

{{< include-code "ComputerDatabaseSimulation#full-example" js >}}


### Run the Simulation locally 

The open-source version of Gatling allows you to run simulations locally, generating load from your computer.
Using the terminal, you can launch your test with the following command in the `javascript` project directory:

```console

npx gatling run --simulation myfirstsimulation

```

When the test has finished, there is an HTML link in the terminal that you can use to access the static report. 

