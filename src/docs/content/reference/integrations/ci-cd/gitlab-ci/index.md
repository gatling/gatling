---
menutitle: Gitlab CI/CD
title: Gitlab CI/CD integration
seotitle: Gitlab CI/CD integration for Gatling Enterprise
description: Learn how to configure GitLab CI/CD to run your simulations on Gatling Enterprise.
lead: Run your Gatling Enterprise simulations from GitLab CI/CD.
badge:
  type: enterprise
  label: Enterprise
aliases:
  - /reference/extensions/ci-cd/gitlab-ci
date: 2023-02-17T14:00:00+00:00
lastmod: 2023-10-11T10:10:00+00:00
---

## The Gatling Enterprise Docker runner

This runner, packaged as a Docker image and [published on Docker Hub](https://hub.docker.com/r/gatlingcorp/enterprise-runner), enables you to start a Gatling Enterprise simulation directly from your GitLab CI/CD pipelines.

This plugin doesn't create a new Gatling Enterprise simulation, you have to create it using the Gatling Enterprise Dashboard before.

On Gatling Enterprise Cloud, you can do it using the options provided by our build tools plugins:

- [Maven]({{< ref "../build-tools/maven-plugin#running-your-simulations-on-gatling-enterprise-cloud" >}})
- [Gradle]({{< ref "../build-tools/gradle-plugin#running-your-simulations-on-gatling-enterprise-cloud" >}})
- [sbt]({{< ref "../build-tools/sbt-plugin#running-your-simulations-on-gatling-enterprise-cloud" >}})

Don't forget to check out [GitLab's official documentation](https://docs.gitlab.com/ee/ci/) to learn how to write CI/CD pipelines on GitLab.

## Docker Hub coordinates

The Docker image is [published on Docker Hub](https://hub.docker.com/r/gatlingcorp/enterprise-runner) with the following coordinates: `gatlingcorp/enterprise-runner:1`.

You can check out the latest releases available [from the GitHub project](https://github.com/gatling/enterprise-action/releases). You generally only need to specify the major version you want to use, currently `1`.

## Pre-requisites

You must first create an API token. It will be used to authenticate with Gatling Enterprise.

You can store the API Token in a [Gitlab CI Variable](https://docs.gitlab.com/ee/ci/variables/#define-a-cicd-variable-in-the-ui) (make sure to check "Mask variable") with the name `GATLING_ENTERPRISE_API_TOKEN`, which our tools will detect automatically. Or if you [use a vault to store secrets](https://docs.gitlab.com/ee/ci/secrets/), store the API Token in your vault and retrieve its value to an environment variable named `GATLING_ENTERPRISE_API_TOKEN` in your Gitlab CI/CD configuration file.

- for Gatling Enterprise Cloud, the [API token]({{< ref "../../execute/cloud/admin/api-tokens" >}}) needs the **Start** permission.
- for Gatling Enterprise Self-Hosted, the [API token]({{< ref "../../execute/self-hosted/admin/api-tokens" >}}) needs the **All** role.

{{< alert info >}}
For {{< badge self-hosted "Self-Hosted" />}}, the runner will also need the **URL for your Gatling Enterprise instance**. In the following examples, we will use `http://my-gatling-instance.my-domain.tld`, but you must replace it with the correct URL for your Gatling Enterprise Self-Hosted instance.

Please also note that it **must be accessible** from [the GitLab runners](https://docs.gitlab.com/runner/) you plan to use (either the GitLab.com runners or your own self-managed runners).
{{< /alert >}}

We also assume that you have already configured a simulation on Gatling Enterprise. You can copy the simulation ID from the simulations list view. In the following examples, we will show the simulation ID as `00000000-0000-0000-0000-000000000000`.

See [Gatling Enterprise Cloud documentation]({{< ref "../../execute/cloud/user/simulations" >}}) or [Gatling Enterprise Self-Hosted documentation]({{< ref "../../execute/self-hosted/user/simulations" >}}).

## Quickstart (minimal job configuration)

In this example, we configure a workflow which will only start a simulation as already configured and uploaded on Gatling Enterprise.

Create a file named `.gitlab-ci.yml` in your repository:

{{< include-file >}}
Cloud: includes/quickstart.cloud.md
Self-Hosted: includes/quickstart.self-hosted.md
{{< /include-file >}}

Push this to GitLab. The pipeline will run automatically on new commits; you can also run it manually from your GitLab project's CI/CD menu.

## Configuration reference

Several options can be configured with environment variables. The Docker runner also provides several outputs which you can export to access in the following stages of your pipeline.

### Inputs

{{< include-file >}}
Cloud: includes/reference-inputs.cloud.md
Self-Hosted: includes/reference-inputs.self-hosted.md
{{< /include-file >}}


### Outputs

Outputs are written to a dotenv file, which can then be exported to make the variables available. Check out [the GitLab documentation](https://docs.gitlab.com/ee/ci/variables/#pass-an-environment-variable-to-another-job) for more details on exporting dotenv files. Example:

```yaml
stages:
  - load-test
  - post-load-test

run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
  variables:
    # For Gatling Enterprise Self-Hosted, you must specify the URL:
    # GATLING_ENTERPRISE_URL: 'http://my-gatling-instance.my-domain.tld'
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
  artifacts:
    reports:
      dotenv: 'gatlingEnterprise.env' # Using the default value

print-output:
  stage: post-load-test
  image: alpine:latest
  script: |
    # Show that we can access the outputs exported by the previous stage
    echo "RUN_ID=$RUN_ID"
    echo "REPORTS_URL=$REPORTS_URL"
    echo "RUNS_URL=$RUNS_URL"
    echo "RUN_STATUS_CODE=$RUN_STATUS_CODE"
    echo "RUN_STATUS_NAME=$RUN_STATUS_NAME"
    echo "RUN_ASSERTIONS=$RUN_ASSERTIONS"
```

- `RUN_ID`: The ID of the run started by this runner.

- `REPORTS_URL`: The URL of the reports page for this run.

- `RUNS_URL`: The URL of the runs history page for this simulation.

- `RUN_STATUS_NAME`: The name of the run's final status (e.g. `Successful`, `AssertionsSuccessful`, `AssertionsFailed`, etc.).

- `RUN_STATUS_CODE`: The code of the run's final status.

- `RUN_ASSERTIONS`: The results of the run's assertions, as a JSON array.

### Logs

Every few seconds, the Docker runner logs to the console output a summary of the run's current status. When the run ends, it logs the status of the run and the results of any assertions. Here's the beginning and end of a very short duration example:

{{< img src="reference_logs_start.png" alt="A run's logs in the GitLab CI/CD console (beginning)" >}}

{{< img src="reference_logs_end.png" alt="A run's logs in the GitLab CI/CD console (end)" >}}

By default, logs are printed every 5 seconds the first 12 times (i.e. during 60 seconds), then every 60 seconds. This can be adjusted using the input variables `RUN_SUMMARY_INITIAL_REFRESH_INTERVAL`, `RUN_SUMMARY_INITIAL_REFRESH_COUNT`, and `RUN_SUMMARY_REFRESH_INTERVAL`. The ongoing logs can also be completely disabled using the input variable `RUN_SUMMARY_ENABLED: 'false'`: in this case, only the final results will be printed.

## Sample use cases for cloud {{% badge cloud "Cloud" /%}} {#cloud-sample-use-cases}

### Build and run simulation

This pipeline is defined in the GitLab repository which contains your Gatling simulation script built with one of our build tools plugins. In this example, every time the code on the `main` branch gets updated, we:

- build, package, and upload to Gatling Enterprise the current version of the simulation script
- run the updated simulation on Gatling Enterprise

Feel free to use different workflow rules or to configure the other inputs and outputs for the runner as documented above, according to your own use case.

{{< include-file >}}
Maven: includes/use-case-build-and-run.maven.md
Maven Wrapper: includes/use-case-build-and-run.mavenw.md
Gradle: includes/use-case-build-and-run.gradle.md
Gradle Wrapper: includes/use-case-build-and-run.gradlew.md
sbt: includes/use-case-build-and-run.sbt.md
{{< /include-file >}}

### Build and update on every push, run on a schedule

This first pipeline is defined in the GitLab repository which contains your Gatling simulation script built with one of our build tools plugins. In this example, every time the code on the `main` branch gets updated, we build, package, and upload to Gatling Enterprise the current version of the simulation script.

{{< include-file >}}
Maven: includes/use-case-separate-build-run-1.maven.md
Maven Wrapper: includes/use-case-separate-build-run-1.mavenw.md
Gradle: includes/use-case-separate-build-run-1.gradle.md
Gradle Wrapper: includes/use-case-separate-build-run-1.gradlew.md
sbt: includes/use-case-separate-build-run-1.sbt.md
{{< /include-file >}}

This second pipeline may be defined in the same repository or another one. It will only run when started by a pipeline schedule, which you can [configure in your GitLab project](https://docs.gitlab.com/ee/ci/pipelines/schedules.html), for example to run once a week.

```yaml
workflow:
  rules:
    # Execute the pipeline only when scheduled
    - if: $CI_PIPELINE_SOURCE == "schedule"

stages:
  - load-test

run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
  variables:
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
```

## Sample use cases {{% badge self-hosted "Self-Hosted" /%}} {#self-hosted-sample-use-cases}

### Building from sources

In this example, we assume you have configured your repository on Gatling Enterprise to [build from sources]({{< ref "../../execute/self-hosted/user/repositories/#downloading-from-sources" >}}), from your GitHub repository's `main` branch. Every time the code on the `main` branch gets updated, we run the updated simulation on Gatling Enterprise.

Feel free to use different trigger events or to configure the other inputs and outputs for the runner as documented above, according to your own use case. But keep in mind that Gatling Enterprise will only download and run your simulation scripts from the branch set [in the simulation configuration]({{< ref "../../execute/self-hosted/user/simulations/#option-1-build-from-sources" >}})!

```yaml
workflow:
  rules:
    # Execute the pipeline only on pushes to the main branch
    - if: $CI_COMMIT_BRANCH == "main"

stages:
  - load-test

# Run the simulation on Gatling Enterprise
# If it is configured to "build from sources" from the branch "main",
# it will download and run the updated version of the code
run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
  variables:
    GATLING_ENTERPRISE_URL: 'http://my-gatling-instance.my-domain.tld'
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
```

### Using a binary repository

This workflow is defined in the GitLab repository which contains your Gatling simulation script built with one of our build tools plugins. In this example, every time the code on the `main` branch gets updated, we build, package, and publish the current version of the simulation script, before starting the simulation on Gatling Enterprise.

In this example, we assume that:
- You have configured your repository on Gatling Enterprise to [download from a binary repository]({{< ref "../../execute/self-hosted/user/repositories/#downloading-from-a-binary-repository" >}}), using Artifactory or Sonatype Nexus.
- You have [configured your simulation]({{< ref "../../execute/self-hosted/user/simulations/#option-2-download-binary-from-repository" >}}) to use the version marker `latest.integration` for the artifact published on the binary repository.
- Your build is properly configured to publish to the binary repository, using [Maven]({{< ref "../build-tools/maven-plugin#publish-to-a-binary-repository" >}}), [Gradle]({{< ref "../build-tools/gradle-plugin#publish-to-a-binary-repository" >}}), or [sbt]({{< ref "../build-tools/sbt-plugin#publish-to-a-binary-repository" >}}).

{{< include-file >}}
Maven: includes/use-case-binary-repo.maven.md
Maven Wrapper: includes/use-case-binary-repo.maven.md
Gradle: includes/use-case-binary-repo.gradle.md
Gradle Wrapper: includes/use-case-binary-repo.gradlew.md
sbt: includes/use-case-binary-repo.sbt.md
{{< /include-file >}}

{{< alert tip >}}
For each build tool, there can be different ways to configure credentials for the target repository. We only provide some examples, with links to the relevant documentations they are based on.
{{< /alert >}}

### Run the simulation weekly

This pipeline will only run when started by a pipeline schedule, which you can [configure in your GitLab project](https://docs.gitlab.com/ee/ci/pipelines/schedules.html), for example to run once a week.

```yaml
workflow:
  rules:
    # Execute the pipeline only when scheduled
    - if: $CI_PIPELINE_SOURCE == "schedule"

stages:
  - load-test

run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
  variables:
    GATLING_ENTERPRISE_URL: 'http://my-gatling-instance.my-domain.tld'
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
```
