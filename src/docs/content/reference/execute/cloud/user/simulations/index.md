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

If you don't have any simulations configured yet and don't know where to start, follow the helper that guides you through the creation process:

{{< img src="helper.png" alt="Helper" >}}

- Build your simulation in-app: leads you to the [in-app simulation builder]({{<ref "/tutorials/quickstart" >}})
- Build a test as code project: leads you to the [Get-started guide]({{<ref "#getting-started-guide" >}}) explaining how you can build a package from sources, upload it, and execute it using Gatling Enterprise

## Getting started guide

{{< img src="getting-started.png" alt="Getting started" >}}

Depending on which programming language you are using, follow the steps to create your first package.
Once this is done, you're ready to upload it and [create you first test-as-code simulation]({{<ref "#creating-a-test-as-code-simulation" >}}).

{{<alert tip >}}
If you need to find this guide later on, you can navigate to it using our [simulation creation modal]({{<ref "#creating-a-test-as-code-simulation" >}})

{{< img src="modal-getting-started.png" alt="Modal getting started" >}}
{{</alert>}}


## Creating a test-as-code simulation

Use the **Create a simulation** button (either coming from the [Simulations page]({{<ref "#managing-simulations" >}}) or the [Getting started guide]({{<ref "#getting-started-guide" >}})) to open the simulation creation modal:

{{< img src="simulation-creation-modal.png" alt="Simulation creation modal" >}}

From here, you can either import a package, select an existing one, or go to the [in-app simulation builder]({{<ref "/tutorials/quickstart" >}}).

Once you've selected your package, click the **Create** button to configure your simulation

{{< alert warning >}}
Gatling Enterprise has a hard limit for run durations of 7 days and will stop any test running for longer than that.
This limit exists for both performance reasons (to avoid data growing too large to be presented in the dashboard) and security
reasons (to avoid a forgotten test running forever).
{{< /alert >}}

### Step 1: name, package and simulation

In this step, define the simulation's general parameters.

{{< img src="step1.png" alt="Create simulation - Step 1" >}}

- **Name**: the name that will appear on the simulations table.
- **Package**: the actual package the simulation will run.
- **Simulation**: the simulation to run in this package.

### Step 2: Locations configuration

In this step, configure the Gatling Enterprise load generator locations.

You can either use the managed locations provided by Gatling Enterprise, use your own [private locations]({{< ref "../../../install/cloud" >}}), or [dedicated IP addresses]({{< ref "dedicated-ips" >}}) for your load generators.

{{< alert info >}}
It is not currently possible to mix managed, private locations and dedicated IPs in the same simulation.
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

To get the best results from your simulation you should select the load generator locations that best match your user base.

{{< img src="step2.png" alt="Create simulation - Step 2" >}}

- **Location**: defines the locations to be used when initiating the Gatling Enterprise load generators.
- **Number of load generators**: number of load generators for this location.
- **Weight distribution**: by default, every load generator will produce the same load. If enabled, you must set the weight in % for each location (e.g. the first location does 20% of the requests, and the second does 80%). The sum of all weights must be 100%.

You can add several locations with different numbers of load generators to run your simulation.

After this step, you can save the simulation, or continue with optional configurations.

### Step 3: Load Generator Parameters {{% badge info "Optional" /%}} {#step-3-load-generators-parameters}

This step allows you to define the Java system properties or JS parameters and environment variables used when running this particular simulation. Properties/variables entered here will add to the defaults, unless you choose to ignore the defaults. If you keep the defaults, and you add a property/variable with the same key as one from the defaults, the simulation's value will be used (it overrides the default). See the [Default Load Generator Parameters]({{<ref "#default-load-generator-parameters">}}) section for more information.

{{< img src="step3.png" alt="Create simulation - Step 3" >}}

{{< alert tip >}}
JVM options, Java System Properties or JS parameters and environment variables will be saved in a snapshot that will be available in the run. This information will be visible by anyone who has read access.
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

You can configure ramp-up and ramp-down time windows to be excluded from computed assertions. This is typically useful when you know that at the beginning of your test run you expect higher response times than when your system is warm (JIT compiler has kicked in, autoscaling has done its work, caches are filled, etc.) and don’t want them the warm-up time to cause your assertions to fail.

{{< img src="step4.png" alt="Create simulation - Step 4" >}}

- **Ramp Up**: the number of seconds you want to exclude at the beginning of the run.
- **Ramp Down**: the number of seconds you want to exclude at the end of the run.

{{< alert tip >}}
Ramp up/down parameters will only be applied if the run duration is longer than the sum of the two.
{{< /alert >}}

{{< alert tip >}}
Ramp up/down parameters can also be specified with the [public API]({{< ref "/reference/execute/cloud/user/api/" >}}) and the [package descriptor]({{< ref "/reference/execute/cloud/user/configuration-as-code/" >}}). 
{{< /alert >}}

## Simulation execution

Using the [simulations table]({{<ref "#managing-simulations" >}}), you can control your simulation execution.

Start or stop your run using the respective {{< icon play >}} or {{< icon stop >}} buttons.

{{< img src="start-table.png" alt="Start your run" >}}

### Lifecycle

Once you have created and started a simulation, your run will go through the following life cycle:

- **Building**: download the simulation package and prepare the hosts.
- **Deploying**: deploy the simulation to the load generators.
- **Injecting**: the simulation is active and can be viewed in Reports. 

{{< img src="building.png" alt="Injecting" >}}

### Logs

By clicking on the **Last run** column of your simulation, you are redirected to the build logs for your run. There is a limit of 1,000 logs for a run.

{{< img src="go-to-logs.png" alt="Go to your logs" >}}

Viewing the Logs can also help determine why a run failed and what errors you need to correct to successfully run your simulation.

{{< img src="logs.png" alt="Logs" >}}

### Useful tips

- You can edit, copy the ID, duplicate and delete the simulation by clicking on the kebab menu icon
- You can search a simulation by its name, or its team name
- You can sort the simulations by any column
- A **Delete** button will appear on the action bar when you select a simulation, you will be able to delete all the selected simulations
- You can edit [default load generator parameters]({{< ref "#default-load-generator-parameters" >}}) using the corresponding button at the top of the page

Be aware that deleting a simulation will delete all the associated runs.

### Default load generator parameters

Default load generator parameters contain every Java system property or JS parameter and environment variable used in your simulations by default.
Editing these properties propagates to all simulations. You can access the form by clicking the button in the top right corner of the [simulation page]({{<ref "#managing-simulations" >}}).

{{< img src="default-load-generator-properties.png" alt="Properties" >}}

If you want specific properties for a simulation, you can ignore the default properties by unchecking the `Default properties` box when creating or editing the simulation:

{{< img src="override-load-generator-properties.png" alt="Override" >}}