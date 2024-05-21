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
  # Maven 3 and JDK 17; see https://hub.docker.com/_/maven for other tags available
  # See also https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/ci/templates/Maven.gitlab-ci.yml
  # for other useful options for Maven builds.
  image: maven:3-openjdk-17-slim
  script:
    # ci_settings.xml is a Maven settings file included in the repository;
    # the credentials in ci_settings.xml use environment variables, which
    # can be set on the GitLab project.
    # See https://docs.gitlab.com/ee/user/packages/maven_repository/#create-maven-packages-with-gitlab-cicd
    # The GitLab doc example uses a token to publish to GitLab Package
    # Registry, you may need to configure e.g. username and password instead;
    # see https://maven.apache.org/settings.html#servers
    - mvn deploy -s ci_settings.xml

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
