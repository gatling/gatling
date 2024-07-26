---
menutitle: Enterprise Plugin
title: Enterprise Jenkins Plugin
seotitle: Jenkins Plugin for Gatling Enterprise
description: Learn how to configure the Gatling Enterprise Jenkins plugin and run your simulations.
lead: Run your Gatling Enterprise simulations from your Jenkins CI.
badge:
  type: enterprise
  label: Enterprise
aliases:
  - /reference/extensions/ci-cd/jenkins/enterprise
date: 2021-03-08T12:50:14+00:00
lastmod: 2023-10-11T10:10:00+00:00
---

## Purpose of this plugin

This plugin enables you to start a Gatling Enterprise simulation directly from your Jenkins platform. This plugin links a Jenkins job with one and only one Gatling Enterprise simulation.

This plugin doesn't create a new Gatling Enterprise simulation, you have to create it using the Gatling Enterprise Dashboard before.

## Installation

To download the plugin, you need to get the HPI file by clicking on the following button:

{{< button title="Download Jenkins Plugin" >}}
https://downloads.gatling.io/releases/frontline-jenkins-plugin/{{< var ciPluginsVersion >}}/frontline-jenkins-plugin-{{< var ciPluginsVersion >}}.hpi
{{< /button >}}

You need to be connected as an administrator of your Jenkins application to install it. Go to **Manage Jenkins**, **Manage Plugins**, **Advanced settings**, **Deploy Plugin**. Choose the hpi file you downloaded, or copy and paste the download URL to the URL field. Click Deploy.

{{< img src="installation.png" alt="Installation" >}}

## API Token and Jenkins credentials

This plugin requires an API token to allow Jenkins to authenticate with Gatling Enterprise.

For Gatling Enterprise Cloud, the [API token]({{< ref "/reference/execute/cloud/admin/api-tokens" >}}) needs the **Start** permission.

We recommend storing the API token using [Jenkins credentials](https://www.jenkins.io/doc/book/using/using-credentials/). Go to **Manage Jenkins**, then **Manage credentials**. You will see your existing credentials, as well as the credentials stores and domains configured on your Jenkins instance.

{{< img src="manage-credentials.png" alt="Manage credentials" >}}

To add new credentials, click on the name of the domain you want to use, then on the **Add Credentials** button. Choose the type **Secret text** and the scope you want to restrict the credentials to, copy and paste your API token to the **Secret** field, and enter an ID (and optionally a description). Click on **Create**.

{{< img src="new-credentials.png" alt="New credentials" >}}

{{< alert warning >}}
If you don't use Jenkins credentials, you can still set an API token directly in the plugin global configuration, but this is not as secure as using the Jenkins credentials and is not recommended.
{{< /alert >}}

## Configuration

The plugin needs some global configuration. Go to **Manage Jenkins**, **Configure System**, then **Global Gatling Enterprise Plugin Configuration**.

Choose the Jenkins credentials where [you stored your API token]({{< ref "#api-token-and-jenkins-credentials" >}}).

The **Address** is the address of Gatling Enterprise Cloud (use https://cloud.gatling.io).

{{< img src="global-configuration.png" alt="Global Configuration" >}}

## Job set-up

### Set-up for a pipeline job (available since Jenkins 2.0)

#### Basics

You can use the Pipeline Snippet Generator to help you use the Jenkins Plugin. Click on the **Pipeline Syntax** link, then choose the step gatlingFrontLineLauncherStep.

{{< img src="pipeline-generator.png" alt="Snippet Generator" >}}

If you don't want to use the globally configured API token, you can choose another one [stored in a Jenkins secret text credential]({{< ref "#api-token-and-jenkins-credentials" >}}). Choose one of the simulations in the drop-down menu, then click Generate Groovy. Copy and paste the result in your Pipeline script, eg:

##### Declarative Pipeline Syntax:
```groovy
pipeline {
    agent any
    stages {
        stage("Gatling Enterprise simulation") {
            steps {
                gatlingFrontLineLauncherStep credentialId: 'MY_JENKINS_CREDENTIAL_ID', simulationId: '00eacd1c-ef91-4076-ad57-99b4c6675a9e'
            }
        }
    }
}
```

##### Scripted Pipeline Syntax:
```groovy
node {
    stage("Gatling Enterprise simulation") {
        gatlingFrontLineLauncherStep credentialId: 'MY_JENKINS_CREDENTIAL_ID', simulationId: '00eacd1c-ef91-4076-ad57-99b4c6675a9e'
    }
}
```
{{< img src="pipeline-configuration.png" alt="Pipeline Configuration" >}}

#### Passing parameters

You can specify a custom Map of system properties which will be used in the Gatling Enterprise run. The syntax is the following:

```groovy
gatlingFrontLineLauncherStep(
    simulationId: '00eacd1c-ef91-4076-ad57-99b4c6675a9e',
    systemProps: ["var": "$var1", "sensitive.var2": "this prop won't be displayed in the run snapshot"]
)
```

This step regularly prints a summary of the run's current status to the build logs. By default, the summary is printed every 5 seconds the first 12 times (i.e. for the first 60 seconds), and then every 60 seconds. You can configure this behavior (or completely disable these logs) with the following parameters:

```groovy
gatlingFrontLineLauncherStep(
    simulationId: '00eacd1c-ef91-4076-ad57-99b4c6675a9e',
    runSummaryEnabled: true,
    runSummaryInitialRefreshInterval: 5,
    runSummaryInitialRefreshCount: 12,
    runSummaryRefreshInterval: 60
)
```

#### Displaying assertions as JUnit

You can display the results of the Gatling Enterprise assertions with the JUnit plugin. Add the following line:
```groovy
junit("gatlingFrontLineJunitResults/*.xml")
```

{{< alert danger >}}
If you don't have any assertions in your Gatling simulation, the JUnit step will fail.
{{< /alert >}}

### Set-up for an old style job

Add a new build step called **Gatling Enterprise Plugin**.

If you don't want to use the globally configured API token, you can chose another one [stored in a Jenkins secret text credential]({{< ref "#api-token-and-jenkins-credentials" >}}). Choose one of the simulations in the drop-down menu.

{{< img src="build-configuration.png" alt="Build configuration" >}}

This step regularly prints a summary of the run's current status to the build logs. By default, the summary is printed every 5 seconds the first 12 times (i.e. for the first 60 seconds), and then every 60 seconds. You can configure this behavior (or disable it completely) by clicking on the Show run summary logging options button.

#### Displaying assertions as JUnit

You can display the results of the Gatling Enterprise assertions with the JUnit plugin.

Add a new build step called **Publish JUnit test result report** and fill the **Test report XMLs** input with the following line:

`gatlingFrontLineJunitResults/*.xml`

{{< img src="junit-configuration.png" alt="JUnit Configuration" >}}

{{< alert danger >}}
If you don't have any assertions in your Gatling simulation, the JUnit step will fail.
{{< /alert >}}

## Usage

A new Gatling Enterprise simulation will be started every time the job is run. Check the Console Output to see the simulation progress. If the simulation ran successfully, it will look like the following:

{{< img src="console-ok.png" alt="Console View" >}}

Live metrics will be displayed in the console, and in the Status page. The link **View Run in Gatling Enterprise** in the build page menu links to Gatling Enterprise.

{{< img src="run-view.png" alt="Results" >}}
