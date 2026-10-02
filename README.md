<div align="center">
  <a href="https://gatling.io/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=hero">
    <img src="https://cdn.prod.website-files.com/685a8fe4ddca049f26333871/6abd00d92ff7aca209806ee9_home_gatling.png" alt="Gatling: simulations written as code, run reports, response time percentiles, and trend analysis">
  </a>

  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://docs.gatling.io/images/logo-gatling.svg">
    <img src="https://docs.gatling.io/images/logo-gatling-noir.svg" alt="Gatling" width="220">
  </picture>

  <p>
    <strong>Load testing as code</strong>
    <br />
    Write tests in Java, JavaScript, TypeScript, Kotlin or Scala.<br />
    Run them on your machine or in your CI, and fail the build when performance regresses.
    <br />
    <br />
    <a href="https://docs.gatling.io/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=nav"><strong>Documentation</strong></a>
    ·
    <a href="https://community.gatling.io/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=nav"><strong>Community forum</strong></a>
    ·
    <a href="#gatling-enterprise-edition"><strong>Enterprise Edition</strong></a>
    ·
    <a href="https://gatling.io/gatling-enterprise-load-testing-live-demo?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=nav"><strong>Live demo</strong></a>
  </p>
</div>

<p align="center">
  <a href="https://github.com/gatling/gatling/actions/workflows/build.yml?query=branch%3Amain"><img src="https://github.com/gatling/gatling/actions/workflows/build.yml/badge.svg?branch=main" alt="Build status"></a>
  <a href="https://central.sonatype.com/search?q=gatling-core"><img src="https://img.shields.io/maven-central/v/io.gatling/gatling-core" alt="Maven Central"></a>
  <a href="https://github.com/gatling/gatling/blob/main/LICENSE.txt"><img src="https://img.shields.io/badge/license-Apache%202.0-informational" alt="License"></a>
</p>

<p align="center">
  <a href="https://gatling.io/customers?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=customers">
    <img src="https://cdn.prod.website-files.com/685a8fe4ddca049f26333871/6ab145cd86b5260ca8f8b896_Customer%20Logos.png" alt="More than 300,000 companies trust Gatling, including Adobe, Betclic, Bouygues Telecom, Canal+, Circle K, Criteo, MultiChoice, Playtika, SNCF Connect, Sophos, Sopra Steria, and Toast" width="100%">
  </a>
</p>

---

## What is Gatling?

Gatling is an open-source load testing tool. You write test scenarios ("simulations") as code, run them against your services, and get a detailed HTML report of response times, throughput, and errors. This repository is **Gatling Community Edition**, licensed under Apache 2.0 and free to use, including commercially.

```text
Write a simulation -> Run it locally or in CI -> Read the report -> Catch regressions before release
```

## Why Gatling

**An engine that does not need a fleet.** Virtual users are lightweight messages on a non-blocking engine built on Netty, not operating system threads. Tools built on a thread-per-user model need a fleet of load generators to reach the same load.

**Tests are code, in your repository.** Readable diffs, reviewable pull requests, and the same review process as the application they test. No proprietary XML format, no export step between a GUI and your pipeline.

**Write once, run anywhere.** The same simulation runs on a laptop and in CI, through the Maven, Gradle, sbt, or npm plugin, with assertions on response times and error rates.

**Open source, Apache 2.0.** No account, no registration, and no cap on test duration or virtual users for any protocol in this repository. 300,000+ companies and 30M+ downloads.

<p align="center">
  <em>"If you wanted to drive hundreds of thousands of transactions per second, it's not a hard thing to achieve in Gatling. You don't have to tune it. You don't have to call in an expert. Any developer getting started would be able to drive that kind of load with ease."</em>
  <br />
  <strong>Chaitanya Bhatt, Principal Engineer at Intuit</strong>
  ·
  <a href="https://gatling.io/intuit?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=customers">Read the customer story</a>
</p>

## Get started

Follow the [official tutorials](https://docs.gatling.io/tutorials/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=get-started): installation, your first simulation, and running it, in every supported language.

A complete simulation, in Java. It sends 10 users to one endpoint and fails if any request fails:

```java
package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;

public class BasicSimulation extends Simulation {
  {
    setUp(
      scenario("Scenario")
        .exec(http("Session").get("/session"))
        .injectOpen(atOnceUsers(10))
    ).protocols(http.baseUrl("https://api-ecomm.gatling.io"))
     .assertions(global().failedRequests().count().lt(1L));
  }
}
```

Put it in `src/test/java/example/` of the [Maven starter project](https://github.com/gatling/gatling-maven-plugin-demo-java) and run:

```bash
./mvnw gatling:test
```

## What is in this repository

| Layer | What you get |
|---|---|
| **Write** | The [Recorder](https://docs.gatling.io/reference/script/http/recorder/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo), which turns a browser session or a HAR file into a simulation. Feeders for CSV, JSON, and databases. [Checks](https://docs.gatling.io/reference/script/http/checks/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo) on status codes, response bodies, headers, and timings |
| **Run** | A non-blocking engine built on Netty. [HTTP/1.1 and HTTP/2](https://docs.gatling.io/reference/script/http/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo), [WebSocket](https://docs.gatling.io/reference/script/websocket/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo), [Server-Sent Events](https://docs.gatling.io/reference/script/sse/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo), and [JMS](https://docs.gatling.io/reference/script/jms/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo). Open and closed workload models |
| **Analyze** | An [HTML report](https://docs.gatling.io/reference/stats/reports/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=repo) per run, with percentiles, throughput over time, and errors per request. Assertions that fail the build when a threshold is breached |
| **Automate** | Maven, Gradle, sbt, and npm plugins. Run from the command line, in whatever CI system you already use |

## SDKs

| SDK | Languages | Where |
|---|---|---|
| **[Java SDK](https://docs.gatling.io/tutorials/test-as-code/java-jvm/installation-guide/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=sdks)** | Java, plus Kotlin and Scala on the JVM | This repository. Any OpenJDK LTS release from 11 to 25, with Maven, Gradle, or sbt |
| **[JavaScript SDK](https://docs.gatling.io/tutorials/test-as-code/javascript/installation-guide/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=sdks)** | JavaScript, TypeScript | [gatling/gatling-js](https://github.com/gatling/gatling-js) |

Both drive the same engine and produce the same reports.

## Protocols

HTTP/1.1, HTTP/2, WebSocket, Server-Sent Events, and JMS ship here under Apache 2.0, with no limits.

[gRPC](https://docs.gatling.io/reference/script/grpc/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=protocols), [MQTT](https://docs.gatling.io/reference/script/mqtt/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=protocols), and [GraphQL](https://docs.gatling.io/reference/script/graphql/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=protocols) are separate dependencies under the Gatling Enterprise Component License, not Apache 2.0. With Community Edition they are capped at 5 concurrent users and 5 minute tests. GraphQL is available in the Java SDK (Java, Kotlin, Scala), not yet in the JavaScript SDK.

Kafka, AMQP, SFTP, FTP, and JDBC are covered by [third-party plugins](https://docs.gatling.io/reference/script/third-parties/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=protocols) maintained by the community, not by Gatling.

## Community and support

[Documentation](https://docs.gatling.io/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=community) · [Community forum](https://community.gatling.io/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=community) · [YouTube](https://www.youtube.com/@gatlingcorp) · [Report a bug](https://github.com/gatling/gatling/issues)

## Contributing

Gatling is, and will remain, open source. Read the [contribution guidelines](CONTRIBUTING.md) before opening a pull request.

## License

Gatling Community Edition is licensed under [Apache 2.0](LICENSE.txt). The gRPC, MQTT, and GraphQL components are distributed under the Gatling Enterprise Component License. Reports are built with [Highcharts](https://www.highcharts.com/), under a license granted by Highsoft AS.

---

## Gatling Enterprise Edition

Everything above runs from this repository alone. Gatling Enterprise Edition is a commercial product built on the same engine, for teams that need to generate load from managed or private infrastructure and share results across the organization.

| | Community Edition | Enterprise Edition |
|---|---|---|
| **License** | Apache 2.0, free | Commercial |
| **Load generation** | Your machine | Managed generators, or your own AWS, Azure, GCP, or Kubernetes accounts through [private locations](https://docs.gatling.io/reference/deploy/private-locations/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=enterprise) |
| **Reporting** | HTML report per run | Live dashboards, run comparison, [trends over time](https://docs.gatling.io/reference/stats/trends/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=enterprise), SLO tracking, and [AI analysis](https://docs.gatling.io/ai-for-analysis/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=enterprise) |
| **Collaboration** | Not included | [Teams, user roles, SSO](https://docs.gatling.io/reference/administration/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=enterprise), and shared reports |
| **Integrations** | Build tool plugins only | [CI/CD, APM, notifications, and SSO integrations](https://gatling.io/integrations?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=enterprise) |
| **gRPC, MQTT, and GraphQL** | 5 users, 5 minute tests | Unlimited |

[Try Enterprise Edition free for 14 days](https://cloud.gatling.io/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=trial), no credit card required.

### AI extensions

Skills and an MCP server that let an AI coding assistant scaffold Gatling projects, convert JMeter test plans and LoadRunner scripts, and drive Gatling Enterprise from your editor. Packaged as a Claude Code plugin, and usable in any LLM client that supports skills or local MCP servers. They require a Gatling Enterprise API token.

| Documentation | What it covers |
|---|---|
| [Overview](https://docs.gatling.io/ai-plugins/overview/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai) | Installation, requirements, and what the extensions contain |
| [Gatling Skills](https://docs.gatling.io/ai-plugins/skills/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai) | Bootstrap a project, build tools, configuration as code, JMeter and LoadRunner converters |
| [Gatling MCP Server](https://docs.gatling.io/ai-plugins/mcp-server/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai) | Query teams, packages, simulations, and load generator locations in natural language |
| [AI Assistant](https://docs.gatling.io/ai-plugins/assistant/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai) | IDE integrations for [VS Code](https://docs.gatling.io/ai-plugins/assistant/vscode/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai), [Cursor](https://docs.gatling.io/ai-plugins/assistant/cursor/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai), [Windsurf](https://docs.gatling.io/ai-plugins/assistant/windsurf/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai), and [Antigravity](https://docs.gatling.io/ai-plugins/assistant/antigravity/?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=ai) |

<br />

<p align="center">
  <a href="https://gatling.io/gatling-enterprise-load-testing-live-demo?utm_campaign=oss&utm_source=github&utm_medium=gatling&utm_content=live-session-cta">
    <img src="https://cdn.prod.website-files.com/685a8fe4ddca049f26333871/6abe241b2becb5acfcc7d005_Live%20Session%20Enterprise%20Edition.png" alt="Live demo: Gatling in action. Build, run and interpret your first load test. See what Enterprise Edition gives you out of the box, in 30 minutes. Register for the next live demo." width="100%">
  </a>
</p>
