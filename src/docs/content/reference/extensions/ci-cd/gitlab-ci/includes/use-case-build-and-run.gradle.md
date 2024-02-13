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
  # Gradle 8 and JDK 17; see https://hub.docker.com/_/gradle for other tags available
  # See also https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/ci/templates/Gradle.gitlab-ci.yml
  # for other useful options for Gradle builds.
  image: gradle:8-jdk17
  script:
    - gradle gatlingEnterpriseUpload -Dgatling.enterprise.simulationId=$SIMULATION_ID

# Run the simulation on Gatling Enterprise
run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
```
