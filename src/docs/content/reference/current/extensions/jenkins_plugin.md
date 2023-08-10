---
title: "Jenkins Plugin"
description: "How to use the Jenkins plugin to set up your load tests in your Continuous Integration pipelines."
lead: "Jenkins CI plugins for Gatling Open-Source and Enterprise"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2080400
---

Thanks to this plugin, you can track a Gatling simulation launched by the Maven plugin in Jenkins.

## Getting Gatling Jenkins plugin

The Gatling Jenkins plugin is available in the Jenkins plugin manager.

You can also get the Gatling Jenkins plugin as a `.hpi` file [here](http://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/gatling).

The source code is hosted in a dedicated project: [gatling-plugin](https://github.com/jenkinsci/gatling-plugin).

## Installing Gatling Jenkins plugin in Jenkins

You can install the plugin manually by following the official Jenkins documentation [here](https://jenkins.io/doc/book/managing/plugins/#installing-a-plugin).

## Jenkins plugin usage

Documentation of the Gatling Jenkins plugin is available on the [jenkins.io website](https://plugins.jenkins.io/gatling).

## How it works

Even if the Jenkins plugin was built with the Maven plugin in mind, using it is not mandatory to be able to archive reports and track your results with the Jenkins plugin.

The Jenkins plugin looks into your job's workspace for any simulation report it can locate, and archives only reports that haven't been archived yet (meaning that you don't need to clean your workspace to delete previous reports).

As long as you can configure a job that will launch Gatling, execute a simulation, and generate a report in your job's workspace (using the Maven plugin, SBT, a shell script, or whatever), you're good to go!

{{< alert tip >}}
The Jenkins plugin relies on the report folder's naming convention *runname-rundate* (e.g. mysimulation-201305132230). You must not change the report folder's name for the Jenkins plugin to work.
{{< /alert >}}


## Using Jenkins with Gatling Enterprise

[Gatling Enterprise](https://gatling.io/enterprise/) has a dedicated Jenkins plugin.
This plugin can launch a Gatling Enterprise simulation and display live metrics.

Please refer to the Gatling Enterprise documentation for installing this plugin on [Cloud](https://gatling.io/docs/enterprise/cloud/reference/plugins/jenkins/) or [Self-hosted](https://gatling.io/docs/enterprise/self-hosted/reference/current/plugins/jenkins/).
