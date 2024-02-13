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
  # JDK 17 from Azul; see https://hub.docker.com/r/azul/zulu-openjdk for other tags available,
  # or use another image configured with a JDK
  # See also https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/ci/templates/Gradle.gitlab-ci.yml
  # for other useful options for Gradle builds.
  image: azul/zulu-openjdk:17-latest
  script:
    # Configure credentials in the build.gradle file using environment
    # variables, which can be set on the GitLab project.
    # See https://docs.gitlab.com/ee/user/packages/gradle_repository/#authenticate-with-a-ci-job-token-in-gradle
    # The GitLab doc example uses a token to publish to GitLab Package
    # Registry, you may need to configure e.g. username and password instead;
    # see https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:authentication_schemes
    - ./gradlew publish

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
