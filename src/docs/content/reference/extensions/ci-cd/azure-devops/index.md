---
menutitle: Azure DevOps Pipelines
title: Integrating Azure DevOps Pipelines
seotitle: Integrating Azure DevOps Pipelines with Gatling Enterprise
description: Learn how to use the CI script with Azure DevOps Pipelines to run your simulations.
lead: Run your Gatling Enterprise simulations from Azure DevOps Pipelines.
badge:
  type: enterprise
  label: Enterprise
date: 2023-11-09T15:25:27:+0000
lastmod: 2023-11-09T15:25:27:+0000
---

We do not currently provide an official Azure Pipelines extension.
This page documents how to use our [CI script]({{< ref "other#using-a-shell-script" >}}) to run tests on Gatling
Enterprise with Azure DevOps

{{< alert info >}}
All the configuration shown in this page is available on the demo project [gatling/azure-devops-demo](https://github.com/gatling/azure-devops-demo).
{{< /alert >}}

## Requirements

You will need:

- A git repository: we will be using GitHub in our example
- A working simulation on [Gatling Enterprise Cloud](https://cloud.gatling.io) or Gatling Enterprise Self-Hosted: you
  will need its ID later on, which you can copy with **Copy Simulation ID to clipboard** in the Simulations page
- An API Token:
  - for Gatling Enterprise Cloud, the [API token]({{< ref "../../execute/cloud/admin/api-tokens" >}}) needs the **Start** permission.
  - for Gatling Enterprise Self-Hosted, the [API token]({{< ref "../../execute/self-hosted/admin/api-tokens" >}}) needs the **All** role.

On Azure DevOps, your will need:

- An Azure DevOps project
- An agent pool:
  - You can learn how to create one by following the official [Azure Devops documentation](https://learn.microsoft.com/en-us/azure/devops/pipelines/agents/agents)
  - You will need the agent pool's name later on, in our example we will use `AzurePipelinesAgentsPool`

## Preparing the repository

First, [download the CI script](https://downloads.gatling.io/releases/frontline-ci-script/{{< var ciPluginsVersion >}}/frontline-ci-script-{{< var ciPluginsVersion >}}.zip), unzip it and put the content at the root of the repository. Make sure it is named `start_simulation.sh`.

{{< alert info >}}
If you want to learn more about this CI script, you can check its [documentation page]({{< ref "other#using-a-shell-script" >}}).
{{< /alert >}}

Create a file called `azure-pipelines.yml` at the root of the repository with the following content:

```yaml
trigger: none

pool: AzurePipelinesPool # Name of the agent pool configured in Azure DevOps

steps:
- task: Bash@3
  displayName: Install JQ
  inputs:
    targetType: 'inline'
    # Curl and JQ are required by the CI script
    # In this example, we use Ubuntu agents which come with curl but not jq
    script: 'sudo apt-get update && sudo apt-get -y install jq'
- task: Bash@3
  displayName: Start simulation
  inputs:
    filePath: 'start_simulation.sh'
    arguments: >
      $(gatlingEnterpriseUrl)
      $(apiToken)
      $(simulationId)
```

{{< alert info >}}
The CI script requires both Curl and JQ.
Make sure they are already installed on your agents or install them in a preliminary step like we did above for JQ.
{{< /alert >}}

## Creating a pipeline

On Azure DevOps, click on **Pipelines** in the left menu, then on **Create Pipeline** or **New Pipeline**.

From here, you will follow a 4 steps process: Connect -> Select -> Configure -> Review.

1. On the **Connect** step, select the type of the repository you want to use. We will be using GitHub.
2. On the **Select** step, select the repository your created earlier.

    If you never connected Azure DevOps to your GitHub account, you'll be prompted by GitHub to authorize the Azure DevOps application to access your repositories on your behalf.

    After your account is linked, you will be able to browse your repositories. When using an organization repository, click on **All repositories** on the top right.

3. On the **Configure** step, as we already made a `azure-pipelines.yml` file, the step will be skipped and you will be asked to review its content directly.

4. On the **Review** step, create three variables by clicking **New variable** on the top right, then:

    - `gatlingEnterpriseUrl`: use `https://cloud.gatling.io`, or your Gatling Enterprise Self-Hosted hostname
    - `apiToken`: make sure **Keep this value secret** is enabled
    - `simulationId`: use the ID of the simulation you want to run

Here is what it looks like:

<div style="margin: 0 auto; max-width: 350px">
{{< img src="variables-configured.png" alt="Variables all configured" caption="Variables all configured" >}}
</div>

Click on **Save**, and then on **Run** to start the job. You will be redirected to the Job summary page.

After the job has been queued, if you see permissions errors, click on the red **Permission needed** link on the bottom left:

{{< img src="permission-needed.png" alt="Permissions needed waiting for user action" >}}

After the job is created and queued, you can see the output of a Gatling Enterprise run by clicking on **Job**, 
then on the **Start simulation** task:

{{< img src="successful-run.png" alt="Example of a successful run" >}}
