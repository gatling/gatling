---
title: "Introduction to scripting"
description: "Get started with Gatling: install, write your first load test, and execute it."
lead: "Learn how to get started with Gatling and create a Gatling simulation"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 1020000
---

{{< alert warning >}}
This guide is intended for Gatling versions `3.9.5` and later. 
{{< /alert >}}

Gatling is a highly flexible load-testing platform. You can write load tests in Java, Kotlin, and Scala or use our [no-code feature](https://gatling.io/features/no-code) with Gatling Enterprise. In this guide, we cover a "Hello world"-style example of how to:

 - [install and setup your local dev environment](#install-gatling),
 - [write your first simulation](#simulation-construction),
 - [run your simulation locally](#run-the-test-locally), 
 - [package and upload your simulation to run on Gatling Enterprise Cloud](#package-and-launch-your-test-with-gatling-enterprise-cloud).

{{< alert tip >}}
Join the [Gatling Community Forum](https://community.gatling.io) to discuss load testing with other users. Please try to find answers in the documentation before asking for help.
{{< /alert >}}

## Setup
This section guides you through installation and setting up your developer environment. Gatling has a lot of optionalities, including:

- build tools,
- CI/CD integrations,
- Java, Kotlin, and Scala SDKs

This guide uses Java and the Maven build tool. Gatling recommends that developers use the Java SDK unless they are already experienced with Scala or Kotlin. Java is widely taught in CS courses, requires less CPU for compiling, and is easier to configure in Maven and Gradle. You can adapt the steps to your development environment using reference documentation links provided throughout the guide. 

### Sign up for Gatling Enterprise Cloud
Gatling Enterprise Cloud is a fully managed SaaS solution for load testing. Sign up for a [trial account](https://auth.gatling.io/auth/realms/gatling/protocol/openid-connect/registrations?client_id=gatling-enterprise-cloud-public&response_type=code&scope=openid&redirect_uri=https%3A%2F%2Fcloud.gatling.io%2Fr%2Fgatling) to run your first test on Gatling Enterprise Cloud. The [Gatling website](https://gatling.io/features) has a full list of Enterprise features.

### Install Gatling 
{{< alert prerequisite >}}
**Prerequisites**  
Java 11, 17, or 21 64-bit OpenJDK LTS (Long Term Support) version installed on your local machine. We recommend the [Azul JDK](https://www.azul.com/downloads/?package=jdk#zulu).
{{< /alert >}}

This guide uses the Maven wrapper, which is accessed by downloading the and extracting the  `zip` file:

<p style="text-align: center;"> <a href="https://github.com/gatling/gatling-maven-plugin-demo-java/archive/refs/heads/main.zip"> <button>Download Gatling</button></a></p> 

## Simulation construction 

This guide introduces the basic Gatling HTTP features. Gatling provides a cloud-hosted web application [https://computer-database.gatling.io](https://computer-database.gatling.io) for running sample simulations. You'll learn how to construct simulations using the Java SDK. Code examples for Kotlin and the Scala SDK are available in the Documentation.

### Learn the simulation components

A Gatling simulation consists of the following:
- importing Gatling classes, 
- configuring the protocol (commonly HTTP),
- describing a scenario, 
- setting up the injection profile (virtual user profile).

The following procedure teaches you to develop the simulation from each constituent component. If you want to skip ahead and copy the final simulation, jump to [Test execution](#test-execution). Learn more about simulations in the [Documentation]({{< ref "../../reference/current/core/simulation/index" >}}). 

#### Setup the file 
Once you have downloaded and extracted the Gatling `zip` file, open the project in your integrated development environment (IDE). Gatling recommends the [IntelliJ community edition](https://www.jetbrains.com/idea/download/). 
1. Navigate to and open `src/tests/java/computerdatabase/ComputerDatabaseSimulation`.
2. Modify the simulation by deleting everything below line 7 `import io.gatling.javaapi.http.*;`.
3. The simulation should now look like the following:

   {{< include-code "setup-the-file" java >}}

#### Extend the `Simulation` class 

You must extend Gatling's `Simulation` class to write a script. To extend the `Simulation` class, after the import statements, add: 

{{< include-code "extend-the-simulation-class" java >}}

#### Define the protocol class
Inside the `ComputerDatabaseSimulation` class, add an `HTTP protocol` class. Learn about all of the `HttpProtocolBuilder` options in the [Documentation]({{< ref "../../reference/current/http/protocol/index" >}}). For this example, the `baseUrl` property is hardcoded as the Gatling computer database test site, and the `acceptHeader` and `contentTypeHeader` properties are set to `application/json`.  

{{< include-code "define-the-protocol-class" java >}}


#### Write the scenario
The next step is to describe the user journey. For a web application, this normally consists of a user arriving at the application and then a series of interactions with the application. For example, if you visit https://computer-database.gatling.io, there is a search box on the top left. The following scenario mocks a user arriving on the home page.

See the [Documentation]({{< ref "../../reference/current/core/scenario/index" >}}) for the available scenario components. 

#### Define the injection profile

The final component of a Gatling simulation is the injection profile. The injection profile is contained in the `setUp` block. The following example adds 2 users per second for 60 seconds. See the [Documentation]({{< ref "../../reference/current/core/injection/index" >}}) for all of the injection profile options. 

{{< include-code "define-the-injection-profile" java >}}


Congrats! You have written your first Gatling simulation. The next step is to learn how to run the simulation locally and on Gatling Enterprise Cloud. 


## Test execution

Now, you should have a completed simulation that looks like the following: 


{{< include-code "full-example" java >}}

### Run the test locally

The open-source version of Gatling allows you to run tests locally, generating load from your computer. Running a new or modified test locally is often useful to ensure it works before launching it on Gatling Enterprise Cloud. Using the Maven plugin, you can launch your test with the following command in the project root directory:

```console

# Maven wrapper Mac OS and Linux
./mvnw gatling:test

# Maven wrapper Windows
mvnw.cmd gatling:test

```
When the test has finished, there is an HTML link in the terminal that you can use to access the static report. 

### Run the test on Gatling Enterprise Cloud

Gatling Enterprise Cloud is a feature-rich SaaS platform that is designed for teams and organizations to get the most out of load testing. With the trial account you created in the [Sign up for Gatling Enterprise Cloud](#sign-up-for-gatling-enterprise-cloud) section, you can upload and run your test with advanced configuration, reporting, and collaboration features. 

#### Create an API token
1. Navigate to your Gatling Enterprise Cloud account. 
2. Select **API Tokens** from, the left-side menu.
3. On the API Tokens page, click the **Create** button in the top-right corner.
4. In the modale:
  - enter a token name,
  - select **Configure** for the organizational role,
  - choose the team,
  -  select a team role,
  - click **Save**.
5. Copy the generated token to your local machine. 

{{< alert tip >}}
Once you close the API token modale you cannot view the token again, but if you forget the value the token can be regenerated.
{{< /alert >}}

#### Package and launch your test with Gatling Enterprise Cloud
To package and launch the test on Gatling Enterprise Cloud, you must set the API token and define some test parameters. The following procedure will guide you through the process.

1. Set the API token using the following terminal command in the project root directory, changing `myApiToken` for the token you generated in the [Create an API Token](#create-an-api-token) section:
  
    ```console
    # Mac OS and Linux
    export GATLING_ENTERPRISE_API_TOKEN=myApiToken

    #Windows
    set GATLING_ENTERPRISE_API_TOKEN=myApiToken
    ```
2. Package and launch the test with the following command. 
    ```console
    # Maven wrapper Mac OS and Linux
     ./mvnw gatling:enterpriseStart

    # Maven wrapper Windows
      mvnw.cmd gatling:enterpriseStart 
    ```
3. Answer the following terminal prompts with the bolded option:
  - Do you want to create a new simulation or start an existing one? **1** 
  - Choose one simulation class in your package: **1** myfirstpackage.MyFirstScript
  - Choose a team from the list: **1**
  - Enter a simulation name: **enter for the default option**
  - Do you want to create a new package or upload your project to an existing one? **1**
  - Choose the load generator location: **6** This is for Paris, the closest location to the site host.
  - Enter the number of load generators: **1**
4. Return to your Gatling Enterprise Cloud account to see the simulation deploy and generate real-time data! 

## Keep learning
You have successfully run your first test! To keep learning, we recommend the following resources:

 - [Gatling Academy](https://gatling.io/academy) 
 - [Introduction to the Recorder]({{< ref "../../tutorials/recorder-tutorial/index" >}})
 - [Writing DRY code]({{< ref "../../tutorials/advanced//index" >}})

