---
title: Simulations
seotitle: Configure simulations in Gatling Enterprise Cloud
description: Learn how to configure and navigate through simulations in Gatling Enterprise Cloud.
lead: Navigate through simulations.
date: 2021-03-10T14:29:43+00:00
lastmod: 2021-08-05T13:13:30+00:00
---

## Managing Simulations

To access the Simulations section, click on **Simulations** in the navigation bar.

The Simulations view contains all the simulations configured by your organization and the results of their last run.

{{< img src="simulations-table.png" alt="Simulation table" >}}

If you don't have any simulations configured yet and don't know where to start, you can download some Gatling Enterprise pre-configured projects by clicking on the **Sample simulations** button at the top right corner of the page.

{{< img src="samples-bar.png" alt="Samples" >}}

Samples are distributed under:
- **Scala** with [**Maven**](https://github.com/gatling/gatling-maven-plugin-demo-scala), [**Gradle**](https://github.com/gatling/gatling-gradle-plugin-demo-scala), [**sbt**](https://github.com/gatling/gatling-sbt-plugin-demo) and [**Gatling Enterprise Bundle**](https://gatling.io/products/)
- **Java** with [**Maven**](https://github.com/gatling/gatling-maven-plugin-demo-java), [**Gradle**](https://github.com/gatling/gatling-gradle-plugin-demo-java) and [**Gatling Enterprise Bundle**](https://gatling.io/products/)
- **Kotlin** with [**Maven**](https://github.com/gatling/gatling-maven-plugin-demo-kotlin) and [**Gradle**](https://github.com/gatling/gatling-gradle-plugin-demo-kotlin)
- [**Javascript** and **Typescript**](https://github.com/gatling/gatling-js-demo)

{{< img src="samples.png" alt="Samples" >}}

Back to the Simulations section, at the top, there is an action bar which allows several actions:

- Filter the simulations by name, or team name
- [Create a simulation]({{< ref "#creating-a-simulation" >}})
- Edit [default load generator parameters]({{< ref "#default-load-generator-parameters" >}})
- Delete selected simulations

{{< img src="action-bar.png" alt="Action bar" >}}

## Default Load Generator Parameters

Default load generator parameters contains every Java system properties and environment variables used by all of your simulations by default.
Editing those properties will be propagated to all the simulations. You can access the form by clicking the button in the top right corner of the page.

If you want to define such properties, check `Enable default properties`.

{{< img src="default-load-generator-properties.png" alt="Properties" >}}

If you want specific properties for a simulation, you will be allowed to ignore those properties by checking the `Ignore defaults` box when creating or editing the simulation:

{{< img src="override-load-generator-properties.png" alt="Override" >}}

## Creating a simulation

In order to create a simulation click on the "Create" button in the simulations table. There are 4 steps to create a simulation, 2 of which are optional.

{{< alert warning >}}
Gatling Enterprise has a hard limit for run durations of 7 days and will stop any test running for longer than that.
This limit exists for both performance reasons (to avoid data growing too large to be presented in the dashboard) and security
reasons (to avoid a forgotten test running forever).
{{< /alert >}}

### Step 1: General

In this step, you will define the simulation's general parameters.

{{< img src="create-simulation-general.png" alt="Create simulation - Step 1" >}}

- **Name**: the name that will appear on the simulations table.
- **Team**: the team which owns the simulation.

#### Package and Class name

If you already have created the package you want to use, you can select it via the **Package** dropdown menu (it must belong to the selected team).

If you did not create your package before, you can click the `Create a new package` button. You will then be prompted with the [package creation form]({{< ref "package-conf/#creation" >}}).

Once your package is selected you will be able to select the **Class name**, the simulation's fully qualified name, detected in the selected package.

### Step 2: Locations configuration

In this step, you'll configure the locations used for the Gatling Enterprise load generators.

You can either use the managed locations provided by Gatling Enterprise Cloud, or use your own [private locations]({{< ref "../../../install/cloud" >}})

{{< alert info >}}
It is not currently possible to mix managed and private locations in the same simulation.
{{< /alert >}}

Managed location load generators have the following specifications:

- 4 cores
- 8GB of RAM
- bandwidth up to 10 Gbit/s

Gatling Enterprise managed locations are available in the following regions:

- AP Pacific (Hong kong)
- AP Pacific (Tokyo)
- AP Pacific (Mumbai)
- AP SouthEast (Sydney)
- Europe (Dublin)
- Europe (Paris)
- SA East (São Paulo)
- US East (N. Virginia)
- US West (N. California)
- US West (Oregon)

If you want to use private locations, please refer to the [specific documentation]({{< ref "../../../install/cloud" >}}).

In order to get the best results from your simulation you should select the load generators that best represent your user base.

{{< img src="create-simulation-locations.png" alt="Create simulation - Step 2" >}}

- **Location**: defines the locations to be used when initiating the Gatling Enterprise load generators.
- **Number of load generators**: number of load generators for this location.
- **Weight distribution**: by default, every load generator will produce the same load. If enabled, you must set the weight in % for each location (e.g. the first location does 20% of the requests, and the second does 80%). The sum of all weights must be 100%.
- **Dedicated IP Addresses**: Check if you want to enable [dedicated IP addresses]({{< ref "dedicated-ips" >}}) for your load generators. Only available for public locations.

You can add several locations with different numbers of load generators to run your simulation.

After this step, you can already save the simulation, or continue with optional configurations.

### Step 3: Load Generator Parameters {{% badge info "Optional" /%}} {#step-3-load-generators-parameters}

This step allows you to define the Java system properties and environment variables used when running this particular simulation. Properties/variables entered here will add to the defaults, unless you choose to ignore the defaults. If you keep the defaults, and you add a property/variable with the same key as one from the defaults, the simulation's value will be used (it overrides the default).

{{< img src="create-simulation-load-generator-parameters.png" alt="Create simulation - Step 3" >}}

{{< alert tip >}}
JVM options, Java System Properties and environment variables will be saved in a snapshot that will be available in the run. This information will be visible by anyone who has read access.
You can exclude some system properties from being copied if you prefix them with `sensitive.`, and environment variables if you prefix them with `SENSITIVE_`.
{{< /alert >}}

{{< alert tip >}}
You can configure the `gatling.enterprise.groupedDomains` Java System property to group connection stats from multiple subdomains and avoid memory issues when hitting a very large number of subdomains.

For example, setting this property as `.foo.com, .bar.com` will consolidate stats for `sub1.foo.com`, `sub2.foo.com`, `sub1.bar.com`, `sub2.bar.com` into `*****.foo.com` and `*****.bar.com`.
{{< /alert >}}

{{< alert tip >}}
System properties can be retrieved in your Gatling simulation with `System.getProperty("YOUR_PROPERTY_KEY")`.

Environment variables can be retrieved in your Gatling simulation with `System.getEnv("YOUR_ENV_VAR_KEY")`.
{{< /alert >}}

### Step 4: Time window {{% badge info "Optional" /%}} {#step-4-time-window}

You can configure some ramp up/down time windows to be excluded when computing assertions. This is typically useful when you know that at the beginning of your test run you're going to expect higher response times than when your system is warm (JIT compiler has kicked in, autoscaling has done its work, caches are filled...) and don’t want them to cause your assertions to fail.

{{< img src="create-simulation-timewindow.png" alt="Create simulation - Step 4" >}}

- **Ramp Up**: the number of seconds you want to exclude at the beginning of the run.
- **Ramp Down**: the number of seconds you want to exclude at the end of the run.

{{< alert tip >}}
Ramp up/down parameters will only be applied if the run duration is longer than the sum of the two.
{{< /alert >}}

{{< alert tip >}}
Ramp up/down parameters can also be specified with the [public API]({{< ref "/reference/execute/cloud/user/api/" >}}) and the [package descriptor]({{< ref "/reference/execute/cloud/user/configuration-as-code/" >}}). 
{{< /alert >}}

## Simulations table

Once you have created a simulation, you can start it by clicking on the {{< icon play >}} icon.

{{< img src="start.png" alt="Start" >}}

A run has the following life cycle:

- **Building**: in which it will download the simulation package and prepare the hosts.
- **Deploying**: in which it will deploy the simulation to run on all the load generators.
- **Injecting**: in which the simulation is running and can be viewed from the Reports. 

{{< img src="injecting.png" alt="Injecting" >}}

### Logs

By clicking on the second icon on last column, Gatling Enterprise will display the build logs of the simulation. There is a limit of 1000 logs for a run.

Viewing the Log can also be helpful in determining why a run failed and what errors you will need to correct to successfully run your simulation.

The logs can also be viewed in the Reports, while the simulation is building.

{{< img src="logs.png" alt="Logs" >}}

You can click on the third icon on last column to display the assertions of the run.
Assertions are the assumptions made at the beginning of the simulation to be verified at the end:

{{< img src="assertions.png" alt="Assertions" >}}

## Useful tips

- You can edit, copy the ID, duplicate and delete the simulation by clicking on the kebab menu icon
- You can search a simulation by its name, or its team name
- You can sort the simulations by any column
- A **Delete** button will appear on the action bar when you select a simulation, you will be able to delete all the selected simulations
- When a simulation is running, you can stop the ongoing run by clicking on the Stop button

Be aware that deleting a simulation will delete all the associated runs.
