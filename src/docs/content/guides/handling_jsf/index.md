---
title: "Handling JSF"
description: ""
lead: ""
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Basic JSF

JSF requires a parameter named `javax.faces.ViewState` to be captured on every page and be passed in every POST request.

Adding a check for capturing the value and a param on very request would be very cumbersome.
Hopefully, we can factor out these operations.

Define factory methods for building JSF requests that would automatically perform those operations:

```scala
||< include-static "HandlingJsfSample.scala#factory-methods" >||
```

You can then build your requests just like you're used to:

```scala
||< include-static "HandlingJsfSample.scala#example-scenario" >||
```

{{< alert tip >}}
The sample above is taken from the [Primefaces demo](http://www.primefaces.org/showcase-labs).
{{< /alert >}}

See Rafael Pestano's [demo project](https://github.com/rmpestano/gatling-jsf-demo) for a complete sample.

## Trinidad

Trinidad's `_afPfm` query parameter can be handled similarly:

```scala
||< include-static "HandlingJsfSample.scala#trinidad" >||
```
