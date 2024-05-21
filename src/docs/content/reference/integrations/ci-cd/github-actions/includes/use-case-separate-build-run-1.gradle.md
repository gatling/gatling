```yaml
name: Build and update Gatling Enterprise Simulation

# Execute the workflow on each push to the main branch
on:
  push:
    branches:
      - main

# Here we use concurrency to cancel previous executions if they are still
# ongoing. Useful to avoid wasting build time if we push several code changes
# in a short time.
# See https://docs.github.com/actions/using-jobs/using-concurrency.
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

# The GATLING_ENTERPRISE_API_TOKEN environment variable is recognized by the
# Gatling Gradle plugin
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

      # Set up Java and the build tools (including Gradle).
      # You can configure other versions of the JDK, as long as they are
      # supported by your version of Gatling and by your build tool.
      # See https://github.com/actions/setup-java/blob/main/README.md for options.
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'zulu'
          java-version: '21'
          cache: 'gradle'

      # Build, package, and upload your Gatling project 
      - name: Build Gatling simulation
        run: gradle gatlingEnterpriseUpload -Dgatling.enterprise.simulationId=${{ env.SIMULATION_ID }}
```
