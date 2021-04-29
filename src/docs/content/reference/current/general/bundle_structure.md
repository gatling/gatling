---
title: "Bundle Structure"
description: "Structure of the Gatling Bundle"
lead: "Explanation of the bin, conf, lib, results and user-files directories of the Gatling Bundle"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

The bundle structure is as following:

* `bin` contains the launch scripts for Gatling and the Recorder.
* `conf` contains the configuration files for Gatling, Akka and Logback.
* `lib` contains the binaries used by Gatling
* `user-files`:
  * `simulations` contains your Simulations scala files. Please respect package folder hierarchy.
  * `resources` contains feeder files and templates for request bodies.
* `results` contains `simulation.log` and reports generated in a sub directory.
