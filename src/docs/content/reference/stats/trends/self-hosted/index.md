---
title: Self-Hosted
description: Learn how to compare test results.
lead: Compare test results.
date: 2021-03-10T09:29:43-05:00
lastmod: 2021-08-16T17:55:36+02:00
---

## Run / Trends

Runs list and trends can be accessed by clicking on the {{< icon history >}} icon in the [simulations table]({{< ref "/reference/execute/self-hosted/user/simulations#simulations-table" >}}).

This view contains the list of your simulation's runs which can be filtered by name and/or status and the Trends which are displaying information between those runs.
{{< img src="run-trends.png" alt="Run trends" >}}

### Runs table

{{< img src="run-table.png" alt="Run table" >}}

Like the result of the latest run in the [simulations table]({{< ref "/reference/execute/self-hosted/user/simulations#simulations-table" >}}) you have access to the [logs]({{< ref "/reference/execute/self-hosted/user/simulations#logs" >}}) of the run by clicking on the {{< icon file-alt >}} icon and you can sort the table by each columns. The logs are only available for run which are not flagged as "Successful".

If there is one, You can click on the {{< icon search >}} icon next to the status to display the [assertions]({{< ref "../reports/self-hosted#assertions" >}}) of the run.
You can delete runs by selecting them and click on the **Delete** button in the action bar above the table.

You can comment a run by clicking on the {{< icon comment-alt >}} icon on the right side of the table.

{{< img src="comment.png" alt="Comment" >}}

You can also click on the {{< icon info-circle >}} icon to see a snapshot of the run configuration. The Java System Properties beginning with `sensitive.` and environment variables beginning with `SENSITIVE_` are not displayed.

{{< img src="snapshot.png" alt="Snapshot" >}}

### Run Comparison

{{< img src="compare-runs.png" alt="Compare runs" >}}

You can compare the results of two runs if you click on the "Compare runs" button in the table. It allows you to compare the response time and errors of the two runs for each request.

You can choose the specific metric you want to compare by clicking on the metric name, and the specific run you want to compare by clicking on the run number.

The delta and variance will be displayed, so you can check if there is a progression or a degradation in performance.

### Trends charts

The trends are charts that will display some globals statistics for each runs (eg: requests count) so that you can easily see how well your runs went compared to each other.
Each run is represented by his number in the chart and the chart won't display the statistics of a failed run (eg: Timeout, broken, etc.).

{{< img src="trends.png" alt="Trends" >}}

You can filter the statistics shown by filtering through scenarios, groups or requests that are involved in each run.
You can choose how many runs will be compared by changing the limit (10, 25, 50, 100):

{{< img src="trends-bar.png" alt="Trends bar" >}}
