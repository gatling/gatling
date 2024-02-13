```yaml
stages:
  - load-test

run-gatling-enterprise:
  stage: load-test
  image:
    name: gatlingcorp/enterprise-runner:1
    entrypoint: ['']
  script:
    - gatlingEnterpriseStart
  variables:
    # We assume GATLING_ENTERPRISE_API_TOKEN is available,
    # e.g. configured on the GitLab project
    # Specify your simulation ID:
    SIMULATION_ID: '00000000-0000-0000-0000-000000000000'
```
