```yaml
workflow:
  rules:
    # Execute the pipeline only on pushes to the main branch
    - if: $CI_COMMIT_BRANCH == "main"

stages:
  - build
  - load-test

variables:
  SIMULATION_ID: '00000000-0000-0000-0000-000000000000'

# Build, package, and upload your Gatling project 
build-gatling-simulation:
  stage: build
  # sbt 1.8.2 and JDK 17.0.5; sbtscala/scala-sbt does not provide 'latest' tags
  # See https://hub.docker.com/r/sbtscala/scala-sbt for other tags available and for the latest versions
  image: sbtscala/scala-sbt:eclipse-temurin-17.0.5_8_1.8.2_2.13.10
  script:
    - sbt Gatling/enterpriseUpload -Dgatling.enterprise.simulationId=$SIMULATION_ID

# Run the simulation on Gatling Enterprise
run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
```
