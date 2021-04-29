---
title: "Timings"
description: "Gatling records the timings of multiple metrics used to analyze your results"
lead: "Learn about all the recorded metrics: active users, response times and counts"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

As Gatling runs and executes requests, several timings are recorded, which make up the basis of all forms of reporting in Gatling: console, HTML reports, etc...

## Active Users

At a given second, active users are computed as:

```
  (the number of active users at the previous second)
+ (the number of users who started during this second)
- (the number of users who finished during the previous second)
```

## Requests

### Response Time

The response time is the elapsed time between the instant a request is sent and the instant the complete response is received:

* The beginning of the request's sending is the instant when the connection to the target host has been established or grabbed from the pool.
* The end of the response's receiving is the instant when the whole response (status, headers and body) has been received by Gatling

## Groups

### Count

The counts are the number of group executions, not the sum of the counts of each individual request in that group.

### Response Time

The response time of a group is the cumulated response times of each individual request in that group.

{{< alert tip >}}
When dealing with embedded resources (inferred or explicitly set), the behaviour is slightly different : as resources are fetched asynchronously,
the cumulated response time for embedded resources starts from the beginning of the first resource request to the end of the last resource request.
{{< /alert >}}

### Duration

Group duration is the elapsed time between the instant a virtual user enters a group and the instant it exits.

Group duration is reported in the "Duration" charts.

### Cumulated Response Time

Group cumulated response time is the time in a group when requests are flying: requests' response time and resources start to end duration.
In short, it's the group duration minus the pauses.

Group cumulated response time is reported in the "Cumulated Response Time" charts.
