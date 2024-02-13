---
menutitle: Other CI/CD systems
title: Integrating Other CI/CD Systems
seotitle: Integrating other CI/CD systems with Gatling Enterprise
description: Learn how to configure other Continuous Integration/Continuous Delivery systems to run your simulations on Gatling Enterprise.
lead: Run your Gatling Enterprise simulations from any CI/CD product.
badge:
  type: enterprise
  label: Enterprise
date: 2021-03-08T12:50:17+00:00
lastmod: 2023-02-17T14:00:00+00:00
---

We provide dedicated support for a number of CI tools: [GitHub Actions]({{< ref "github-actions" >}}), [Gitlab CI]({{< ref "gitlab-ci" >}}), [Jenkins]({{< ref "jenkins" >}}), [Teamcity]({{< ref "teamcity" >}}), [Bamboo]({{< ref "github-actions" >}}). However, we also document here how to run your simulations on Gatling Enterprise from any other CI products, using either one of the supported build tools or our CI shell script. Note that we also provide dedicated instructions to use our CI shell script [with Azure DevOps Pipelines]({{< ref "./azure-devops" >}})

This will not create a new Gatling Enterprise simulation, you have to create it using the Gatling Enterprise Dashboard before, or do it using the options provided by our build tools plugins:
- [Maven]({{< ref "../build-tools/maven-plugin#working-with-gatling-enterprise-cloud" >}})
- [Gradle]({{< ref "../build-tools/gradle-plugin#working-with-gatling-enterprise-cloud" >}})
- [sbt]({{< ref "../build-tools/sbt-plugin#working-with-gatling-enterprise-cloud" >}})

## Pre-requisites

You must first [create an API token]({{< ref "../../execute/cloud/admin/api-tokens" >}}). It will be used to authenticate with Gatling Enterprise. Most CI tools should offer a way to store this token securely, and expose it to build scripts as an environment variable.

In the following examples, we assume the API Token is available in an environment variable named `GATLING_ENTERPRISE_API_TOKEN`, which our tools will detect automatically.

We also assume that you have already [configured a simulation]({{< ref "../../execute/cloud/user/simulations" >}}) on Gatling Enterprise. You can copy the simulation ID from the simulations list view. In the following examples, we will show the simulation ID as `00000000-0000-0000-0000-000000000000`.

## Using a build tool plugin

You can build you Simulation, and then run the updated Simulation on Gatling Enterprise, using the `enterpriseStart` command with any of our supported build tools.

With the `waitForRunEnd=true` option, it will display live metrics until the end of the run, and exit with an error code if the run fails on Gatling Enterprise (e.g. if the run crashes or if the assertions fail).

Configure your CI build to run the command corresponding to the build tool you use:

{{< include-file >}}
Maven: includes/run-with-build-tool.maven.md
Gradle: includes/run-with-build-tool.gradle.md
Gradle Wrapper: includes/run-with-build-tool.gradlew.md
sbt: includes/run-with-build-tool.sbt.md
{{< /include-file  >}}

## Using a shell script

This script launches an existing simulation on Gatling Enterprise and displays live metrics.

It can be [downloaded here](https://downloads.gatling.io/releases/frontline-ci-script/{{< var ciPluginsVersion >}}/frontline-ci-script-{{< var ciPluginsVersion >}}.zip).

### Shell script requirements

This script runs with:

- the `bash` shell
- the `curl` HTTP client, [see here for more information](https://curl.se/)
- the `jq` JSON processor, [see here for more information](https://stedolan.github.io/jq/)

These tools must be installed on the machine or container where your CI system will execute the script.

### Shell script usage

Configure your CI build to call the script with 3 parameters like this:

{{< include-file >}}
1-Cloud: includes/script.cloud.md
2-Self-Hosted: includes/script.self-hosted.md
{{< /include-file  >}}
