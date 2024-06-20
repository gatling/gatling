---
title: Trends
description: Learn how to compare test results.
lead: Compare test results.
date: 2021-03-10T14:29:43+00:00
lastmod: 2021-08-05T13:13:30+00:00
badge:
  type: enterprise
  label: Enterprise
---

## Run / Trends

The runs list and trends for a simulation can be accessed by clicking on the {{< icon history >}} icon in the [simulations table]({{< ref "/reference/execute/cloud/user/simulations#simulations-table" >}}).

This view contains the list of your simulation's runs which can be filtered by status and the trends which are displaying information between those runs.
{{< img src="trends-overview.png" alt="Run trends" >}}

### Simulation runs

{{< img src="run-table.png" alt="Run table" >}}

As with the results of the latest run in the [simulations table]({{< ref "/reference/execute/cloud/user/simulations#simulations-table" >}}), you
can access the [logs]({{< ref "/reference/execute/cloud/user/simulations#logs" >}}) of the run by clicking on the status badge. 

{{< img src="logs-badge.png" alt="Logs badge" >}}

{{< alert warning >}}
The logs are only available for runs which are not flagged as "Successful".
{{< /alert >}}

If you defined assertions, you can display them by also clicking on the badge:

{{< img src="assertions-badge.png" alt="Assertions badge" >}}

You can delete runs by selecting them and clicking on the **Delete** button in the action bar above the table.

You can also click on the {{< icon gear >}} icon to see the properties of the run. 

{{< img src="snapshot.png" alt="Snapshot" >}}

{{< alert warning >}}
The Java system properties beginning with `sensitive.` and environment variables beginning with `SENSITIVE_` will not be displayed.
{{< /alert >}}

### Run Comparison

{{< img src="compare-runs.png" alt="Compare runs" >}}

You can compare the results of two runs if you click on the "Compare runs" button in the table. It allows you to compare the response time and errors of the two runs for each request.

You can choose the specific metric you want to compare by clicking on the metric name, and the specific runs you want to compare by clicking on the run number.

The delta and variance will be displayed, so you can check if there is a progression or a degradation in performance.

### Trends charts

The trends are charts that will display some global statistics for each run (eg: requests count) so that you can easily see how well your runs went compared to each other.
Each run is represented by its number in the chart and the chart won't display the statistics of a failed run (eg: Timeout, broken, etc..).

{{< img src="trends.png" alt="Trends" >}}

Using the buttons above the charts on the left, you can change the statistics shown by filtering through scenarios, groups or requests that are involved in each run.
You can choose how many runs will be compared by changing the limit (10, 25, 50, 100):

{{< img src="trends-bar.png" alt="Trends bar" >}}
