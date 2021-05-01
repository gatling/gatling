---
title: "Reports"
description: "Analyze your reports"
lead: "Analyze your reports thanks to the indicators, active users and requests / responses over time, and distribution"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Overview

Global menu points to consolidated statistics.

Details menu points to per-request-type statistics.

{{< alert tip >}}
You can use the `-ro` option to generate reports from a truncated simulation.log file, for example when the run was interrupted (Ctrl+C or process killed).

Note that the component in charge of logging into the simulation.log file uses a buffer, so last data might be missing if you forcefully interrupt.
See [Configuration page]({{< ref "../configuration#cli-options" >}}).
{{< /alert >}}

## Overall Simulation charts

Most of those charts are available for both the overall simulation report and for per request/group charts.

### Indicators

{{< img src="charts-indicators.png" alt="DetailsIndicators" >}}

This chart shows how response times are distributed among standard ranges.
The right panel show number of OK/KO requests.

{{< alert tip >}}
these ranges can be configured in the `gatling.conf` file.
{{< /alert >}}

{{< img src="charts-statistics.png" alt="Statistics" >}}

The top panel shows some standard statistics such as min, max, average, standard deviation and percentiles globally and per request.

{{< alert tip >}}
these percentiles can be configured in the `gatling.conf` file.
{{< /alert >}}

{{< alert tip >}}
If your scenario contains groups, this panel becomes a tree : each group is a non leaf node, and each request is a descendant leaf of a group. Group timings are by default the cumulated response times of all elements inside the group. Group duration can be displayed instead of group cumulated response time by editing the `gatling.conf` file.
{{< /alert >}}

The bottom panel shows some details on the failed requests.

### Active users over time

{{< img src="charts-users.png" alt="ActiveUsers" >}}

This chart displays the active users during the simulation : total and per scenario.

"Active users" is neither "concurrent users" or "users arrival rate".
It's a kind of mixed metric that serves for both open and closed workload models and that represents "users who were active on the system under load at a given second".

It's computed as:

```
  (number of alive users at previous second)
+ (number of users that were started during this second)
- (number of users that were terminated during previous second)
```

### Response time distribution

{{< img src="charts-distrib.png" alt="ResponseTimeDistribution" >}}

This chart displays the distribution of the response times.

### Response time percentiles over time

{{< img src="charts-response-percentiles-per-sec.png" alt="ResponseTimePercentilesPerSecond" >}}

This charts displays a variety of response time percentiles over time, but only for successful requests.
As failed requests can end prematurely or be caused by timeouts, they would have a drastic effect on the percentiles computation.

### Requests per second over time

{{< img src="charts-requests-per-sec.png" alt="RequestsPerSecond" >}}

This chart displays the number of requests sent per second over time.

### Responses per second over time

{{< img src="charts-responses-per-sec.png" alt="reports/charts-responses-per-sec.png" >}}

This chart displays the number of responses received per second over time : total, successes and failures.

## Request/group specific charts

Those charts are only available when consulting the details for a request/group.

### Response Time against Global RPS

{{< img src="charts-response-time-global-rps.png" alt="ResponseTimeOverLoad" >}}

This chart shows how the response time for the given request is distributed, depending on the overall number of request at the same time.
