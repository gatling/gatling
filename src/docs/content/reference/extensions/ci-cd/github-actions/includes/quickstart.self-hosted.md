```yaml
name: Run Gatling Enterprise Simulation

on:
  workflow_dispatch:
    inputs:
      simulation_id:
        type: string
        required: true

jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - name: Gatling Enterprise Action
        uses: gatling/enterprise-action@v1
        with:
          # Specify the URL for your Gatling Enterprise Self-Hosted instance:
          gatling_enterprise_url: http://my-gatling-instance.my-domain.tld
          api_token: ${{ secrets.GATLING_ENTERPRISE_API_TOKEN }}
          simulation_id: ${{ inputs.simulation_id }}
```
