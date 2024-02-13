---
title: Introduction
seotitle: Introduction to Gatling Enterprise Self-Hosted installation
description: Gatling Enterprise architecture overview and conventions.
date: 2021-03-26T16:01:21+01:00
lastmod: 2022-03-17T16:37:43+00:00
---

## Audience and goals

This section is intended for operations people in charge of deploying the Gatling Enterprise components.

It describes Gatling Enterprise's architecture, components, how to install them and what the prerequisites are.

## Architecture overview

{{< img src="gatling-enterprise-architecture.png" alt="Gatling Enterprise Architecture Diagram" >}}

Gatling Enterprise consists of:

- A Cassandra database
- The Gatling Enterprise WebApp:
  * A rich web UI
  * A public REST API
- The Gatling Enterprise Extensions:
  * Continuous Integration plugins for Jenkins, Bamboo and TeamCity or any solution that can trigger a bash script
  * A Grafana Datasource for querying Gatling Enterprise metrics
- Gatling injectors to be used like standard Gatling OSS, with extra features so Gatling Enterprise can collect data from them

## Documentation conventions

In this documentation, you'll find several mentions to some placeholders in capital letters.

### Custom Gatling Enterprise Bundle download link {#custom-bundle-download-link}

Any references to private links that allows downloading the Gatling Enterprise bundle were removed from this documentation and replaced with the following placeholder:

```
REPLACE_WITH_YOUR_CUSTOM_GATLING_ENTERPRISE_BUNDLE_DOWNLOAD_LINK
```

This custom link was shared with your license key by our sales persons with the manager in charge of the contract and the administrators registered for support.

{{< alert warning >}}
This placeholder only makes sense for self-hosted customers. AWS and Azure Marketplace customers spawn a pre-installed virtual machine and already have all the dependencies needed.
{{< /alert >}}
