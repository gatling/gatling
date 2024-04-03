---
title: Gatling Deployment Descriptor with sbt
description: Setup tutorial
lead: Set up your project for automatic configuration of your simulations in Gatling Enterprise.
date: 2024-01-31T18:30:00+02:00
lastmod: 2024-02-01T11:00:00+00:00
private: true
---

{{< alert tip >}}
If you need a package to test this, you can use the following one: [`gatling-sbt-plugin-demo`](https://github.com/gatling/gatling-sbt-plugin-demo)
{{< /alert >}}
# Configuration

## 1. Edit your `plugins.sbt` file

* In `addSbtPlugin`, make sure you use the following version:

  ```scala
  addSbtPlugin("io.gatling" % "gatling-sbt" % "4.8.0")
  ```

## 2. Create your Gatling configuration

Add a new directory at the module's `baseDir`: `.gatling`.

Create a new file `.gatling/package.conf`.

```console
module-baseDir
├── .gatling/
│   └── package.conf
└── src/
    ├── main/
    └── test/
```

This file is in [`HOCON` format (Human-Optimized Config Object Notation)](https://github.com/lightbend/config/blob/main/HOCON.md), which means you can also write `JSON` if you prefer.

{{< alert tip >}}
Managed location name must be an available region (see list in example file below).

Private location name must be an id configured in your control plane ([see documentation]({{< ref "/reference/install/cloud/private-locations/introduction#configuration" >}})).
{{< /alert >}}

{{<alert warning>}}
Mixing managed and private locations is not supported by Gatling Enterprise. Ensure all your locations are either managed or private for a given simulation.
{{< /alert >}}

```bash
gatling.enterprise {
  # The name of the package (Mandatory)
  name = "My package name"
  # The team UUID (Mandatory)
  # You can copy it from Organization / Teams / More / Copy ID
  team = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"

  # Simulations based on the package (Optional)
  # List of objects [{...}, {...}, {...}]
  simulations = [{
    # The name of the simulation (Mandatory)
    name = "My simulation name"

    # The FQDN of the class that extends Simulation (Mandatory)
    classname = "com.example.MySimulation"

    ## Using or not dedicated IPs (optional)
    ## Only available if dedicated IPs are configured in your organization
    # useDedicatedIps = false

    # Locations configuration (mandatory)
    # Map of objects "key1" {...}, "key2" {...}, "key3"{...}
    locations {
      # Mixing managed and private locations is not supported.
      # Ensure all your locations are either managed or private for a given simulation.

      # Managed location:
      # Configuration by region
      # Available regions:
      #  - "US West - N. California"
      #  - "Europe - Paris"
      #  - "Europe - Dublin"
      #  - "AP Pacific - Mumbai"
      #  - "US West - Oregon"
      #  - "AP SouthEast - Sydney"
      #  - "US East - N. Virginia"
      #  - "SA East - São Paulo"
      #  - "AP - Tokyo"
      #  - "AP - Hong Kong"

      "Europe - Paris" {
        # Amount of load generators in this region (mandatory)
        size = 1
        ## Weight of this region (optional)
        ## (total sum of all locations MUST be 100 for a simulation)
        # weight = 100
      }

      ## Private location:
      ## The name must be an id configured in your control plane
      
      # "prl_example" {
      #   # Amount of load generators in this location (mandatory)
      #   size = 1
      #   ## Weight of this location (optional)
      #   ## (total sum of all locations MUST be 100 for a simulation)
      #   # weight = 100
      # }
    }

    # Configure specific parameters for this simulation (optional)
    parameters {
      ## Ignore parameters configured at the organization level (optional)
      # ignoreDefaults = false

      ## Java System properties (optional)
      ## key = value
      # systemProperties {
      #   "com.example.prop" = "value frop system prop"
      # }

      ## Environment variables (optional)
      ## key = value
      # environmentVariables {
      #   MY_SIMULATION_ENV_VAR = "value from environment"
      # }
    }

    # Meaningful time window (optional)
    timeWindow {
      ## Time to exclude from assertions at the beginning of the run (optional)
      # rampUp = 0

      ## Time to exclude from assertions at the end of the run (optional)
      # rampDown = 0
    }
  }]
}
```

## 3. Upload your project to Gatling Enterprise

(package configuration, package upload, simulations configuration)

{{< alert tip >}}
Ensure you referenced the API token for your [sbt plugin]({{< ref "/reference/extensions/build-tools/sbt-plugin#api-tokens" >}}).

To create an API token: [documentation]({{< ref "/reference/execute/cloud/admin/api-tokens" >}})
(must have the **Configure** role).
{{< /alert >}}

Use the following command when using sbt:

`sbt Gatling / enterpriseUpload`

A successful upload results in the following (here with demo project):

```bash
sbt Gatling / enterpriseUpload
[info] Generating Gatling Enterprise package <module-baseDir>/gatling-sbt-plugin-demo/target/gatling/gatling-sbt-plugin-demo-gatling-enterprise-<version>.jar
[info] Package configuration file detected, applying it.
[info] Package id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
[info] Package uploaded
[success] Successfully upload package
[success] Total time: x s, completed MMM dd, yyyy, hh:mm:ss
```

If you encounter any problems, please contact our support team.

# Result

Your package and configured simulation are now uploaded to Gatling Enterprise 👍.

Go to [Gatling Enterprise](https://cloud.gatling.io/) for further use. 
