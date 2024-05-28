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

  See [Gatling Enterprise Cloud public API documentation]({{< ref "../../execute/cloud/user/api" >}}) or [Gatling Enterprise Self-Hosted public API documentation]({{< ref "../../execute/self-hosted/user/api" >}}).

- `FAIL_ACTION_ON_RUN_FAILURE` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): If `true`, the Action will fail if the simulation run ends in an error (including failed assertions). Note: if set to `false` and the simulation ends in an error, some of the outputs may be missing (e.g. there will be no assertion results if the simulation crashed before the end).

- `WAIT_FOR_RUN_END` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): If `true`, the runner will wait for the end of te simulation run on Gatling Enterprise before terminating. Note: if set to `false`, some of the outputs may be missing (there will be no status nor assertion results).

- `OUTPUT_DOT_ENV_FILE_PATH` {{< badge info >}}optional{{< /badge >}} (defaults to `gatlingEnterprise.env`): path to a dotenv file where output values will be written

- `RUN_SUMMARY_ENABLED` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): Assuming `wait_for_run_end` is also true, will regularly log a summary of the ongoing run to the console until it finishes. See also the [logs section]({{< ref "#logs" >}}).

- `RUN_SUMMARY_INITIAL_REFRESH_INTERVAL` {{< badge info >}}optional{{< /badge >}} (defaults to `5`): Initial interval before displaying a new summary of the ongoing run in the console, in seconds. Should be a multiple of 5 (otherwise it will be rounded up). Only used a limited number of times (set by `run_summary_initial_refresh_count`) before switching to the interval set by run_summary_refresh_interval. See also the [logs section]({{< ref "#logs" >}}).

- `RUN_SUMMARY_INITIAL_REFRESH_COUNT` {{< badge info >}}optional{{< /badge >}} (defaults to `12`): Number of times to use `run_summary_initial_refresh_interval` as the interval before displaying a new summary of the ongoing run in the console. After that, `run_summary_refresh_interval` will be used. This allows to avoid spamming the log output once the test run is well underway. See also the [logs section]({{< ref "#logs" >}}).

- `RUN_SUMMARY_REFRESH_INTERVAL` {{< badge info >}}optional{{< /badge >}} (defaults to `60`): Interval before displaying a new summary of the ongoing run in the console, in seconds. Should be a multiple of 5 (otherwise it will be rounded up). See also the [logs section]({{< ref "#logs" >}}).
