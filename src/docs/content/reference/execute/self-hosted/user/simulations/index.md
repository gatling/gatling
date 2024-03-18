---
title: Simulations
seotitle: Configure simulations in Gatling Enterprise Self-Hosted
description: Learn how to configure and navigate through simulations in Gatling Enterprise Self-Hosted.
lead: Navigate through simulations.
date: 2021-03-10T09:29:43-05:00
lastmod: 2023-03-09T17:00:00+00:00
---

## Managing Simulations

To access the Simulations section, click on **Simulations** in the navbar.

The Simulations view contains all the simulations you have configured, and the result of their last run.

{{< img src="simulations-table.png" alt="Simulation table" >}}

If you don't have any simulations configured yet and don't know how to start, you can download some Gatling Enterprise pre-configured projects by clicking on the "Download sample simulations" green button.

{{< img src="samples.png" alt="Samples" >}}

Those samples are ready to use maven, sbt and gradle projects with proper configuration for Gatling Enterprise. You can also download those samples with the download link in the Documentation section.

Back to the Simulations section, at the top, there is an action bar which allow several actions:

- Create a simulation
- Search by simulation or team name
- Edit global properties
- Delete selected simulations

{{< img src="action-bar.png" alt="Action bar" >}}

## Global Properties

Global properties contains every JVM options, Java System Properties and environment variables used by all of your simulations by default.
Editing those properties will be propagated to all the simulations.

If you don't want to use the default properties, check `Use custom global properties` and enter your own.

{{< img src="properties.png" alt="Properties" >}}

If you want specific properties for a simulation, you will be allowed to ignore those properties by checking the `Override Global Properties` box when creating or editing the simulation:

{{< img src="override.png" alt="Override" >}}

## Creating a simulation

{{< alert warning >}}
Gatling Enterprise has a hard run duration limit of 7 days and will stop any test running for longer than that.
This limit exists for both performance (data who grow too humongous to be presented in the dashboard) and security (forgotten test running forever) reasons.
{{< /alert >}}

In order to create a simulation click on the "Create" button in the simulations table. There are 6 steps to create a simulation, 3 of which are optional.

### Step 1: General

{{< img src="create-simulation1.png" alt="Create simulation - Step 1" >}}

- **Name**: the name that will appear on the simulations table.
- **Team**: the team which owns the simulation.
- **Class name**: the package and the name of your simulation scala class in the project that you want to start.

### Step 2: Build configuration

In this step, you'll describe which [repository]({{< ref "repositories" >}}) Gatling Enterprise will use, and how to use it.

{{< img src="create-simulation2a.png" alt="Create simulation - Step 2" >}}

- **Build type**: How you want to retrieve and build your simulation. You may choose to build from sources, download a binary from a Sonatype Nexus or JFrog Artifactory repository, or download a binary from an AWS S3 bucket.
- **Repository**: The [repository]({{< ref "repositories" >}}) you created previously

#### Option 1: Build from sources

In this step, Gatling Enterprise will download the sources from your repository, and compile them.

- **Build command**: the command to build your project. Three common commands are built-in for projects whose build tools configuration follow our installation guide:
  * `mvn clean package -DskipTests --quiet` for maven project
  * `sbt -J-Xss100M ;clean;test:assembly -batch --error` for sbt project
  * `gradle clean frontLineJar -quiet` for gradle project

  {{< alert warning >}}
  Please make sure that the tools you are using are installed and available on the Gatling Enterprise machine, for example: `mvn`,  `sbt`, `git`, and `ssh`.
  {{< /alert >}}

  In addition, two wrapper commands are built-in, if you prefer to use a build tool wrapper script included in your project rather than a version of the build tool installed on the Gatling Enterprise machine. The commands are otherwise the same as the regular commands for the same build tools.
  * Use the Maven Wrapper command if your project includes an `mvnw` script (see [the official Maven documentation](https://maven.apache.org/wrapper/) to configure your project)
  * Use the Gradle Wrapper command if your project includes a `gradlew` script (see [the official Gradle documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html) to configure your project)


You can provide optional settings if you toggle **Advanced build configuration**.

- **Relative path**: the path to your simulation project in your repository (eg the Gatling simulation is not at the root of your git)
- **Environment variables**: the environment variables to be used along the build command. You can add as many environment variables as you want
- **Git Branch or Tag**: if you're using a git repository, you may specify another branch or tag than the one configured in the repository configuration

#### Option 2: Download binary from repository

In this step, you'll describe how Gatling Enterprise will download a jar deployed in a previously added repository.
This jar must have been built with the same maven/sbt/gradle configuration as described in the Developer section in this guide.

{{< img src="create-simulation2b.png" alt="Create simulation - Step 2 - Binary" >}}

- **Artifact Maven coordinates**: the maven coordinates of the desired artifact.

The following version markers can be used:

* `latest.integration`: latest version with snapshots included
* `latest.release`: same with snapshots excluded

#### Option 3: Download binary from AWS S3

In this step, you'll describe how Gatling Enterprise will download a jar deployed in an AWS S3 bucket.
This jar must have been built with the same maven/sbt/gradle configuration as described in the Developer section in this guide.

{{< img src="create-simulation2c.png" alt="Create simulation - Step 2 - S3" >}}

- **Key**: the key for the jar you want to download

### Step 3: Pools configuration

In this step, you'll configure the pools used for the Gatling Enterprise injectors.

{{< img src="create-simulation3.png" alt="Create simulation - Step 3" >}}

- **Weight distribution**: on even, every injector will produce the same load. On custom, you have to set the weight in % of each pool (eg the first pool does 20% of the requests, and the second does 80%). The sum of the weight should be 100%.
- **Pools**: defines the pools to be used when initiating the Gatling Enterprise injectors, see the section about [pools]({{< ref "../../../install/self-hosted/injectors/configuration/pools" >}}).
You can add many pools with a different number of hosts to run your simulation.
If you have more hosts than needed on your Pool, the hosts will be chosen randomly between all hosts available in this Pool.

After this step, you can save the simulation, or click on **More options** to access optional configuration.

### Step 4 & 5: JVM options & Injector parameters

These steps allow you to define JVM arguments, Java System Properties and environment variables used when running this particular simulation. You can choose to override the global properties.

{{< img src="create-simulation4.png" alt="Create simulation - Step 4" >}}
{{< img src="create-simulation5.png" alt="Create simulation - Step 5" >}}

{{< alert tip >}}
The JVM options, Java System Properties and environment variables will be saved in a snapshot that will be available in the run. This information will be visible by anyone who has read access.
You can exclude some properties from being copied if you prefix them with `sensitive.`, and environment variables if you prefix them with `SENSITIVE_`.
{{< /alert >}}

{{< alert tip >}}
You can configure the `gatling.enterprise.groupedDomains` System property to group connection stats from multiple subdomains and avoid memory issues when hitting a very large number of subdomains.
For example, setting this property as `.foo.com, .bar.com` will consolidate stats for `sub1.foo.com`, `sub2.foo.com`, `sub1.bar.com`, `sub2.bar.com` into `*****.foo.com` and `*****.bar.com`.
{{< /alert >}}

{{< alert tip >}}
Java System properties can be retrieved in your Gatling simulation with `System.getProperty("YOUR_PROPERTY_KEY")`.

Environment variables can be retrieved in your Gatling simulation with `System.getEnv("YOUR_ENV_VAR_KEY")`.
{{< /alert >}}

### Step 6: Time window

Configure some ramp up or ramp down time windows to be excluded when computing assertions. This is typically useful when you know that at the beginning of your test run you're going to expected higher response times than when your system is warm (JIT compiler has kicked in, autoscaling has done its work, caches are filled...) and don’t want them to cause your assertions to fail.

{{< img src="create-simulation6.png" alt="Create simulation - Step 6" >}}

- **Ramp Up**: the number of seconds you want to exclude at the beginning of the run.
- **Ramp Down**: the number of seconds you want to exclude at the end of the run.

{{< alert tip >}}
Ramps parameters will only be applied if the run duration is longer than the sum of the two.
{{< /alert >}}

## Simulations table

Now that you have created a simulation, you can start it by clicking on the {{< icon play >}} icon in the **Start** column of the table.

{{< img src="start.png" alt="Start" >}}

A run have the following life cycle:

- **Building**: in which it will download the simulation artifact and prepare the hosts
- **Deploying**: in which it will deploy the simulation to run on all the hosts
- **Injecting**: in which the simulation is running and viewable from the Reports

{{< img src="injecting.png" alt="Injecting" >}}

### Logs

By clicking on the {{< icon file-alt >}} icon in the **Build Start** column, Gatling Enterprise will display the build logs of the simulation. There is a limit of 1000 logs for a run.

{{< img src="logs.png" alt="Logs" >}}

You can click on the {{< icon search >}} icon next to the status (if there is one) to display the assertions of the run.
Assertions are the assumptions made at the beginning of the simulation to be verified at the end:

{{< img src="assertions.png" alt="Assertions" >}}

## Useful tips

- You can edit the simulation by clicking on the {{< icon pencil-alt >}} icon next to his name
- You can search a simulation by his name, or its team name
- You can sort the simulations by any column except the **Start** one
- A **Delete** button will appear on the action bar when you select a simulation, you will be able to delete all the selected simulations
- When a simulation is running, you can stop the run by clicking on the Abort button
- You can copy a simulation ID by clicking on the {{< icon clipboard >}} icon next to his name

Be aware that deleting a simulation will delete all the associated runs.
