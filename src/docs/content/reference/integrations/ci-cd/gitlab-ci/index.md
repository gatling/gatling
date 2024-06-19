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

For Gatling Enterprise Cloud, the [API token]({{< ref "../../execute/cloud/admin/api-tokens" >}}) needs the **Start** permission.

We also assume that you have already configured a simulation on Gatling Enterprise. You can copy the simulation ID from the simulations list view. In the following examples, we will show the simulation ID as `00000000-0000-0000-0000-000000000000`.

See [Gatling Enterprise Cloud documentation]({{< ref "../../execute/cloud/user/simulations" >}}).

## Quickstart (minimal job configuration)

In this example, we configure a workflow which will only start a simulation as already configured and uploaded on Gatling Enterprise.

Create a file named `.gitlab-ci.yml` in your repository:

```yaml
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
    # We assume GATLING_ENTERPRISE_API_TOKEN is available,
    # e.g. configured on the GitLab project
    # Specify your simulation ID:
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
```

Push this to GitLab. The pipeline will run automatically on new commits; you can also run it manually from your GitLab project's CI/CD menu.

## Configuration reference

Several options can be configured with environment variables. The Docker runner also provides several outputs which you can export to access in the following stages of your pipeline.

### Inputs

Example:

```yaml
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
    GATLING_ENTERPRISE_API_TOKEN: 'my-api-token' # Typically not hard-coded in the script!
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
    EXTRA_SYSTEM_PROPERTIES: >
      {
        "sys_prop_1":"value 1",
        "sys_prop_2":42,
        "sys_prop_3":true
      }
    EXTRA_ENVIRONMENT_VARIABLES: >
      {
        "ENV_VAR_1":"value 1",
        "ENV_VAR_2":42,
        "ENV_VAR_3":true
      }
    OVERRIDE_LOAD_GENERATORS: >
      {
        "4a399023-d443-3a58-864f-3919760df78b":{"size":1,"weight":60},
        "c800b6d9-163b-3db7-928f-86c1470a9542":{"size":1,"weight":40}
      }
    FAIL_ACTION_ON_RUN_FAILURE: 'true'
    WAIT_FOR_RUN_END: 'true'
    OUTPUT_DOT_ENV_FILE_PATH: 'path/to/file.env'
    RUN_SUMMARY_ENABLED: 'true'
    RUN_SUMMARY_INITIAL_REFRESH_INTERVAL: '5'
    RUN_SUMMARY_INITIAL_REFRESH_COUNT: '12'
    RUN_SUMMARY_REFRESH_INTERVAL: '60'
```

- `GATLING_ENTERPRISE_API_TOKEN` {{< badge danger >}}required{{< /badge >}}: The API token used to authenticate with Gatling Enterprise.

- `SIMULATION_ID` {{< badge danger >}}required{{< /badge >}}: The ID of the simulation as configured on Gatling Enterprise.

- `EXTRA_SYSTEM_PROPERTIES` {{< badge info >}}optional{{< /badge >}}: Additional Java system properties, will be merged with the simulation's configured system properties. Must be formatted as a JSON object containing the desired key/value pairs. Values can be strings, numbers or booleans.

- `EXTRA_ENVIRONMENT_VARIABLES` {{< badge info >}}optional{{< /badge >}}: Additional environment variables, will be merged with the simulation's configured environment variables. Must be formatted as a JSON object containing the desired key/value pairs. Values can be strings, numbers or booleans.

- `OVERRIDE_LOAD_GENERATORS` {{< badge info >}}optional{{< /badge >}}: Overrides the simulation's load generators configuration. Must be formatted as a JSON object. Keys are the load generator IDs, which can be retrieved from the public API (using the `/pools` route). Weights are optional.

  See [Gatling Enterprise Cloud public API documentation]({{< ref "../../execute/cloud/user/api" >}}).

- `FAIL_ACTION_ON_RUN_FAILURE` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): If `true`, the Action will fail if the simulation run ends in an error (including failed assertions). Note: if set to `false` and the simulation ends in an error, some of the outputs may be missing (e.g. there will be no assertion results if the simulation crashed before the end).

- `WAIT_FOR_RUN_END` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): If `true`, the runner will wait for the end of te simulation run on Gatling Enterprise before terminating. Note: if set to `false`, some of the outputs may be missing (there will be no status nor assertion results).

- `OUTPUT_DOT_ENV_FILE_PATH` {{< badge info >}}optional{{< /badge >}} (defaults to `gatlingEnterprise.env`): path to a dotenv file where output values will be written

- `RUN_SUMMARY_ENABLED` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): Assuming `wait_for_run_end` is also true, will regularly log a summary of the ongoing run to the console until it finishes. See also the [logs section]({{< ref "#logs" >}}).

- `RUN_SUMMARY_INITIAL_REFRESH_INTERVAL` {{< badge info >}}optional{{< /badge >}} (defaults to `5`): Initial interval before displaying a new summary of the ongoing run in the console, in seconds. Should be a multiple of 5 (otherwise it will be rounded up). Only used a limited number of times (set by `run_summary_initial_refresh_count`) before switching to the interval set by run_summary_refresh_interval. See also the [logs section]({{< ref "#logs" >}}).

- `RUN_SUMMARY_INITIAL_REFRESH_COUNT` {{< badge info >}}optional{{< /badge >}} (defaults to `12`): Number of times to use `run_summary_initial_refresh_interval` as the interval before displaying a new summary of the ongoing run in the console. After that, `run_summary_refresh_interval` will be used. This allows to avoid spamming the log output once the test run is well underway. See also the [logs section]({{< ref "#logs" >}}).

- `RUN_SUMMARY_REFRESH_INTERVAL` {{< badge info >}}optional{{< /badge >}} (defaults to `60`): Interval before displaying a new summary of the ongoing run in the console, in seconds. Should be a multiple of 5 (otherwise it will be rounded up). See also the [logs section]({{< ref "#logs" >}}).

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
