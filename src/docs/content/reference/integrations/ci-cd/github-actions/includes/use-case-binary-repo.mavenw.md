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

jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      # Check out your GitHub repository.
      - name: Checkout
        uses: actions/checkout@v3

      # Set up Java.
      # You can configure other versions of the JDK, as long as they are
      # supported by your version of Gatling and by your build tool.
      # See https://github.com/actions/setup-java/blob/main/README.md for options.
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'maven'
          # Configure your Maven settings.xml
          # See https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven
          server-id: repository-id-configured-in-your-pom
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD

      # Build, package, and publish your Gatling project 
      - name: Build Gatling simulation
        run: ./mvnw --batch-mode deploy
        env:
          # Retrieve username and password from GitHub secrets
          # See https://docs.github.com/en/actions/security-guides/encrypted-secrets
          MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}

      # Run the simulation on Gatling Enterprise
      - name: Gatling Enterprise Action
        uses: gatling/enterprise-action@v1
        with:
          gatling_enterprise_url: http://my-gatling-instance.my-domain.tld
          api_token: ${{ secrets.GATLING_ENTERPRISE_API_TOKEN }}
          simulation_id: '00000000-0000-0000-0000-000000000000'
```
