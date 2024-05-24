---
title: Cloud
description: Learn how to analyze test reports and discover the slow parts of your application.
lead: Analyze your reports and discover the bottlenecks in your application.
badge:
  type: enterprise
  label: Enterprise
date: 2021-03-10T14:29:43+00:00
lastmod: 2023-10-12T09:41:48+00:00
---

Click on the {{< icon chart-area >}} icon in the [simulations table]({{< ref "/reference/execute/cloud/user/simulations#simulations-table" >}}) or in the [runs table]({{< ref "../trends/cloud#runs-table" >}}) to access the reports.

This view displays all the metrics available for a specific run.

The page is split into a [header]({{< ref "#header" >}}) displaying the run title with the general actions, and the three main following sections:

- [Summary]({{< ref "#summary" >}})
- [Report]({{< ref "#report" >}})
- [Logs]({{< ref "#logs" >}})

## Summary

The main objective of this tab is to provide a brief overview of your run performance.

The first section gives you general information about your run configuration and execution:

{{< img src="summary-general-section.png" alt="Summary general section" >}}

You can add a description to your run by clicking the **Edit** button and then **Save**.

### Main KPIs

This section displays your run main KPIs:

{{< img src="summary-main-kpis.png" alt="Summary main KPIs" >}}

- Error ratio: the ratio of responses with errors compared with the total number, in percent
- Total requests: the total number of requests made during the run
- Max. concurrent V.U: the maximum number of concurrent virtual users during the run
- P95 response time: the response time of one of the requests, such that 95% of the requests were faster and only 5% were slower

### Assertions

Assertions are the run's acceptance criteria. It allows you to quickly know if your requirements are met or not.
After having observed the first run(s) results, you will be able to define your criteria to validate a simulation.

Gatling gives you the ability to create your own custom assertions thanks to [our DSL]({{< ref "../../script/core/assertions" >}}).

Each assertion has a status (successful or failed) that let you know which part of your run met your requirements.

{{< img src="summary-assertions.png" alt="Summary assertions" >}}

### Request and response charts

The following section contains a global overview of your requests and responses per second and the response time percentiles:

{{< img src="summary-charts.png" alt="Summary charts" >}}

{{< alert info >}}
Tip: you can right-click on any chart to place pins that will also be visible in the "Report" tab. They are often useful to highlight sections of your run that will need to be studied more thoroughly.
{{< /alert >}}


### Errors list

The final section contains the details of every error that occurred during your run:

{{< img src="summary-errors.png" alt="Summary errors" >}}


## Report

Gatling Reports gives you all the information you need regarding the run execution: details about the injection profile, results including advanced metrics in various charts and formats to allow deep analysis, and also the possibility to export the raw data for custom reporting.

All metrics and charts are split into the following panels:
- [Requests]({{< ref "#requests" >}})
- [Groups]({{< ref "#groups" >}}) (if you defined some)
- [Users]({{< ref "#users" >}})
- [Connections]({{< ref "#connections" >}})
- [DNS]({{< ref "#dns" >}})
- [Load Generators]({{< ref "#load-generators" >}})

Using the request timeline at the top of the page, you can select your run time range to review. 
Either select a predefined time range from the dropdown menu or drag and drop on the chart the desired area.

{{< img src="report-timeline.png" alt="Report timeline" >}}

### Requests

The requests panel allows you to review how your requests and responses performed over time.
You can add scenarios, groups and requests filtering options to filter by your criteria.

You can visualize your run execution using charts:

{{< img src="report-requests-charts.png" alt="Report requests charts" >}}

or using tabular data:

{{< img src="report-requests-table.png" alt="Report requests table" >}}

{{< alert info >}}
This table is also connected to the timeline and the time window selected, so if you change the time window the table will refresh the data to filter it.
{{< /alert >}}

You can download the table as a CSV file for later use. To do so, you can click the download button on the top right side of the table.

{{< img src="download-summary.png" alt="Download-summary" >}}

CSV files are generated according to the selected **Scenario**

{{< img src="download-summary-select-scenario.png" alt="Download-summary-select-scenario" >}}

When downloading a group level summary, you receive data for both **Duration** and **Cumulated response time**.


### Groups

If you defined [groups]({{< ref "../../script/core/scenario#groups" >}}) in your simulation, you will be able to break down your results per each one of them.

Once again like the requests panel, you can visualize your run execution using charts:

{{< img src="report-groups-charts.png" alt="Report groups charts" >}}

or using tabular data:

{{< img src="report-groups-table.png" alt="Report groups table" >}}

{{< alert info >}}
This table is also connected to the timeline and the time window selected, so if you change the time window the table will refresh the data to filter it.
{{< /alert >}}


You can download the table as a CSV file for later use. To do so, you can click the download button on the top right side of the table.

{{< img src="download-group-csv.png" alt="Download group csv" >}}

CSV files are generated according to the selected **Scenario**

{{< img src="download-group-select-scenario.png" alt="Download-group-select-scenario" >}}

When downloading a group-level summary, you receive data for both **Duration** and **Cumulated response time**.


### Users

The users panel shows how your virtual users defined in your injection profile where injected over time in your simulation.

{{< img src="report-users.png" alt="Report Users panel" >}}

### Connections

The connections panel shows metrics regarding the number of socket connections, TCP events, and TLS handshakes over time.

{{< img src="report-connections.png" alt="Report Connections panel" >}}

{{< alert warning >}}
If your kernel version is too low (below 3.10) you might not be able to get data from the TCP connection by state chart. If you want to access this data, you should upgrade your kernel.
{{< /alert >}}

### DNS

The DNS panel shows metrics related to DNS requests made during the run execution.

{{< img src="report-dns.png" alt="Report DNS panel" >}}

### Load Generators

This panel shows metrics related to the CPU, heap, GC, and TCP events of the load generators VMs used for your run and configured in your [simulation]({{< ref "../../execute/cloud/user/simulations#creating-a-simulation" >}}).

{{< img src="report-load-generators.png" alt="Report Load Generators panel" >}}


## Logs

This tab allows you to read logs from your simulation and load generators while your run is ongoing or when it failed.

{{< img src="report-logs.png" alt="Report Logs" >}}

{{< alert warning >}}
Logs are archived and are inaccessible for successfully completed runs.
{{< /alert >}}


## Header

The page header lets you edit your run title by clicking the {{< icon pencil >}} icon and gives you access to general actions:

- [Public links]({{< ref "#shareable-links" >}})
- [Print to PDF]({{< ref "#print-to-pdf" >}})
- [View properties]({{< ref "#properties" >}})


### Generate Shareable Links {#shareable-links}

A shareable link is a link to the current report that is accessible to anyone without having to log in to Gatling Enterprise. To generate a shareable link, click on the **Share** button in the **Actions** dropdown at the top right of your page and choose the expiration date of your link.

{{< img src="report-export-button.png" alt="Report export button" >}}

{{< img src="generate-public-link.png" alt="Generate public links" >}}

The maximum allowed lifetime for a shareable link is 1 year.

Choose an expiration date, then click the generate button.

{{< img src="successful-generation-public-link.png" alt="Successful public link generation" >}}

You can copy the shareable link to share your report to non-Gatling Enterprise users, or click on the **Shareable link** itself to access it. Click on the **OK** button to close this modal.

### Print to PDF

The "Print to PDF" option lets you create your own custom report. This custom report is visible online or in a PDF file that you can easily save and share.

All metrics and charts displayed in the [Report tab]({{< ref "#report" >}}) are available for your custom report. You can also create reusable templates for your future needs.

Click the "Print to PDF" button in the **Actions** dropdown to access the configure and print PDF page.

{{< img src="report-print-button.png" alt="Print to PDF button" >}}

This report is initialized with:

- a title element with the date of the run you were coming from
- the run status
- the run comments
- the run assertions
- the run requests summary
- 3 charts of the run:
  * Requests and Responses per second
  * Responses per Second by Status
  * Response Time Percentiles

{{< img src="export-page.png" alt="Print to PDF page" >}}

This page is a configurable list of different elements that will be displayed in the report. You can click on the **Add block** button under every element
to add another one.

Every element can be moved up or down by clicking on the arrows on the top right of the element, or be removed by clicking on the red trash icon.

Those elements are composed of:

- **Title**: adds a title element.
- **Text Area**: adds an editable text element.
- **New Page**: allows you to skip a page in the report.
- **Run**:
  - **Status**: adds an editable text element with a predefined text set to the status of the selected run.
  - **Comments**: adds an editable text element with a predefined text set to the comments of the selected run.
  - **Assertions**: adds a table with the assertions of the selected run.
  - **Summary**: adds the summary table of the selected run in a new landscape page.
- **Chart**: adds a chart element that you can interact with before exporting it to PDF.
- **Counts**: adds a count chart element that you can interact with before exporting it to PDF.

As you can see below, every chart (or other element) can be interacted with individually. You can zoom in on sections, or select the run, the scenario,
the group, etc.. whose data you want to fetch. You do not need to have the same settings for each element.

{{< img src="export-charts.png" alt="Export charts" >}}

After adding all desired elements in the report you can click on the **Preview & Print to PDF** button on the top right or the page to get your PDF file.

{{< img src="export-actions.png" alt="Export actions" >}}

There are two more actions available to you:

- **Save**: saves the current Export configuration:
  - **as a template**: this option will save the element list without the content
  - **as a save**: this option saves everything, including the content of the Text Area and the configuration of the graphs
- **Load**: load a previously saved template or save.

### View properties {#properties}

The **View properties** button, located in the top right action bar, shows every property you configured for your run.

{{< img src="report-view-properties-button.png" alt="Run properties" >}}
{{< img src="report-properties.png" alt="Run properties" >}}


## Useful Tips {#tips}

### Zoom

You can reset the zoom status by double-clicking on a chart.
It is possible to change the time range window by doing any of the following actions:

- Clicking the zoom icons of the control buttons
- Selecting a zone in any charts or timelines
- Selecting a range of time from the top navigation bar

### Markers

To ease your analysis, right-click on the charts to create markers. Click on the top of the marker to delete it.

{{< img src="marker.png" alt="Marker" >}}

### Multiple Highlights

In the top right menu, you can activate the *Multiple Highlights* setting which allows the tooltip to be displayed on every chart at the same time.

{{< img src="multiplehighlight.png" alt="Multiple" >}}

### Percentiles Mask

In the top right menu, you can click on the **Percentiles** setting to be able to choose which percentiles to display in the chart.

{{< img src="percentilesmask.png" alt="Percentiles mask" >}}
{{< img src="percentileschart.png" alt="Percentiles chart" >}}

### Date Time / Elapsed Time

In the top right menu, you can activate the **Date Time** setting which lets you switch from elapsed time to date time.

{{< img src="datetime.png" alt="Multiple" >}}

### Highlight Legend

If you hover your mouse over a label on the percentiles chart legend, you will be able to highlight the curve on the chart, leading to a better view of that curve.
The highlight legend option is enabled for every "non stacked" graph.

{{< img src="highlightchart.png" alt="Highlight chart" >}}

### Run comparison

Gatling enterprise also offers the possibility to view and compare reports for each run.

You can either decide to export a PDF report and select for each chart which run it relates to, or you can compare the request response times and error rates using the compare function in the [simulation details page]({{< ref "../trends/cloud#run-comparison" >}}).
