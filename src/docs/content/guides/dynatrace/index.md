---
title: "Dynatrace Integration"
description: "Setting Dynatrace Header"
lead: "Setting Dynatrace Header"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 0036
---

## Use Case

As described in [Dynatrace's documentation](https://www.dynatrace.com/support/help/setup-and-configuration/integrations/third-party-integrations/test-automation-frameworks/dynatrace-and-load-testing-tools-integration/), you can define a custom header in your load tests that you can then parse on the Dynatrace side.

## Suggested Solution

The idea here is to use [`sign`]({{< ref "../../reference/current/http/protocol#sign" >}}) on the HttpProtocol to define a global signing function to be applied on all generated requests.

{{< include-code "dynatrace" java kt scala >}}
