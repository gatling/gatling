---
title: Postman
seotitle: Gatling Postman reference
description: Learn how to use your Postman collections in Gatling load tests.
lead: Use your Postman collections in Gatling load tests
date: 2024-11-15T10:35:00+02:00
lastmod: 2024-11-15T10:35:00+02:00
badge:
  type: enterprise
  label: Enterprise
---

[Postman](https://www.postman.com) is a collaborative platform for testing and documenting APIs. Check the
[Postman official documentation](https://learning.postman.com/docs/) for more details.

## License and limitations {#license}

**The Gatling Postman component is distributed under the
[Gatling Enterprise Component License]({{< ref "/project/licenses/enterprise-component" >}}).**

Gatling Postman can be used with both the [Open Source](https://gatling.io/products/) and
[Enterprise](https://gatling.io/products/) versions of Gatling.

Its usage is unlimited when running on [Gatling Enterprise](https://gatling.io/products/). When used with
[Gatling Open Source](https://gatling.io/products/), usage is limited to:

- 5 users maximum
- 5 minute duration tests

Limits after which the test will stop.

## Scope of Postman support

Postman support is only available for the JavaScript/TypeScript DSL for Gatling; it is not available in Java, Kotlin, or Scala.
If you are interested in a JVM implementation, submit a request on the [public roadmap](https://gatling.io/roadmap/).

The Gatling Postman component currently supports most features needed to import a Postman collection and run simple requests defined therein.
However, it does not yet support:

- Integrating with Postman Cloud: collections and environments must be [exported to JSON files](https://learning.postman.com/docs/getting-started/importing-and-exporting/exporting-data/).
- Running pre-request or post-response scripts.
- Modifying variable values during execution or injecting them from data files.
- Dynamic variables.
- Request authorizations (e.g. Basic Auth, Bearer Token, etc.) and settings (e.g. disable follow redirects, etc.).

For requests which include file uploads, we currently support loading them from the project's resources folder as if
they were in [Postman's working directory](https://learning.postman.com/docs/getting-started/installation/settings/#working-directory).
In other words: in Postman, select files from your working directory; then copy those files to your Gatling project's
resources folder.

## Installation

### Getting started with the demo project {#demo-project}

A [demo project](https://github.com/gatling/gatling-postman-demo) is available with JavaScript and TypeScript test examples:

- [JavaScript](https://github.com/gatling/gatling-postman-demo/tree/main/javascript)
- [TypeScript](https://github.com/gatling/gatling-postman-demo/tree/main/typescript)

### Adding the Gatling Postman dependency {#dependency}

You can also add Gatling Postman to an existing Gatling project written in JavaScript or TypeScript. In that case:

1. Add the Gatling Postman dependency:

    ```shell
    npm install --save "@gatling.io/postman"
    ```

2. Copy your exported Postman files (collection, and optionally environment and global variables) to the resources folder.

For more details on how to create a JavaScript or TypeScript project, check the
[dedicated tutorial]({{< ref "/tutorials/scripting-intro-js/" >}}).

## DSL overview

The Gatling Postman support works as an extension for Gatling's HTTP protocol; it generates requests or scenarios which
are compatible with Gatling's HTTP protocol. Here is an example of a Gatling simulation using a request imported from a
Postman Collection. Like all Gatling simulations, it should be defined in a file ending in `.gatling.js` (JavaScript) or
`.gatling.ts` (TypeScript):

```javascript
import { simulation, scenario, atOnceUsers } from "@gatling.io/core"; // 1
import { http } from "@gatling.io/http"; // 1
import { postman } from "@gatling.io/postman";

export default simulation((setUp) => { // 2
  const httpProtocol = http; // 3

  const collection = postman
    .fromResource("My Collection.postman_collection.json");

  const scn = scenario("My scenario").exec( // 4
    // Manually defined HTTP request (not using Postman):
    http("My HTTP request").get("http://example.com"),
    // HTTP request generated from the Postman collection: 
    collection.request("My Postman request"),
  );

  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol); // 5
});
```

1. The [Gatling core and HTTP imports]({{< ref "/tutorials/scripting-intro-js/#set-up-the-file" >}}) are used for all 
   functionalities not specific to Postman.
2. As for all [JavaScript tests]({{< ref "/tutorials/scripting-intro-js/#define-the-simulation-function" >}}), define a
   simulation function.
3. Gatling Postman uses the standard [Gatling HTTP protocol]({{< ref "/reference/script/protocols/http/protocol" >}}).
4. It is possible to perform Postman requests inside an existing Gatling test.
5. Don't forget to [set up an injection profile]({{< ref "/tutorials/scripting-intro-js/#define-the-injection-profile" >}}).

## DSL reference

### Import Postman

Add the following import statement in your simulation file:

{{< include-code "imports" js >}}

### Import collections

You can import a collection file, exported from Postman, from the project resources (by default, from the `resources` folder):

{{< include-code "fromResource" js >}}

You can inject a Postman environment:

{{< include-code "environment" js >}}

You can also inject global variables:

{{< include-code "globals" js >}}

Variables used in your Postman collection will be substituted with values from the collection, environment, and global
variables, according to [Postman's variable scopes](https://learning.postman.com/docs/sending-requests/variables/variables/#variable-scopes).
Note that this currently only happens when requests are generated; Postman variable values cannot yet be modified during
the Gatling simulation execution.

### Create Gatling requests and scenarios

If the Postman collection is organized with folders, you can navigate with the `folder` function: 

{{< include-code "folder" js >}}

You can generate individual Gatling HTTP requests from Postman requests:

{{< include-code "request" js >}}

You can generate an entire Gatling scenario, which will execute all requests defined in the current Postman folder (or
collection root) one after the other:

{{< include-code "scenario" js >}}

By default, only requests defined directly at the current level are included. Use the `recursive` option if you want to
include all requests defined in sub-folders (at any depth).
