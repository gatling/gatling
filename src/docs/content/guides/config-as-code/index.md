---
title: Configuration-as-code
seotitle: Configure and deploy tests to Gatling Enterprise Cloud in your CI chain
description: Learn how to configure and deploy tests to Gatling Enterprise Cloud using a package descriptor conf file.
lead: Learn how to configure and deploy tests to Gatling Enterprise Cloud using a package descriptor conf file.
---

This guide helps you to configure your packages and simulations in your local developer environment, 
then deploy and start your simulations with a single command. 

It assumes that you have a working Gatling project. 

If you are just starting out, 
we recommend trying the [Intro to scripting]({{< ref "/tutorials/scripting-intro" >}}) tutorial before using this guide.

## Prerequisites 

- A Gatling Project _(Demo projects: [Maven](https://github.com/gatling/gatling-maven-plugin-demo-java), [Gradle](https://github.com/gatling/gatling-gradle-plugin-demo-java), [sbt](https://github.com/gatling/gatling-sbt-plugin-demo))_
- A Gatling Enterprise Cloud account [sign up for a free trial](https://auth.gatling.io/auth/realms/gatling/protocol/openid-connect/registrations?client_id=gatling-enterprise-cloud-public&response_type=code&scope=openid&redirect_uri=https%3A%2F%2Fcloud.gatling.io%2Fr%2Fgatling)
- An [API token]({{< ref "/reference/execute/cloud/admin/api-tokens" >}}) with the **`Configure`** permission

## Configure the package descriptor


The package descriptor is a `conf` file located `/.gatling/package.conf`. 

The complete set of configuration options can be found in the [Configuration-as-code reference documentation]({{< ref "/reference/execute/cloud/user/configuration-as-code" >}}). 

This guide will demonstrate how to configure the:
- package name,
- package ID,
- simulation name,
- simulation ID,
- the `default` block.   

{{< alert tip >}}
All of the configuration properties are optional. 
If you want to upload your package and simulation(s) with the default settings, 
skip to the [Deploy to Gatling Enterprise Cloud](#deploy-to-gatling-enterprise-cloud) section.
{{< /alert >}}

### Create the `conf` file

To create the package descriptor file:
1. Add a directory `.gatling` at your project root.
2. Add a file named `package.conf` to the `.gatling` directory.
3. Add the following code to the `package.conf` file:

```hocon
gatling.enterprise.package {

}
```

### Define the package 

Once you have created the `package.conf` file:
- name the package,
- assign the package to a team (this team must exist in the Gatling Enterprise Cloud UI),
- add the `id` property, but leave it commented out for now.

```hocon
gatling.enterprise.package {
  # id = "00000000-0000-0000-0000-000000000000"
  name = "My package name"
  team = "My team name"
}
```

### Define the simulation(s)

The next step is to add your simulations to the `package.conf` file.

Reminder: this step is optional, but once you add the `simulations` block, you must define the `classname` property. 

The classname is the simulation's fully qualified name, for example, `computerdatabase.ComputerDatabaseSimulation` 
if you followed the Intro to scripting tutorial. 

Each simulation also has a unique `id` property, which we will use later in the guide. 

Add the `simulations` array with one simulation object:
```hocon
gatling.enterprise.package {
  # id = "00000000-0000-0000-0000-000000000000"
  name = "My package name"
  team = "My team name"
  simulations = [
    {
      # id = "00000000-0000-0000-0000-000000000001"
      classname = "computerdatabase.ComputerDatabaseSimulation"
    }
  ]
}
```

### Deploy to Gatling Enterprise Cloud

At this point, making your initial package deployment is a good idea.

This allows you to work with the package and simulation IDs for keeping track of tests and avoid duplicate packages and 
simulations on Gatling Enterprise Cloud. Use the following procedure to make your initial deployment: 

1. Add the [API Token]({{< ref "reference/execute/cloud/admin/api-tokens" >}}) to your Gatling project.
  - [Gatling Plugin with Maven]({{< ref "reference/integrations/build-tools/maven-plugin/#prerequisites" >}})
  - [Gatling Plugin with Gradle]({{< ref "reference/integrations/build-tools/gradle-plugin/#prerequisites" >}})
  - [Gatling Plugin with sbt]({{< ref "reference/integrations/build-tools/sbt-plugin/#prerequisites" >}})
2. Deploy your package and simulation(s) with the following command:
  - [Maven]({{< ref "reference/integrations/build-tools/maven-plugin/#deploying-on-gatling-enterprise-cloud" >}}): `mvn gatling:enterpriseDeploy`
  - [Gradle]({{< ref "reference/integrations/build-tools/gradle-plugin/#deploying-on-gatling-enterprise-cloud" >}}): `gradle gatlingEnterpriseDeploy`
  - [sbt]({{< ref "reference/integrations/build-tools/sbt-plugin/#deploying-on-gatling-enterprise-cloud" >}}): `sbt Gatling/enterpriseDeploy`


The CLI deploys the package and simulation to Gatling Enterprise Cloud and returns the package ID and simulation ID in the terminal. 

### Add IDs to the `package.conf` file

After your first successful deployment, Gatling Enterprise Cloud will return the Package ID and the simulation ID to your terminal.

Copy and paste these values into the respective `id` fields in your `package.conf` file. 

After this step, you can change the package and simulation names without creating new packages and simulations on Gatling Enterprise Cloud. 

{{< alert warning >}}
If there's no ID, the deployment is based on the **name** and the **team** of the package and simulations.

This mean that if one of them change, it will result in a new package and simulations being deployed.
{{< /alert >}}

## Set up reusable simulation properties with the `default` object

Setting up a `default` object allows you to deploy your simulations with consistent settings and reduce the amount of configuration on each simulation. An example use case is to specify that your simulations are always run distributed between 2 regions with 1 load generator in each location. The virtual users have a 60%/40% distribution. 

To setup the `default` object, add the following code to your `package.conf` file. Note that this example does not have the simulations array, meaning it uses the default simulation settings.

```hocon
gatling.enterprise.package {
  # id = "00000000-0000-0000-0000-000000000000"
  name = "My package name"
  team = "My team name" # or ID with team = "00000000-0000-0000-0000-000000000000"
  default {
    simulation {
      locations = [
        {
          name: "Europe - Paris"
          size: 1
          weight: 60
        },
        {
          name: "US East - N. Virginia"
          size: 1
          weight: 40
        }
      ]	    
    }
  }
}
```
Learn more about configuring your packages and simulations in the [configruation as code reference documentation]({{< ref "/reference/execute/cloud/user/configuration-as-code" >}}).
