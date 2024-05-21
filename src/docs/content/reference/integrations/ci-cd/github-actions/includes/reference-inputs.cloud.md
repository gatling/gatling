
Example:

```yaml
steps:
  - uses: gatling/enterprise-action@v1
    with:
      api_token: ${{ secrets.GATLING_ENTERPRISE_API_TOKEN }}
      simulation_id: '00000000-0000-0000-0000-000000000000'
      extra_system_properties: >
        {
          "sys_prop_1":"value 1",
          "sys_prop_2":42,
          "sys_prop_3":true
        }
      extra_environment_variables: >
        {
          "ENV_VAR_1":"value 1",
          "ENV_VAR_2":42,
          "ENV_VAR_3":true
        }
      override_load_generators: >
        {
          "4a399023-d443-3a58-864f-3919760df78b":{"size":1,"weight":60},
          "c800b6d9-163b-3db7-928f-86c1470a9542":{"size":1,"weight":40}
        }
      fail_action_on_run_failure: true
      wait_for_run_end: true
      run_summary_enabled: true
      run_summary_initial_refresh_interval: 5
      run_summary_initial_refresh_count: 12
      run_summary_refresh_interval: 60
```

- `api_token` {{< badge danger >}}required{{< /badge >}} (unless using an environment variable named `GATLING_ENTERPRISE_API_TOKEN` instead): The API token used by the Action to authenticate with Gatling Enterprise.

- `simulation_id` {{< badge danger >}}required{{< /badge >}}: The ID of the simulation as configured on Gatling Enterprise.

- `extra_system_properties` {{< badge info >}}optional{{< /badge >}}: Additional Java system properties, will be merged with the simulation's configured system properties. Must be formatted as a JSON object containing the desired key/value pairs. Values can be strings, numbers or booleans.

- `extra_environment_variables` {{< badge info >}}optional{{< /badge >}}: Additional environment variables, will be merged with the simulation's configured environment variables. Must be formatted as a JSON object containing the desired key/value pairs. Values can be strings, numbers or booleans.

- `override_load_generators` {{< badge info >}}optional{{< /badge >}}: Overrides the simulation's load generators configuration. Must be formatted as a JSON object. Keys are the load generator IDs, which can be retrieved [from the public API]({{< ref "../../execute/cloud/user/api" >}}) (using the `/pools` route). Weights are optional.

- `fail_action_on_run_failure` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): If `true`, the Action will fail if the simulation run ends in an error (including failed assertions). Note: if set to `false` and the simulation ends in an error, some of the outputs may be missing (e.g. there will be no assertion results if the simulation crashed before the end).

- `wait_for_run_end` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): If `true`, the Action will wait for the end of te simulation run on Gatling Enterprise before terminating. Note: if set to `false`, some of the outputs may be missing (there will be no status nor assertion results).

- `run_summary_enabled` {{< badge info >}}optional{{< /badge >}} (defaults to `true`): Assuming `wait_for_run_end` is also true, will regularly log a summary of the ongoing run to the console until it finishes. See also the [logs section]({{< ref "#logs" >}}).

- `run_summary_initial_refresh_interval` {{< badge info >}}optional{{< /badge >}} (defaults to `5`): Initial interval before displaying a new summary of the ongoing run in the console, in seconds. Should be a multiple of 5 (otherwise it will be rounded up). Only used a limited number of times (set by `run_summary_initial_refresh_count`) before switching to the interval set by run_summary_refresh_interval. See also the [logs section]({{< ref "#logs" >}}).

- `run_summary_initial_refresh_count` {{< badge info >}}optional{{< /badge >}} (defaults to `12`): Number of times to use `run_summary_initial_refresh_interval` as the interval before displaying a new summary of the ongoing run in the console. After that, `run_summary_refresh_interval` will be used. This allows to avoid spamming the log output once the test run is well underway. See also the [logs section]({{< ref "#logs" >}}).

- `run_summary_refresh_interval` {{< badge info >}}optional{{< /badge >}} (defaults to `60`): Interval before displaying a new summary of the ongoing run in the console, in seconds. Should be a multiple of 5 (otherwise it will be rounded up). See also the [logs section]({{< ref "#logs" >}}).
