```shell
./start_simulation.sh \
  'http://my-gatling-instance.my-domain.tld' \
  "$GATLING_ENTERPRISE_API_TOKEN" \
  '00000000-0000-0000-0000-000000000000'
```

- Gatling Enterprise URL: address of your Gatling Enterprise server, for example:
  `http://my-gatling-instance.my-domain.tld`.
- API token: the [API token]({{< ref "reference/execute/self-hosted/admin/api-tokens" >}}) will allow the script to
  authenticate to Gatling Enterprise. The API token needs the **All** role.
- Simulation ID: the ID of the simulation you want to start. You can get this ID on the
  [Simulations table]({{< ref "reference/execute/self-hosted/user/simulations" >}}), with the {{< icon clipboard >}}
  icon.
