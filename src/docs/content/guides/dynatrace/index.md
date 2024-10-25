---
title: Dynatrace integration
seotitle: Integrate Gatling with Dynatrace
description: Set a custom test header on all generated requests.
lead: Set a custom test header on all generated requests.
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Using Gatling and Dynatrace to capture request attributes

Pass Gatling load test request attributes to Dynatrace using additional HTTP headers. Dynatrace can handle, extract, and tag information from incoming HTTP headers containing information such as:

- script name, 
- test step name, and 
- virtual user ID.

 You can then filter your monitoring data based on the defined tags. 

### Configure Dynatrace extraction rules

You can use any HTTP headers or HTTP parameters to pass contextual information. To configure the extraction rules in Dynatrace reference the [extraction rules](https://docs.dynatrace.com/docs/platform-modules/applications-and-microservices/services/request-attributes/capture-request-attributes-based-on-web-request-data) documentation.

### Add contextual information to headers 

The header `x-dynatrace-test` is used in the following example with the following set of key/value pairs for the header:
| **Acronym** | **Full Term**            | **Description**                                                                                              |
|-------------|--------------------------|--------------------------------------------------------------------------------------------------------------|
| **VU**      | Virtual User ID          | A unique identifier for the virtual user who sent the request.                                               |
| **SI**      | Source ID                | Identifies the product that triggered the request (e.g., Gatling).                                           |
| **TSN**     | Test Step Name           | Represents a logical test step within the load testing script (e.g., Login, Add to Cart).                    |
| **LSN**     | Load Script Name         | Name of the load testing script that groups test steps into a multistep transaction (e.g., Online Purchase). |
| **LTN**     | Load Test Name           | Uniquely identifies a test execution (e.g., 6h Load Test – June 25).                                         |
| **PC**      | Page Context             | Provides information about the document loaded on the currently processed page.                              |

{{< img src="dynatrace.png" alt="Dynatrace Report" >}}

## Defining a global signing function (example)

The idea here is to use [`sign`]({{< ref "/reference/script/protocols/http/protocol#sign" >}}) on the HttpProtocol to define a global signing function to be applied on all generated requests.

{{< include-code "dynatrace-sample" >}}
