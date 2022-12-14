---
title: "Configuration"
description: "How to configure Gatling: configuration file options, bundle command line options, logging."
lead: "Configure the logs with logback.xml, the configuration with gatling.conf, and the zip bundle command options"
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
weight: 2030800
---

Gatling can be configured and optimized in three ways:

* with configuration files, located in `conf` directory
* with command line options
* with the `$JAVA_OPTS` environment variable

## Configuration files

### logback.xml

This file allows you to configure the log level of Gatling.
For further information, you should have a look at the [Logback Documentation](http://logback.qos.ch/manual/index.html).

{{< alert tip >}}
In order to log requests and responses, uncomment the dedicated loggers in the [default logging configuration file](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/logback.dummy).
{{< /alert >}}

### gatling.conf {#gatling-conf}

Gatling configuration is based on the great [Typesafe Config library](https://github.com/lightbend/config).

Gatling configuration files, such as the default configuration file, use the [HOCON format](https://github.com/lightbend/config/blob/master/HOCON.md).

{{< alert tip >}}
Please check [the configuration file source on GitHub](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/gatling-defaults.conf) for all the available configuration options.
{{< /alert >}}

Gatling uses a fallback strategy, where:

**System properties > `gatling.conf` > gatling-defaults.conf**

`gatling-defaults.conf` is shipped in the gatling-core jar and must not be tampered.

`gatling.conf` is the default name of the user defined file. It's resolved from the ClassLoader, not the filesystem, meaning it must be placed in `src/test/resources` for a maven/gradle/sbt project and in `conf` in the bundle distribution.

This file name can be changed with a System property named `gatling.conf.file`, eg `-Dgatling.conf.file=gatling-special.conf`. Again, beware it's loaded from the ClassLoader, not the filesystem, eg:

* `-Dgatling.conf.file=src/test/resource/gatling-special.conf` is incorrect
* `-Dgatling.conf.file=gatling-special.conf` where `gatling-special.conf` is placed in `src/test/resource` is correct

The bundle distribution and the maven/gradle/sbt plugins demo projects contain an easy-to-edit `gatling.conf` file with all the available properties commented with the default values.

If you want to override default values, you have two possibilities:

* change the value in `gatling.conf`.
* set a System property (the name of the property must match the [HOCON Path](https://github.com/typesafehub/config/blob/master/HOCON.md#paths-as-keys))

{{< alert warning >}}
When editing `gatling.conf`, don't forget to remove the leading `#` that comments the line, otherwise your change will be ineffective.
{{< /alert >}}

## Zip Bundle Command Line Options {#cli-options}

The Zip bundle can be used to start Gatling locally, or with the Enterprise cloud edition. Run the `gatling.bat` file if you run on Windows, or the `gatling.sh` file if you run on MacOS or Linux.

Gatling can be started with several options listed below:

Common options:

| Option (short)     | Option (long)                      | Description                                                                                                                                          |
| --- | --- | --- |
| `-h`               | `--help`                           | Show help (this message) and exit                                                                                                                    |
| `-rm <value>`      | `--run-mode <value>`               | Specify if you want to run the Simulation locally, on Gatling Enterprise or package the simulation. Options are `local`, `enterprise` and `package`  |

Options used when compiling your Gatling simulations:

| Option (short)  | Option (long)                      | Description                                                                                        |
| --- | --- | --- |
| `-sf <path>`    | `--simulations-folder <path>`      | Uses `<path>` as the folder where simulations are stored                                           |
| `-bf <path>`    | `--binaries-folder <path>`         | Uses `<path>` as the folder where simulation binaries are stored                                   |
| `-eso <value>`  | `----extra-scalac-options <value>` | Defines additional scalac options for the compiler                                                 |
| `-ecjo <value>` | `--extra-compiler-jvm-options "-Option1 -Option2"` | Defines additional JVM options used when compiling your code (e.g. setting the heap size with "-Xms2G -Xmx4G"). See https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html for available options. |

Options used when running Gatling locally:

| Option (short)     | Option (long)                      | Description                                                                                        |
| --- | --- | --- |
| `-nr`              | `--no-reports`                     | Runs simulation but does not generate reports                                                      |
| `-ro <folderName>` | `--reports-only <folderName>`      | Generates the reports for the simulation log file located in `<gatling_home>/results/<folderName>` |
| `-rf <path>`       | `--results-folder <path>`          | Uses `<path>` as the folder where results are stored                                               |
| `-rsf <path>`      | `--resources-folder <path>`        | Uses `<path>` as the folder where resources are stored                                             |
| `-bf <path>`       | `--binaries-folder <path>`         | Uses `<path>` as the folder where simulation binaries are stored                                   |
| `-s <className>`   | `--simulation <className>`         | Uses `<className>` as the name of the simulation to be run                                         |
| `-rd <description>`| `--run-description <description>`  | A short `<description>` of the run to include in the report                                        |
| `-erjo`            | `--extra-run-jvm-options "-Option1 -Option2"` | Defines additional JVM options used when running your code locally (e.g. setting the heap size with "-Xms2G -Xmx4G"). See https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html for available options. |

Options used when running Gatling on Gatling Enterprise:

| Option (short)     | Option (long)                      | Description                                                                                        |
| --- | --- | --- |
| `-bm`              | `--batch-mode`                     | No interactive user input will be asked                                                            |
| `-at <token>`      | `--api-token <token>`              | Gatling Enterprise's API token with the 'Configure' role                                           |
| `-sid <id>`        | `--simulation-id <id>`             | Specifies the Gatling Enterprise Simulation, when creating a new Simulation                        |
|`-pid`              | `--package-id <id>`                | Specifies the Gatling Enterprise Package, when creating a new Simulation                           |
|`-tid`              | `--team-id <id>`                   | Specifies the Gatling Enterprise Team, when creating a new Simulation                              |
|`-s`                | `--simulation <className>`         | Runs `<className>` simulation                                                                      |
|`-ssp`              | `--simulation-system-properties k1=v1,k2=v2` | Optional System Properties used when starting the Gatling Enterprise simulation          |
|`-sev`              | `--simulation-environment-variables k1=v1,k2=v2` | Optional Environment Variables used when starting the Gatling Enterprise simulation          |

## $JAVA_OPTS

Default command line options for JAVA are set in the launch scripts.
You can use the JAVA_OPTS environment variable to override those defaults, eg:

```console
JAVA_OPTS="myAdditionalOption" bin/gatling.sh
```
