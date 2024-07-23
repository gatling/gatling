---
title: Package Generation
seotitle: Generate packages for Gatling Enterprise Cloud
description: Learn how to package Gatling simulations for Gatling Enterprise Cloud from the Gatling zip bundle, or from a Maven, sbt, or Gradle project.
lead: Generate a package from your Gatling bundle or with a Maven, sbt, or Gradle project.
aliases:
 - artifact_gen
date: 2021-03-08T12:50:32+00:00
lastmod: 2021-11-22T16:00:00+00:00
---

## Generating Packages for Gatling Enterprise

Gatling Enterprise deploys packages containing your compiled Simulations and resources. Those packages have to be generated
upstream, using one of the methods below, before you can run them with Gatling Enterprise.

Gatling Enterprise is compatible with Gatling version from 3.5 to {{< var gatlingVersion >}} included, however, these instructions are aligned with the new Maven-based bundle released in Gatling 3.11.

{{< alert tip >}}
If you go to the [Simulations page]({{< ref "simulations" >}}) in the Gatling Enterprise Cloud app, you can click on
"Sample simulations" to download sample projects for all the options below.
{{< /alert >}}

### Gatling bundle

Once you have created a simulation you want to upload, you can use the `enterpriseDeploy` command to upload your package and simulation with the default configuration. To customize your package and simulation configuration, see the [Configuration as code documentation]({{< ref "configuration-as-code" >}}). 

To use the `enterpriseDeploy` command:

1. Create an [API token]({{< ref "/reference/execute/cloud/admin/api-tokens" >}}) in Gatling Enterprise. 
2. Set the API token in your local environment using either:
    - the `GATLING_ENTERPRISE_API_TOKEN` environment variable,
    - the `gatling.enterprise.apiToken` [Java System property](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html).
3. Run the `enterpriseDeploy` command:

{{< platform-toggle >}}
Linux/MacOS: ./mvnw gatling:enterpriseDeploy
Windows: mvnw.cmd gatling:enterpriseDeploy
{{</ platform-toggle >}}

{{< alert info >}}
Learn how to work with environment variables, Java system properties, and JavaScript parameters in the
[Configuration documentation]({{< ref "/reference/script/core/configuration#manage-configuration-values" >}}). 
{{< /alert >}}

Alternatively, you can package your simulation and then upload it using the Gatling Enterprise UI. To package and upload your simulation:

1. Run the command `enterprisePackage` in your local terminal:

    {{< platform-toggle >}}
    Linux/MacOS: ./mvnw gatling:enterprisePackage
    Windows: mvnw.cmd gatling:enterprisePackage
    {{</ platform-toggle >}}

2. Log in to Gatling Enterprise and go to the **Packages** page from the left-side navigation menu.
3. Click **+ Create**. 
4. Use the upload modal to name the package and assign it to a team.
5. Upload the `.jar` file created in step 1 from your project's `target` folder.
6. Click **Save**.

Finally, you can get the list of all the available options with the `help` command:

{{< platform-toggle >}}
Linux/MacOS: ./mvnw gatling:help
Windows: mvnw.cmd gatling:help
{{</ platform-toggle >}}

{{< alert warning >}}
These commands are only available since Gatling `3.11`. If you're using an older version, you'll have to upgrade.
{{< /alert >}}

### Maven, Gradle or sbt project

To set up you project, and to learn how to use you preferred build tool to upload your simulations to Gatling Enterprise
Cloud, please refer to the documentation pages of the respective plugins:

- [Gatling plugin for Maven]({{< ref "../../../integrations/build-tools/maven-plugin" >}}) (for Java, Kotlin and Scala)
- [Gatling plugin for Gradle]({{< ref "../../../integrations/build-tools/gradle-plugin" >}}) (for Java, Kotlin and Scala)
- [Gatling plugin for sbt]({{< ref "../../../integrations/build-tools/sbt-plugin" >}}) (for Scala)

{{< alert warning >}}
The Gatling build plugins now include everything you need to work with Gatling Enterprise. Previous versions required an
additional plugin to work with Gatling Enterprise. If you have one of the old "FrontLine" plugins
(`io.gatling.frontline:frontline-maven-plugin`, `io.gatling.frontline:frontline-gradle-plugin` or
`io.gatling.frontline:sbt-frontline`) in your build, we recommend removing it and updating to the latest version of the
Gatling plugin instead.
{{< /alert >}}

## Note on Feeders

A typical mistake with Gatling and Gatling Enterprise is to rely on having an exploded Maven/Gradle/sbt project structure, and to try to load files from the project filesystem.

This filesystem structure will not be accessible once your project has been packaged and deployed to Gatling Enterprise.

If your feeder files are packaged with your test sources, you must resolve them from the classpath. This will work both
when you run simulations locally and when you deploy them to Gatling Enterprise.

```scala
// incorrect
val feeder = csv("src/test/resources/foo.csv")

// correct
val feeder = csv("foo.csv")
```

## Load Sharding

Injection rates and throttling rates are automatically distributed amongst nodes.

However, Feeders data is not automatically sharded, as it might not be the desired behavior.

If you want data to be unique cluster-wide, you have to explicitly tell Gatling to shard the data, e.g.:

```scala
val feeder = csv("foo.csv").shard
```

Assuming the CSV file contains 1000 entries, and you run your simulation on 3 Gatling nodes, the entries will be distributed as follows:

- First node will access the first 333 entries
- Second node will access the next 333 entries
- Third node will access the last 334 entries

{{< alert tip >}}
`shard` is available in Gatling OSS DSL but is a noop there. It's only effective when running tests with Gatling Enterprise.
{{< /alert >}}

## Resolving Load Generator Location in Simulation

When running a distributed test from multiple locations, you could be interested in knowing where a given load generator is deployed in order to trigger specific behaviors depending on the location.

For example, you might want to hit `https://example.fr` if the load generator is deployed in the `Europe - Paris` location, and `https://example.com` otherwise.

In your simulation code, you can resolve the name of the location in which the load generator running the code is deployed:

```scala
val locationName = System.getProperty("gatling.enterprise.poolName") // pool is the former name of location
val baseUrl = if (locationName == "Europe - Paris") "https://example.fr" else "https://example.com"
```

{{< alert tip >}}
This System property is only defined when deploying with Gatling Enterprise.
It is not defined when running locally with any Gatling OSS launcher.
{{< /alert >}}
