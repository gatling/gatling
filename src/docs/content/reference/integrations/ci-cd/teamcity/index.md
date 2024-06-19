---
menutitle: TeamCity
title: TeamCity Plugin
seotitle: TeamCity Plugin for Gatling Enterprise
description: Learn how to configure the Gatling Enterprise TeamCity plugin and run your simulations.
lead: Run your Gatling Enterprise simulations from your TeamCity CI.
badge:
  type: enterprise
  label: Enterprise
aliases:
  - /reference/extensions/ci-cd/teamcity
date: 2021-03-08T12:50:20+00:00
lastmod: 2023-10-11T10:10:00+00:00
---

## Purpose of this plugin

This plugin allows you to start a Gatling Enterprise simulation directly from your TeamCity platform. This plugin links a TeamCity plan with one and only one Gatling Enterprise simulation.

This plugin doesn't create a new Gatling Enterprise simulation, you have to create it manually before.

## Installation

To download the plugin, you need to get the ZIP file by clicking on the following button:

{{< button title="Download Teamcity Plugin" >}}
https://downloads.gatling.io/releases/frontline-teamcity-plugin/{{< var ciPluginsVersion >}}/frontline-teamcity-plugin-{{< var ciPluginsVersion >}}.zip
{{< /button >}}

You need to be connected as an administrator of your TeamCity application to install it. Navigate to **Administration**, **Plugins**, **Upload plugin zip**, and choose the downloaded zip file.

{{< img src="upload-plugin.png" alt="Upload plugin" >}}

Once the plugin is uploaded, you need to enable it.

## Configuration

The plugin needs a global configuration. Go to **Administration**, then **frontline-teamcity-plugin**:

- the **Gatling Enterprise Address** is the address of Gatling Enterprise Cloud (https://cloud.gatling.io).
- the **Gatling Enterprise API Token** is needed to authenticate to Gatling Enterprise Cloud:
  - the [API token]({{< ref "../../execute/cloud/admin/api-tokens" >}}) needs the **Start** permission.

{{< img src="administration.png" alt="" >}}

## Plan set-up

Add a new build step called **Gatling Enterprise Launcher**. Choose in the Simulation list the simulation you want to monitor. You need to configure the global properties of the plugin, and create at least a simulation on Gatling Enterprise to do this step.

{{< img src="configuration.png" alt="Configuration" >}}

This step regularly prints a summary of the run's current status to the build logs. By default, the summary is printed every 5 seconds the first 12 times (i.e. for the first 60 seconds), and then every 60 seconds. You can configure this behavior (or disable it completely) in the step configuration.

### JUnit reporting

You can display the results of the Gatling Enterprise assertions as a JUnit Test.

Add a new build feature called **XML report processing**. Choose **Ant JUnit** as report type, and enter in the **Monitoring rules** input the following line:

`gatlingFrontLineJunitResults/*.xml`

{{< img src="junit.png" alt="JUnit" >}}

## Usage

A new Gatling Enterprise simulation will be started every time the job is run. Check the Console Log to see the advancement of the simulation. If the simulation ran successfully, it will look like the following:

{{< img src="log.png" alt="Console Log" >}}

When the job run is finished, you will be able to see on the **Gatling Enterprise Results** tab, the summary of the Gatling Enterprise simulation.

{{< img src="display-results.png" alt="Display results" >}}
