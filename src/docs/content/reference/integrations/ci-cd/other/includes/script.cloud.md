```shell
./start_simulation.sh \
  'https://cloud.gatling.io' \
  "$GATLING_ENTERPRISE_API_TOKEN" \
  '00000000-0000-0000-0000-000000000000'
```

- Gatling Enterprise URL: `https://cloud.gatling.io`.
- API token: the [API token]({{< ref "reference/execute/cloud/admin/api-tokens" >}}) will allow the script to
  authenticate to Gatling Enterprise. The API token needs the **Configure** permission.
- Simulation ID: the ID of the simulation you want to start. You can get this ID on the
  [Simulations table]({{< ref "reference/execute/cloud/user/simulations" >}}), with the {{< icon clipboard >}} icon.
