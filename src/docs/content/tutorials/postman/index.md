---
title: Create a simulation from Postman
seotitle: Gatling Postman getting started
description: Learn how to use your Postman collections in Gatling load tests.
lead: Use your Postman collections in Gatling load tests
date: 2024-12-15T15:22:00+02:00
lastmod: 2024-12-16T10:22:00+02:00
badge:
  type: enterprise
  label: Enterprise
---

{{< alert warning >}}
This guide is intended for Gatling versions `{{< var gatlingVersion >}}` and later.
{{< /alert >}}

## Introduction

The Gatling Postman component allows Postman users to use their preexisting Postman Collections, environments, and global variables to construct Gatling load tests. This feature: 

- reduces test authoring time, 
- increases consistency between functional and non-functional tests,
- improves collaboration between developers, operations, and QA teams.

To learn more about [Postman](https://www.postman.com), visit the [Postman official documentation](https://learning.postman.com/docs/). 

## Scope of Postman support

Postman support is only available for the Gatling JavaScript/TypeScript SDK; it is not available in Java, Kotlin, or Scala.

The Gatling Postman component is designed to import Postman collections and run straightforward requests. While it already supports most core functionalities, we're continuously enhancing its capabilities to meet your evolving needs. Here's a quick look at features we’re actively considering but are limited or not currently available:

- Postman Cloud Integration: Currently, collections and environments must be exported as JSON files for use. We’re exploring deeper integration possibilities.
- Script Execution: Support for pre-request and post-response scripts is not currently supported.
- Dynamic Variable Handling: Enhancements for modifying variable values during execution and injecting them from data files is not currently supported.
- Randomly Generated Data: Postman's dynamic variables used to generate sample data is not currently supported. 
- Request Authorization & Settings: Features like Basic Auth, Bearer Token handling, and advanced settings (e.g., disabling follow redirects). 
- File Uploads: For now, file uploads are supported by placing files in your Gatling project's `resources` folder.

More information about the current functionalities and limitations is available in the [reference documentation]({{< ref "/reference/script/protocols/postman/" >}}).

## Installation

To get started with the Gatling Postman integration, either:

- download a demo project
- add the Postman dependency to an existing project

Both methods are detailed in the following sections. 

### Start with the demo project {#demo-project}

A [demo project](https://github.com/gatling/gatling-postman-demo) is available with JavaScript and TypeScript test examples:

- [JavaScript](https://github.com/gatling/gatling-postman-demo/tree/main/javascript)
- [TypeScript](https://github.com/gatling/gatling-postman-demo/tree/main/typescript)

You can download the demo project and open it in your IDE to start working with the Gatling Postman integration. 

### Add the Gatling Postman dependency to an existing project {#dependency}

You can also add Gatling Postman to an existing Gatling project written in JavaScript or TypeScript. In that case, add the Gatling Postman dependency to set up your project:

```shell
npm install --save "@gatling.io/postman"
```

For more details on how to create a JavaScript or TypeScript project, check the
[dedicated tutorial]({{< ref "/tutorials/scripting-intro-js/" >}}).

## Import your Postman assets

To use your Postman assets in a Gatling project, copy the exported Postman collection, optionally environment, and global variables to the `resources` folder. 

## Write your simulation

The simulation consists of 3 parts. In the simplest form: 

1. Import statements that bring the core Gatling functionalities and the Postman component:

    ```javascript
    import { simulation, scenario, atOnceUsers } from "@gatling.io/core";
    import { http } from "@gatling.io/http";
    import { postman } from "@gatling.io/postman";
    ```

2. The virtual user scenario contained in a simulation function:
  
    ```javascript
    export default simulation((setUp) => {
    const httpProtocol = http;

    const collection = postman
      .fromResource("myCollection.postman_collection.json");

    const scn = collection.scenario("My Scenario");
    //...
    });
    ```

3. The injection profile that defines how virtual users are added to your scenario:

    ```javascript
    setUp(
      scn.injectOpen(
          constantUsersPerSec(0.1).during(50)
      ).protocols(httpProtocol)
   );
    ```
  
The following code example combines the 3 preceeding parts and is a complete Gatling simulation using the Postman component. The code example is annotated to explain how it works with the following numbers:

1. Import a Postman Collection.
2. Use the Postman Collection as a virtual user scenario.
3. Create an injection profile that adds 1 new user every 10 seconds for 50 seconds.

```javascript
import { simulation, constantUsersPerSec } from "@gatling.io/core";
import { http } from "@gatling.io/http"; 
import { postman } from "@gatling.io/postman";

export default simulation((setUp) => {
  const httpProtocol = http;

  const collection = postman
    .fromResource("myCollection.postman_collection.json"); //1

  const scn = collection.scenario("My Scenario", { pauses: 1 }); //2
  
  setUp(
    scn.injectOpen(
        constantUsersPerSec(0.1).during(50)
    ).protocols(httpProtocol) //3
 ); 
});
```
{{< alert tip >}}
You can develop more complex scenarios that, for example, blend Postman Collections and Gatling requests. To learn more about the complete SDK functionality, see the [reference documentation]({{< ref "/reference/script/protocols/postman/" >}}). 
{{< /alert >}}

## Run your simulation on Gatling Enterprise

To run your simulation without usage limits, you must use Gatling Enterprise (for more information on usage limits see [License and limitations]({{< ref "#license" >}})). 

1. Package your test by running the command `npx gatling enterprise-package` in your terminal. The packaged simulation is saved in the `target` folder.
2. Log in to your Gatling Enterprise account. 
3. Click on **Simulations** in the left-side menu.
4. Click on **Create a simulation** and follow the prompts to upload your package and create your simulation.
5. Start your simulation and see the live results!

## Run the Simulation locally for debugging {{% badge info "Optional" /%}} {#run-the-simulation-locally-for-debugging}

Use the following command to run your simulation in your local developer environment. The simulation only starts if the injection profile respects the license limitations for open-source usage. 

```shell
npx gatling run
```
If you have more than 1 simulation in the `/src` folder, use the interactive CLI to select the Postman-based simulation. 

## License and limitations {#license}

**The Gatling Postman component is distributed under the
[Gatling Enterprise Component License]({{< ref "/project/licenses/enterprise-component" >}}).**

Gatling Postman can be used with both the [Open Source](https://gatling.io/products/) and
[Enterprise](https://gatling.io/products/) versions of Gatling.

Its usage is unlimited when running on [Gatling Enterprise](https://gatling.io/products/). When used with
[Gatling Open Source](https://gatling.io/products/), usage is limited to:

- 5 users maximum
- 5 minute duration tests

Limits after which the test stops.
