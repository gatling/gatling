---
menutitle: Introduction to the Recorder
title: Introduction to the Gatling Recorder
aliases:
  - quickstart
description: "Learn the basics about Gatling: installing, using the Recorder to generate a basic raw test and how to execute it."
lead: Learn Gatling concepts and use the Recorder to create a runnable Gatling simulation.
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

## Introduction

{{< alert warning >}}
This tutorial is intended for Gatling versions `{{< var gatlingVersion >}}` and later. 
{{< /alert >}}

The Gatling Recorder allows you to capture browser-based actions to create a realistic user scenario for load testing. The Recorder application is launched from Gatling, using either the bundle or the Maven, Gradle, or sbt plugins.  

In this tutorial, we use Gatling to load test a simple cloud-hosted web server and introduce you to the basic elements of the Recorder. We strongly recommend completing the [Introduction to scripting tutorial]({{< ref "/tutorials/scripting-intro" >}}) before starting to work with the Recorder. 

This tutorial uses Java and the Maven-based Gatling bundle. Gatling recommends that developers use the Java SDK unless they are already experienced with Scala or Kotlin. Java is widely taught in CS courses, requires less CPU for compiling, and is easier to configure in Maven and Gradle. You can adapt the steps to your development environment using reference documentation links provided throughout the guide.

{{< alert tip >}}
Join the [Gatling Community Forum](https://community.gatling.io) to discuss load testing with other users. Please try to find answers in the documentation before asking for help.
{{< /alert >}}

## Prerequisites

This tutorial requires running Gatling on your local machine and using the Mozilla FireFox browser to create your Gatling Script. Additionally, the tutorial uses Gatling Enterprise Cloud to run tests with dedicated load generators and enhanced data reporting features. Use the following links to access each of the prerequisites:

- [Download Gatling](https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/{{< var gatlingVersion >}}/gatling-charts-highcharts-bundle-{{< var gatlingVersion >}}-bundle.zip)
- [Create a Gatling Enterprise Cloud trial account](https://cloud.gatling.io/)
- [Configure your web browser]({{< ref "/reference/script/protocols/http/recorder/#configuration" >}}) (link)


## Plan the user scenario

This tutorial uses an application named _Computer-Database_, which is deployed at the URL: [http://computer-database.gatling.io](http://computer-database.gatling.io). This application is for demonstration purposes and is read-only. Please be kind and only run small proof of concept load tests against the site.

To test the performance of the _Computer-Database_ application, create scenarios representative of what happens when users navigate the site.

The following is an example of what a real user might do with the application. 

1. The user arrives at the application.
2. The user searches for 'macbook'.
3. The user opens one of the search results.
4. The user goes back to the home page.
5. The user browses through pages of records.
6. The user creates a new entry in the computer database. 


## Launch the Recorder

Using the Recorder requires running Gatling in your local development environment. To install Gatling, follow the [Gatling installation]({{< ref "/reference/install/oss" >}}) instructions. Once you have installed Gatling, open the project in your IDE or terminal and launch the recorder:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:recorder
Windows: mvnw.cmd gatling:recorder
{{</ code-toggle >}}


Once launched, the Recorder application opens, allowing you to configure the settings before recording a web browser session.

Set it up with the following options:

* *Recorder Mode* set to *HTTP Proxy*
* *computerdatabase* package
* *RecordedSimulation* name
* *Follow Redirects?* checked
* *Infer HTML resources?* checked
* *Automatic Referers?* checked
* *Remove cache headers?* checked
* *No static resources* clicked
* Select the desired `format`. This tutorial assumes "Java 17" 

After configuring the recorder, all you have to do is click **Start!**. 

{{< alert tip >}}
For more information regarding Recorder and browser configuration, please check out the [Recorder reference documentation]({{< ref "/reference/script/protocols/http/recorder" >}}).
{{< /alert >}}


## Record a website session

Once the Recorder is launched, there are 4 buttons to control the session recording:
- **Add** - adds a tag to organize actions in your session.
- **Clear** - clears the _Executed events_.
- **Cancel** - cancels the Recorder session.
- **Stop & Save** - stops and saves the current Recorder session. 



Based on the scenario described in [Launch the Recorder](#launch-the-recorder) perform the following actions in your configured web browser. Try to act as a real user would, don't immediately jump from one page to another without taking the time to read. This makes your scenario similar to how a real user would behave.

1. Enter a 'Search' tag in the Recorder application and click **Add**.
2. Go to the website: [http://computer-database.gatling.io](http://computer-database.gatling.io)
3. Search for models with 'macbook' in their name.
4. Select 'Macbook pro'.
5. Return to the Recorder application.
6. Enter a 'Browse' tag and click **Add**
6. Go back to the home page.
7. Browse through the model pages by clicking on the **Next** button.
8. Return to the Recorder application.
8. Enter an 'Edit' tag and click **Add**.
9. Return to the browser and Click on **Add new computer**.
10. Fill the form.
11. Click on **Create this computer**.
12. Return to the Recorder application and click **Stop**

The simulation is generated in the folder `src/test/java/`.

{{< alert tip >}}
The scenario components and their functionality are described in the [Intro to Scripting]({{< ref "/tutorials/scripting-intro" >}}) tutorial. For more details regarding the Simulation structure, please check out the [Simulation reference page]({{< ref "/reference/script/core/simulation" >}}).
{{< /alert >}}

## Run the simulation on Gatling Enterprise Cloud 

Gatling Enterprise Cloud is a feature-rich SaaS platform that is designed for teams and organizations to get the most
out of load testing. With the trial account, you created in the [Prerequisites section](#prerequisites), you can upload and run your test with advanced configuration, reporting, and collaboration features. 

From Gatling 3.11 packaging and running simulations on Gatling Enterprise Cloud is simplified by using [configuration as code]({{< ref "reference/execute/cloud/user/configuration-as-code" >}}). In this tutorial, we only use the default configuration to demonstrate deploying your project. You can learn more about customizing your configuration with our [configuration-as-code guide]({{< ref "guides/config-as-code" >}}). 

To deploy and run your simulation on Gatling Enterprise Cloud, use the following procedure: 

1. Generate an [API token]({{< ref "/reference/execute/cloud/admin/api-tokens" >}}) with the `Create` permission in your Gatling Enterprise Cloud account. 
2. Add the API token to your current terminal session by replacing `<your-API-token>` with the API token generated in step 1 and running the following command:

{{< code-toggle console >}}
Linux/MacOS: export GATLING_ENTERPRISE_API_TOKEN=<your-API-token>
Windows: set GATLING_ENTERPRISE_API_TOKEN=<your-API-token>
{{</ code-toggle >}}

3. Run the following command in your terminal to deploy and start your simulation:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:enterpriseStart
Windows: mvnw.cmd gatling:enterpriseStart
{{</ code-toggle >}}

Watch the Simulation deploy automatically and generate real-time reports.

## Run the simulation locally for debugging

The open-source version of Gatling allows you to run simulations locally, generating load from your computer. Running a new or modified simulation locally is often useful to ensure it works before launching it on Gatling Enterprise Cloud.
Using the terminal, you can launch your test with the following command in the project root directory:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:test
Windows: mvnw.cmd gatling:test
{{</ code-toggle >}}

The Gatling interactive CLI starts and asks a series of questions. Answer the questions as follows: 

1. Press 1 and then enter to select `Run the Simulation locally`.
2. Press 0 and then enter to select `RecordedSimulation`
3. (optional) Enter a run description.
4. Press enter.

The simulation should start on your local machine, with the progress displayed in your terminal. When the test has finished, there is an HTML link in the terminal that you can use to access the static report.

## Keep learning

Now that you have completed the Introduction to scripting and Introduction to the Recorder tutorials, you have a solid foundation of Gatling and load testing knowlege. We strongly recommend you complete the Writing realistic tests tutorial to learn the essential skills for writing clean and concise tests. 

 - [Gatling Academy](https://academy.gatling.io/) 
 - [Writing realistic tests]({{< ref "advanced" >}})
