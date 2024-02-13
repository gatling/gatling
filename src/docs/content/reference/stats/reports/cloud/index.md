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

The reports can be accessed by clicking on the {{< icon chart-area >}} icon in the [simulations table]({{< ref "/reference/execute/cloud/user/simulations#simulations-table" >}}) or in the [runs table]({{< ref "../trends/cloud#runs-table" >}}).

This view displays all of the metrics available for a specific run.
The Reports page consists of:

- [The run bar]({{< ref "#run-bar" >}})
- [The top navigator menu]({{< ref "#top-navigator-menu" >}})
- [Assertions]({{< ref "#assertions" >}})
- [The timeline]({{< ref "#timeline" >}})
- [Tabs]({{< ref "#tabs" >}})
- [Filter bar]({{< ref "#filter-bar" >}})
- [Charts area]({{< ref "#charts" >}})
- [The summary]({{< ref "#summary" >}}) (only for requests and groups tabs)
- [Public links]({{< ref "#shareable-links" >}})
- [Print to PDF]({{< ref "#print-to-pdf" >}})
- [Tips]({{< ref "#tips" >}})

{{< img src="reports.png" alt="Reports" >}}

## Run Bar

This bar is a combination of buttons including:

- **Start / Stop**: Start a new run of the simulation, or stop the ongoing run
- **Share**: Create a [shareable link]({{< ref "#shareable-links" >}}) to share this reports to anyone you want.
- **Print to PDF**: [Print a PDF]({{< ref "#print-to-pdf" >}}) of your reports

{{< img src="run-bar.png" alt="Run bar" >}}

## Top Navigator Menu

The navigation menu allows you to choose the simulation time range. You can also view a custom time range by dragging the cursors. 

{{< img src="timewindow.png" alt="Timewindow" >}}

## Timeline

The timeline contains metrics of the full run providing an overview of the run.
Global information are available such as the resolution and the simulation name.

The resolution indicates the number of seconds per data point in the graph.

You can change the time range with control buttons or by selecting a region on the timeline:

{{< img src="timeline.png" alt="Timeline" >}}

## Assertions

The image below shows the status of the simulation (ongoing, successful, timeout...).
If your simulation has some assertions configured, you can click this label.
You can add comments to the run by clicking on the {{< icon comment-alt >}} icon.

{{< img src="timeline-assertions.png" alt="Timeline assertions" >}}

## Tabs

Below the navigator chart, there are tabs to switch charts.
Each tab has the same structure except the summary that is available only for requests and groups tabs.

{{< img src="tabs.png" alt="Tabs" >}}

## Filter Bar

This filter bar is a combination of buttons:

- **Switch to Summary**: Switch to [summary]({{< ref "#summary" >}}) view for Requests & Groups tabs
- buttons to filter the metrics displayed in the charts area

{{< img src="filter-bar.png" alt="Filter bar" >}}

## Charts

All charts in Gatling Enterprise runs are interactive, so if you select a time window on a chart it will automatically change the time window
for all other charts. Metrics are drawn in multiple charts.

{{< img src="charts.png" alt="Charts" >}}

Some of them have an icon to update the chart settings:

{{< img src="distrib-chart.png" alt="Distribution chart" >}}

You can also switch from area to pie and column charts using the icon.

{{< img src="pie-button.png" alt="Pie button" >}}

{{< alert warning >}}
If your kernel version is too low (around below 3.10) you might not be able to get data from the TCP connection by state graph on the Connections tab. If you want to access this data, you should upgrade your kernel.
{{< /alert >}}

## Summary (Requests and Groups only) {#summary}

This view is available only from the Requests and Groups tabs.
The summary of the data which is laid out in this table which can be viewed in flat mode (default) or hierarchy mode
The summary is also connected to the timeline and the time window selected, so if you change the time window the summary
will refresh the data to match the time window.

In flat mode, you can click on the title of each column to arrange the data in ascending or descending order. 

{{< img src="summary.png" alt="Summary" >}}

### Download CSV

In order to download summaries as CSV for later use, you can click the download button on the top right side of the summary

{{< img src="download-summary.png" alt="Download-summary" >}}

CSV files are generated according to the selected **Scenario**

{{< img src="download-summary-select-scenario.png" alt="Download-summary-select-scenario" >}}

When downloading a group level summary, you will receive data for both **Duration** and **Cumulated response time**.


## Generate Shareable Links {#shareable-links}

A shareable link is a link of the current reports which will be accessible to anyone, without having to log in to Gatling Enterprise. To generate a shareable link, click on the *Share* button and choose the expiration date of your link.

{{< img src="generate-public-link.png" alt="Generate public links" >}}

The maximum allowed lifetime for a shareable link is 1 year.

Choose an expiration date, then click the generate button.

{{< img src="successful-generation-public-link.png" alt="Successful public link generation" >}}

You can copy the shareable link to share your report to non-Gatling Enterprise users, or click on the "Go" Button to access it yourself. You can click on the "OK" button to close this modal.

## Print to PDF

When clicking on the blue "Print to PDF" button in the navigator menu, you will have access to a page where you can configure and then print a PDF report of a specific simulation run.

{{< img src="run-bar.png" alt="Print to PDF button" >}}

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

This page is a configurable list of different elements that will be displayed in the report. You can click on the blue add button under every element
to add another one.

Every element can be moved up or down by clicking on the blue arrow on the top right of the element, or be removed by clicking on the red dash.

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

After adding all desired elements in the report you can click on the *Preview & Print to PDF* button on the top right to get your PDF file.

{{< img src="export-actions.png" alt="Export actions" >}}

There are two more actions available to you:

- **Save**: saves the current Export configuration:
  - **as a template**: this option will save the element list without the content
  - **as a save**: this option will save everything, including the content of the Text Area and the configuration of the graphs
- **Load**: load a previously saved template or save.

## Useful Tips {#tips}

### Zoom

You can reset the zoom status by double clicking on a chart.
It is possible to change the time range window by doing any of the following actions:

- Clicking the zoom icons of the control buttons
- Selecting a zone in any charts or timelines
- Selecting a range of time from the top navigation bar

### Markers

To ease your analysis, you can create markers on all the charts by right clicking on them. You can click on the top of the marker to delete it.

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
