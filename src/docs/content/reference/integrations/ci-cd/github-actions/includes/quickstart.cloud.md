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
          api_token: ${{ secrets.GATLING_ENTERPRISE_API_TOKEN }}
          simulation_id: ${{ inputs.simulation_id }}
```
