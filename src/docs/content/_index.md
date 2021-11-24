---
title: "Gatling"
description: "Gatling documentation"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
cascade:
  docsRepo:
    url: https://github.com/gatling/gatling/blob/main/content
    rel: ""
ordering:
  - tutorials
  - reference
  - guides
---

Gatling is a highly capable load testing tool.
It is designed for ease of use, maintainability and high performance.

Out of the box, Gatling comes with excellent support of the HTTP protocol that makes it a tool of choice for load testing any HTTP server.
As the core engine is actually protocol agnostic, it is perfectly possible to implement support for other protocols.
For example, Gatling currently also ships JMS support.

The [quickstart]({{< ref "tutorials/quickstart" >}}) has an overview of the most important concepts, walking you through the setup of a simple scenario for load testing an HTTP server.

Having *scenarios* that are defined in code and are resource efficient are the two requirements that motivated us to create Gatling. Based on an expressive [DSL](http://en.wikipedia.org/wiki/Domain-specific_language), the *scenarios* are self-explanatory. They are easy to maintain and can be kept in a version control system.

Gatling's architecture is asynchronous as long as the underlying protocol, such as HTTP, can be implemented in a non blocking way. This kind of architecture lets us implement virtual users as messages instead of dedicated threads, making them very resource cheap. Thus, running thousands of concurrent virtual users is not an issue.

## Gatling Enterprise

[Gatling Enterprise](https://gatling.io/enterprise/), formerly known as Gatling FrontLine, is a management interface for Gatling, that includes advanced metrics and advanced features for integration and automation.

{{< img src="Gatling-enterprise-logo-RVB.png" alt="FrontLine" >}}

## Migrating from a Previous Version of Gatling

* If you're upgrading from Gatling 2.3 to Gatling 3.0, please check the [dedicated migration guide]({{< ref "reference/current/upgrading/2.3-to-3.0.md" >}}).
* Otherwise, please follow the [previous migration guides]({{< ref "reference/current/upgrading" >}}).
