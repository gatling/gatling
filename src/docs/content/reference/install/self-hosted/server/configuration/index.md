---
title: Configuration
seotitle: How to configure Gatling Enterprise Self-Hosted
description: First time configuration of Gatling Enterprise and content of the default configuration file.
lead: First time configuration of Gatling Enterprise and content of the default configuration file.
date: 2021-03-26T17:57:29+01:00
lastmod: 2021-08-16T17:55:36+02:00
---

## Configuring Gatling Enterprise

The first step before using Gatling Enterprise is to configure your license key.

{{< img src="configuration.png" alt="License key configuration" >}}

Once you've filled your license and clicked on the "Next" button you will get the credentials to connect to the superAdmin account. You can change this password in the `frontline.conf` file.

{{< img src="adminCredentials.png" alt="Admin credentials" >}}

Click on the "Next" button to finish the configuration step and restart Gatling Enterprise.

## Default Configuration File

Find below the default `frontline.conf` file:

{{< include-code "frontline.conf" hocon >}}

{{< alert warning >}}
Since 1.14.0, the `frontline.cassandra` configuration object uses the standard configuration keys from the Cassandra
Java driver (except for `gatling-keyspace`, `replication` and `runsCleanup`). The previous configuration keys (now
deprecated) are still supported for backward compatibility, but the two configuration styles are not compatible with
each other. We recommend fully migrating to the new style.
{{< /alert >}}
