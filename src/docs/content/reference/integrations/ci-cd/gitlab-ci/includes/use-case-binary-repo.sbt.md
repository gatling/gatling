```yaml
workflow:
  rules:
    # Execute the pipeline only on pushes to the main branch
    - if: $CI_COMMIT_BRANCH == "main"

stages:
  - build
  - load-test

# Build, package, and upload your Gatling project 
build-gatling-simulation:
  stage: build
  # sbt 1.8.2 and JDK 17.0.5; sbtscala/scala-sbt does not provide 'latest' tags
  # See https://hub.docker.com/r/sbtscala/scala-sbt for other tags available and for the latest versions
  image: sbtscala/scala-sbt:eclipse-temurin-17.0.5_8_1.8.2_2.13.10
  # In this example, the sbt build uses credentials from a file, as documented in:
  # https://www.scala-sbt.org/1.x/docs/Publishing.html#Credentials
  # The file content is set in a variable named SBT_CREDENTIALS in the GitLab project
  script:
    - echo $SBT_CREDENTIALS > ~/.sbt/.credentials
    - sbt publish

# Run the simulation on Gatling Enterprise
run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
  variables:
    GATLING_ENTERPRISE_URL: 'http://my-gatling-instance.my-domain.tld'
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
```
