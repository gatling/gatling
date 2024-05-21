```yaml
name: Run Gatling Enterprise Simulation

# Execute the workflow on each push to the main branch
on:
  push:
    branches:
      - main

# Here we use concurrency to cancel previous executions if they are still
# ongoing. Useful to avoid running the same simulation several times
# simultaneously if we push several code changes in a short time.
# See https://docs.github.com/actions/using-jobs/using-concurrency.
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

# The GATLING_ENTERPRISE_API_TOKEN environment variable is recognized by both
# the Gatling Maven plugin and the gatling/enterprise-action Action
env:
  SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
  GATLING_ENTERPRISE_API_TOKEN: ${{ secrets.GATLING_ENTERPRISE_API_TOKEN }}

jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      # Check out your GitHub repository.
      - name: Checkout
        uses: actions/checkout@v4

      # Set up Java.
      # You can configure other versions of the JDK, as long as they are
      # supported by your version of Gatling and by your build tool.
      # See https://github.com/actions/setup-java/blob/main/README.md for options.
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'zulu'
          java-version: '21'
          cache: 'maven'

      # Build, package, and upload your Gatling project 
      - name: Build Gatling simulation
        run: ./mvnw gatling:enterpriseUpload -Dgatling.enterprise.simulationId=${{ env.SIMULATION_ID }}

      # Run the simulation on Gatling Enterprise
      - name: Gatling Enterprise Action
        uses: gatling/enterprise-action@v1
        with:
          simulation_id: ${{ env.SIMULATION_ID }}
```
