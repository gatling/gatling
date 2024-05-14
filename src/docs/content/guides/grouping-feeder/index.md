---
title: Grouping Feeder records
seotitle: Grouping records in a Gatling Feeder
description: Group different records from a Gatling Feeder.
lead: Group different records from a Gatling Feeder.
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Use Case

Assuming you have a feeder file that contains data where records must be grouped by virtual users, such as:

```csv
username,url
user1,url1
user1,url2
user2,url3
user2,url4
```

You want to make sure *user1* will pick *url1* and *url2* while *user2* will pick *url3* and *url4*.

## Suggested Solution

The idea here is to use [`readRecords`]({{< ref "/reference/script/core/session/feeders#read-records" >}}) to load all the csv file records in memory so you can group them the way you want.

{{< include-code "grouping-feeder" >}}
