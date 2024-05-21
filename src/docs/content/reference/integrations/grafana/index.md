---
menutitle: Grafana Datasource
title: Grafana Datasource for Gatling Enterprise
description: Learn how to configure the Gatling Enterprise Grafana datasource to display your Gatling Enterprise simulation reports.
lead: Display Gatling Enterprise simulation reports in Grafana.
badge:
  type: enterprise
  label: Enterprise
date: 2021-03-08T12:50:08+00:00
lastmod: 2021-08-05T13:13:30+00:00
---

## Requirements

Download and install [Grafana](http://grafana.org/download/).

## Grafana datasource installation

The Gatling Enterprise datasource for Grafana is packaged as a ZIP bundle and can be downloaded by clicking on the
following button:

{{< button title="Download Grafana Datasource" >}}
https://downloads.gatling.io/releases/frontline-grafana-bundle/{{< var grafanaPluginVersion >}}/frontline-grafana-bundle-{{< var grafanaPluginVersion >}}-bundle.zip
{{< /button >}}

You can install it using the `grafana-cli`:

```shell
grafana-cli --pluginUrl GRAFANA_DATASOURCE_BUNDLE_URL plugins install frontline
```
Please note that the plugin is unsigned so recent versions of Grafana may reject it by default. In this case, [you have to add the plugin to the list of allowed unsigned plugin](https://grafana.com/docs/grafana/latest/administration/configuration/#allow_loading_unsigned_plugins).

Please edit the existing line with `allow_loading_unsigned_plugins` in `defaults.ini` and add `frontline` to the comma-separated list, eg:

```
allow_loading_unsigned_plugins = frontline
```

## Adding the datasource

- In the Grafana side menu you will find the `Data Sources` link under `Configuration` (link name may depend on the grafana version).

{{< img src="datasource-menu.png" alt="" >}}

{{< alert tip >}}
If this link is missing in the side menu, it means that your current user does not have the `Admin` role for the current organization.
{{< /alert >}}

- Click the **Add data source** link in the top header.
- Select **Gatling Enterprise**.

| Name      | Description                                                                                                                                            |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Name      | The datasource name. Type `Gatling Enterprise` if you want to use our samples.                                                                         |
| Default   | Should be checked if you want that datasource to be selected by default in new panels.                                                                 |
| URL       | URL of your Gatling Enterprise server (`https://cloud.gatling.io` if using Gatling Enterprise Cloud).                                                  |
| Access    | Server  access via Grafana backend, Browser  access directly from browser.                                                                             |
| Auth      | Gatling Enterprise datasource ignore these fields.                                                                                                     |
| API Token | [API token]({{< ref "../execute/cloud/admin/api-tokens" >}}) needed to authenticate to Gatling Enterprise. The API token needs the Read permission. |

{{< img src="configure-datasource.png" alt="pdfheight=20%" >}}

## Templating

{{< alert tip >}}
Gatling Corp provides some dashboards that you can import if you don't want to bother about setting up all the charts and these template variables.
{{< /alert >}}

{{< alert tip >}}
Samples are in the `dashboardSamples` directory in your Grafana bundle.
They are built with a datasource named *Gatling Enterprise*. Make sure this datasource exists or modify the json file accordingly.
{{< /alert >}}

To use the Gatling Enterprise datasource in Grafana, you will need to set template variables.

These are global dashboard parameters that can be used in your graphs with the query builder.

To create a variable on your dashboard, Click the settings button on the top right menu of the dashboard, go to the Variables tab and click on the `+ New` button.

For example, to create the simulation template variable and use them in your graphs:

{{< img src="templating-edit.png" alt="" >}}

You will need for the scopes to use `Custom` type as shown below :

{{< img src="templating-edit-custom.png" alt="" >}}

At the end, your template variables should be something like:

{{< img src="templating.png" alt="" >}}

{{< img src="dashboard.png" alt="" >}}

## Gatling Enterprise Metrics

{{< alert tip >}}
You can get the list of metrics and test the results via Gatling Enterprise's Public API.
See either [Gatling Enterprise Cloud documentation]({{< ref "../execute/cloud/user/api" >}}) or
[Gatling Enterprise Self-Hosted documentation]({{< ref "../execute/self-hosted/user/api" >}}).
{{< /alert >}}

Gatling Enterprise offers a large amount of metrics:

- **Requests metrics:**
  - `req.<Percentiles>`: Response time percentiles metric.
  - `requests`: Requests count per second.
  - `responsesOk`: Responses OK count per second.
  - `responsesKo`: Responses KO count per second.
  - `responsesByStatus`: Requests count by status per second.
  - `errors`: Errors count by error name per second.
- **Group metrics:**
  - `groupCumulated.<Percentiles>`: Group cumulated response time percentiles.
  - `groupDuration.<Percentiles>`: Group duration percentiles metric.
  - `groupCountsOk`: Group count OK per second.
  - `groupCountsKo`: Group count KO per second.
- **Users metrics:**
  - `usr+`: Users arrival count per second.
  - `usr-`: Users termination count per second.
  - `usrActive`: Concurrent users count per second.
- **Load Generators metrics:**
  - `gcCount`: GC count per second.
  - `gcTime`: GC time per second.
  - `heapUsed`: Heap used in Mb.
  - `heapMax`: Heap max in Mb.
  - `heapCommitted`: Heap committed in Mb.
  - `tcpSeg`: TCP events count per second.
  - `tcpConn`: TCP connection events count per second.
  - `cpuSys`: CPU system usage in percent.
  - `cpuUser`: CPU user usage in percent.
- **Connections metrics:**
  - `bitsSent`: Bits sent per second.
  - `bitsReceived`: Bits received per second.
  - `connectionOpened`: Connection opened count per second.
  - `connectionClosed`: Connection closed count per second.
  - `connectionTcpState`: TCP connection count by state.
  - `tcp.<Percentiles>`:  TCP connect duration percentiles metric.
  - `tls.<Percentiles>`: TLS handshake duration percentiles metric.
- **DNS metrics:**
  - `dns.<Percentiles>`: DNS resolution duration percentiles metric.

{{< alert tip >}}
The returned percentiles are: min, p25, p50, p75, p80, p85, p90, p95, p99, p999, p9999, max, mean and pAll. (pAll is useful if you want to display in the same graph).
{{< /alert >}}

## Metric Requirements

### Part 1

| Metrics name                   | simulation         | scenario           | group              | request            |
|--------------------------------|--------------------|--------------------|--------------------|--------------------|
| `req.<Percentiles>`            | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| `requests`                     | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| `responsesOk`                  | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| `responsesKo`                  | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| `responsesByStatus`            | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| `errors`                       | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| `groupCumulated.<Percentiles>` | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |                    |
| `groupDuration.<Percentiles>`  | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |                    |
| `groupCountsOk`                | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |                    |
| `groupCountsKo`                | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |                    |
| `usr+`                         | {{< icon check >}} |                    |                    |                    |
| `usr-`                         | {{< icon check >}} |                    |                    |                    |
| `usrActive`                    | {{< icon check >}} |                    |                    |                    |
| `gcCount`                      | {{< icon check >}} |                    |                    |                    |
| `gcTime`                       | {{< icon check >}} |                    |                    |                    |
| `heapUsed`                     | {{< icon check >}} |                    |                    |                    |
| `heapMax`                      | {{< icon check >}} |                    |                    |                    |
| `heapCommitted`                | {{< icon check >}} |                    |                    |                    |
| `tcpSeg`                       | {{< icon check >}} |                    |                    |                    |
| `tcpConn`                      | {{< icon check >}} |                    |                    |                    |
| `cpuSys`                       | {{< icon check >}} |                    |                    |                    |
| `cpuUser`                      | {{< icon check >}} |                    |                    |                    |
| `bitsSent`                     | {{< icon check >}} |                    |                    |                    |
| `bitsReceived`                 | {{< icon check >}} |                    |                    |                    |
| `connectionOpened`             | {{< icon check >}} |                    |                    |                    |
| `connectionClosed`             | {{< icon check >}} |                    |                    |                    |
| `connectionTcpState`           | {{< icon check >}} |                    |                    |                    |
| `tcp.<Percentiles>`            | {{< icon check >}} |                    |                    |                    |
| `tls.<Percentiles>`            | {{< icon check >}} |                    |                    |                    |
| `dns.<Percentiles>`            | {{< icon check >}} |                    |                    |                    |

### Part 2

| Metrics name                   | remote             | hostname           | load generator     | scope              |
---------------------------------|--------------------|--------------------|--------------------|--------------------|
| `req.<Percentiles>`            |                    |                    |                    | {{< icon check >}} |
| `requests`                     |                    |                    |                    |                    |
| `responsesOk`                  |                    |                    |                    |                    |
| `responsesKo`                  |                    |                    |                    |                    |
| `responsesByStatus`            |                    |                    |                    |                    |
| `errors`                       |                    |                    |                    |                    |
| `groupCumulated.<Percentiles>` |                    |                    |                    | {{< icon check >}} |
| `groupDuration.<Percentiles>`  |                    |                    |                    | {{< icon check >}} |
| `groupCountsOk`                |                    |                    |                    |                    |
| `groupCountsKo`                |                    |                    |                    |                    |
| `usr+`                         |                    |                    |                    |                    |
| `usr-`                         |                    |                    |                    |                    |
| `usrActive`                    |                    |                    |                    |                    |
| `gcCount`                      |                    |                    | {{< icon check >}} |                    |
| `gcTime`                       |                    |                    | {{< icon check >}} |                    |
| `heapUsed`                     |                    |                    | {{< icon check >}} |                    |
| `heapMax`                      |                    |                    | {{< icon check >}} |                    |
| `heapCommitted`                |                    |                    | {{< icon check >}} |                    |
| `tcpSeg`                       |                    |                    | {{< icon check >}} |                    |
| `tcpConn`                      |                    |                    | {{< icon check >}} |                    |
| `cpuSys`                       |                    |                    | {{< icon check >}} |                    |
| `cpuUser`                      |                    |                    | {{< icon check >}} |                    |
| `bitsSent`                     | {{< icon check >}} |                    |                    |                    |
| `bitsReceived`                 | {{< icon check >}} |                    |                    |                    |
| `connectionOpened`             | {{< icon check >}} |                    |                    |                    |
| `connectionClosed`             | {{< icon check >}} |                    |                    |                    |
| `connectionTcpState`           | {{< icon check >}} |                    |                    |                    |
| `tcp.<Percentiles>`            | {{< icon check >}} |                    |                    | {{< icon check >}} |
| `tls.<Percentiles>`            | {{< icon check >}} |                    |                    | {{< icon check >}} |
| `dns.<Percentiles>`            |                    | {{< icon check >}} |                    | {{< icon check >}} |
