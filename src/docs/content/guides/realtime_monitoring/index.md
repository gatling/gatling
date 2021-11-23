---
title: "Realtime Monitoring"
description: "Analyze reports metrics as it is happening"
lead: "Analyze reports metrics as it is happening"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 3050000
---

## Introduction

By default, Gatling only provides live feedback in the console output, and generates static HTML reports.

## Gatling Enterprise

[Gatling Enterprise](https://gatling.io/enterprise/), formerly known as Gatling FrontLine, is a management interface for Gatling, that includes advanced metrics and advanced features for integration and automation.

Amongst other features like clustering support, MQTT support, advanced integration with CI tools (Jenkins, TeamCity and Bamboo) and with Grafana,
Gatling Enterprise offers enterprise-grade realtime monitoring and metrics persistence.

{{< img src="enterprise.png" alt="Gatling Enterprise" >}}

For more information, please get in touch at **contact@gatling.io**.

## Graphite Integration

Gatling can export live metrics over the Graphite `plaintext` protocol.

In your `gatling.conf`, you have to add the `graphite` to the data writers and specify the target host:

```hocon
gatling {
  data {
    writers = [console, file, graphite]

    graphite {
      host = "localhost"
      port = 2003
      #light = false              # only send the all* stats
      #protocol = "tcp"           # the protocol used to send data to Carbon (currently supported : "tcp", "udp")
      #rootPathPrefix = "gatling" # the root prefix of the metrics
      #bufferSize = 8192          # internal data buffer size, in bytes
      #writePeriod = 1            # write period, in seconds
    }
  }
}
```

Gatling will send the following metrics:

```
// percentilesXX: XX is the value defined in gatling.conf under percentile(1|2|3|4)
gatling.((groups.)*.request|allRequests).(ok|ko|all).(count|min|max|mean|stdDev|percentilesXX)
gatling.users.(<scenario>|allUsers).(active|waiting|done)"
```

Please check your tool of choice's documentation, eg:
* Prometheus' [Graphite Exporter](https://github.com/prometheus/graphite_exporter)
* InfluxDB Telegraph's [Graphite input](https://docs.influxdata.com/telegraf/v1.20/data_formats/input/graphite/)

{{< alert warning >}}
As explained in [one of our blog posts](/2018/11/metrics-analysis-part-1-mean-standard-deviation/), Graphite and InfluxDB can't store distributions but only numbers. As a result, only one-second-resolution non-aggregated response time stats are correct.

**All aggregations will result in computing averages on percentiles and will inherently be broken.**

This is not a limitation of Gatling, but a limitation of those time series databases.
{{< /alert >}}
