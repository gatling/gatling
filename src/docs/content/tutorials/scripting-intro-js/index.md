---
menutitle: Create a simulation with JavaScript
title: Create your first JavaScript-based simulation
description: "Get started with Gatling and JavaScript: install, write your first load test, and execute it."
lead: Learn how to get started with Gatling and create a Gatling simulation with JavaScript.
date: 2023-12-16T18:30:56+02:00
lastmod: 2023-12-16T21:30:56+02:00
---

{{< alert warning >}}
This guide is only intended for the  Gatling JavaScript SDK version `{{< var gatlingJsVersion >}}`.
{{< /alert >}}

Gatling is a highly flexible load-testing platform. You can write load tests in Java, Kotlin, Scala, JavaScript, and TypeScript, or use our [no-code feature](https://gatling.io/features/no-code-generator/) with Gatling Enterprise.

In this guide, we cover a "Hello world"-style example for JavaScript of how to:

 - [install and setup your local dev environment]({{< ref "#install-gatling" >}}),
 - [write your first simulation]({{< ref "#simulation-construction" >}}),
 - [Package and upload your simulation to Gatling Enterprise]({{< ref "#package" >}}),
 - [Create and run a new test on Gatling Enterprise]({{< ref "#test" >}})
 - [test your simulation locally]({{< ref "#run-local" >}}).

{{< alert tip >}}
Join the [Gatling Community Forum](https://community.gatling.io) to discuss load testing with other users. Please try to find answers in the Documentation before asking for help.
{{< /alert >}}

## Setup

This section guides you through installation and setting up your developer environment. This guide uses JavaScript and the `gatling-js-demo` project. The JavaScript SDK is currently available for the `HTTP` protocol only. 

### Sign up for Gatling Enterprise Cloud

Gatling Enterprise Cloud is a fully managed SaaS solution for load testing. Sign up for a [trial account](https://auth.gatling.io/auth/realms/gatling/protocol/openid-connect/registrations?client_id=gatling-enterprise-cloud-public&response_type=code&scope=openid&redirect_uri=https%3A%2F%2Fcloud.gatling.io%2Fr%2Fgatling) to run your first test on Gatling Enterprise Cloud. The [Gatling website](https://gatling.io/features) has a full list of Enterprise features.

### Install Gatling 

{{< alert info >}}
**Prerequisites**  
- [Node.js](https://nodejs.org/) v18 or later (LTS versions only) and npm v8 or later.
{{< /alert >}}

Then, use the following procedure to install Gatling:

1. Download the JavaScript SDK zip file using the following download button:
{{< button title="Download Gatling for JavaScript" >}}
https://github.com/gatling/gatling-js-demo/archive/refs/heads/main.zip{{< /button >}}  

2. Unzip and open the project in your IDE or terminal.
3. Navigate to the `/javascript` folder for JavaScript projects in your terminal. 
4. Run `npm install` to install the packages and dependencies including the `gatling` command. 

## Simulation construction 

This guide introduces the basic Gatling HTTP features. Gatling provides a cloud-hosted web application
[https://computer-database.gatling.io](https://computer-database.gatling.io) for running sample simulations. You'll learn how to construct simulations
using the JavaScript SDK. 

### Learn the simulation components

A Gatling simulation consists of the following:

- Importing Gatling functions.
- Configuring the protocol (commonly HTTP).
- Describing a scenario.
- Setting up the injection profile (virtual users profile).

The following procedure teaches you to develop the simulation from each constituent component. If you want to skip ahead
and copy the final simulation, jump to [Test execution]({{< ref "#test-execution" >}}). Learn more about simulations in the
[Documentation]({{< ref "/reference/script/core/simulation" >}}). 

#### Set up the file 

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

The next step is to describe the user journey. For a web application, this usually consists of a user arriving at the application and then a series of interactions with the application. The following scenario mocks a user arriving on the home page of the [Gatling sample application](https://computer-database.gatling.io). Add the scenario:

{{< include-code "ScriptingIntro3Sample#write-the-scenario" js >}}

See the [Documentation]({{< ref "/reference/script/core/scenario" >}}) for the available scenario
components. 

#### Define the injection profile

The final component of a Gatling simulation is the injection profile. In your simulation you must call the `setUp` function exactly once to configure the injection profile. If you have several scenarios, each needs its own injection profile. 

The following example adds 2 users per second for 60 seconds and each user executes the scenario we defined in [Write the Scenario]({{< ref="#write-the-scenario" >}}). See the [Documentation]({{< ref "/reference/script/core/injection" >}}) for all of the injection profile options. 

{{< include-code "ScriptingIntro4Sample#define-the-injection-profile" js >}}

Congrats! You have written your first Gatling simulation. The next step is to learn how to run the simulation. 

## Test execution

Now, you should have a completed simulation that looks like the following: 

{{< include-code "ComputerDatabaseSimulation#full-example" js >}}

### Package and upload your simulation to Gatling Enterprise { #package }

To run your simulation on Gatling Enterprise, you need to package the script with all of the required files. The output of this step is a file named `package.zip` in the `target` folder. To upload your simulation to Gatling Enterprise: 

1. Run the following command in your terminal:

    ```console
    npx gatling enterprise-package
    ```

2. Log in to your Gatling Enterprise account.
3. Click on **Packages** in the left-side menu.
4. Click the **Create** button.
5. Name the package and choose a team (default is typical for trial accounts) 
6. Upload the `package.zip` file you created in step 1.
7. Click **Save** to save your package to Gatling Enterprise.

### Create and run a new test on Gatling Enterprise { #test }

An executable test on Gatling Enterprise is called a Simulation. To run your first simulation, you need to select some minimum settings. 

1. Click **Simulations** in the left-side menu.
2. Click **Create a simulation**.
3. Click **Create a simulation with a package**
4. Fill in the **General** section including selecting the package you created in the [Package and upload your simulation to Gatling Enterprise]({{< ref "#package" >}}) section.
5. Choose a location for generating virtual users from (load). The sample application provided by Gatling is hoasted near Paris, so this location will usually yield the fastest response times.
6. Click **Save and launch** to start your test! 

{{< alert info >}}
Learn about how to read your load test results in the [Reports documentation]({{< ref "reference/stats/reports/cloud" >}}).
{{</ alert >}}

### Test the simulation locally {{% badge info "Optional" /%}} {#run-local}

The open-source version of Gatling allows you to run simulations locally, generating load from your computer. This is ideal for learning, crafting, and debugging simulations. 

Using the terminal, you can launch your test with the following command in the `javascript` project directory:

```console
npx gatling run --simulation myfirstsimulation
```

When the test has finished, there is an HTML link in the terminal that you can use to access the static report. 
