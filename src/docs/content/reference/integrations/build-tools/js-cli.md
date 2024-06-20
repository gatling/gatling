---
menutitle: JavaScript CLI
title: JavaScript tooling
seotitle: Tooling to work with Gatling in JavaScript and TypeScript
description: >
  Use our command-line (CLI) tool and a package manager such as npm to work with Gatling and Gatling Enterprise, when
  using the JavaScript/TypeScript DSL to write your tests.
lead: >
  Run Gatling simulations written in JavaScript/TypeScript from the command line, and package them to run on Gatling
  Enterprise
date: 2024-06-20T14:00:00+02:00
lastmod: 2024-06-20T14:00:00+02:00
---

## Versions

Check out available versions on [npm](https://www.npmjs.com/package/@gatling.io/cli).

## Pre-requisites and compatibility

You need:

- [Node.js](https://nodejs.org/) v18 or later (we only support using LTS versions)
- npm v8 or later, which comes pre-installed with Node.js

Using another compatible package manager (such as Yarn) should also work, but you need to adapt the equivalent npm
commands and configuration on your own.

## Setup

Cloning or downloading [our demo projects](https://github.com/gatling/gatling-js-demo) on GitHub is definitely the fastest way to get started. The repo
includes both a JavaScript and a TypeScript projects.

If you prefer to manually configure your npm project rather than clone one of our samples, you will need to add the
following dependencies:

```shell
npm install --save-dev "@gatling.io/cli"
npm install --save "@gatling.io/core"
npm install --save "@gatling.io/http"
```

The `@gatling.io/cli` contains the `gatling` command-line interface (CLI) tool. Once added to your project's dev
dependencies, you can execute it with the command `npx gatling` without having to install it globally or add it to your shell's path.

You can explore all available commands with `npx gatling --help`, and all options for a given command with
`npx gatling command-name --help`. The most typical usages are explained below.

Remember that you can define aliases for commonly used commands in the `scripts` section of you `package.json` file, as
shown [in the npm documentation](https://docs.npmjs.com/cli/v10/using-npm/scripts). 

### Project layout

By default, the `gatling` CLI tool expects that:

- Gatling simulation files are located in the `src` folder (can be overridden with the `--sources-folder` option).
  * The `gatling` tool will find simulations defined in files named with a `.gatling.js` or `.gatling.ts` extension at
    the root of this folder.
- Resource files accessible to Gatling (e.g. feeder files) are located in the `resources` folder (can be overridden with
  the `--resources-folder` option).
- When running locally, it will create test reports in the `target/gatling` folder (can be overridden with the
  `--results-folder` option).
- When running locally or packaging for Gatling Enterprise, it will create a code bundle file at `target/bundle.js`
  (can be overridden with the `--bundle-file` option).
- When packaging for Gatling Enterprise, it will create the package file at `target/package.zip` (can be overridden with
  the `--package-file` option).

### Dependency management

You can add other library dependencies from npm, as long as:

- they do not rely on native (non-JavaScript) binaries
- they do not use JavaScript APIs which are specific to Node.js

You can also use dev dependencies normally for other tools you might want to run on your code; e.g. our demo projects
include a [Prettier](https://prettier.io) configuration for automatic code formatting.

### Upgrade the JavaScript SDK version

Gatling periodically releases new versions of the JavaScript SDK to maintain compatibility with Gatling Enterprise,
add new functionality, and improve performance. Be sure to check the
[Gatling upgrade guides]({{< ref "/release-notes/upgrading" >}}) for breaking changes.

Use the following procedure to upgrade your SDK version:

1. Update the following in the `package.json` to the latest version and save the file:

    * `version`
    * `@gatling.io/core`
    * `@gatling.io/http`
    * `@gatling.io/cli`

2. Run `npm install`

## Commands

### Running your simulations

Use the `gatling run` command to execute Gatling simulations. Specify which simulation to run with the `--simulation`
option. For instance:

```shell
npx gatling run --simulation "my-simulation"
```

Runs a simulation defined in `src/my-simulation.gatling.js` or `src/my-simulation.gatling.ts`, and write the report
inside the folder `target/gatling`.

You can check out other options with `npx gatling run --help`.

### Running the Gatling Recorder

You can launch the [Gatling Recorder]({{< ref "../../script/protocols/http/recorder" >}}):

```shell
npx gatling recorder
```

You can check out other options with `npx gatling recorder --help`.

### Running your simulations for Gatling Enterprise Cloud

#### Prerequisites

You need to configure [an API token]({{< ref "/reference/execute/cloud/admin/api-tokens/" >}}) for most of the actions
between the CLI and Gatling Enterprise Cloud.

{{< alert warning >}}
The API token needs the `Configure` role on expected teams.
{{< /alert >}}

Since you probably don't want to include you secret token in your source code, you can configure it using either:

- the `GATLING_ENTERPRISE_API_TOKEN` environment variable
- the `--api-token` option

#### Packaging your simulations for Gatling Enterprise Cloud

Use the `enterprise-package` command to create a package of your simulations to deploy on Gatling Enterprise.
For instance:

```shell
npx gatling enterprise-package
```

Will create a package `target/package.zip`, which contains all simulations matching the pattern `src/*.gatling.js` or
`src/*.gatling.ts`, and all resources files found in the `resources` folder.

You may want to specify the package file name:

```shell
npx gatling enterprise-package --package-file "target/my-package-file.zip"
```

You can check out other options with `npx gatling enterprise-package --help`.

#### Deploying on Gatling Enterprise Cloud

With the `enterprise-deploy` command, you can:

- Create, update and upload packages
- Create and update simulations

This command automatically checks your simulation project and performs the deployment according to your configuration.

By default, `enterprise-deploy` searches for the package descriptor in `.gatling/package.conf`.
However, you can target a different filename in `.gatling` by using the following command:
```shell
npx gatling enterprise-deploy --package-descriptor-filename="<file name>"
```

You can check out other options with `npx gatling enterprise-deploy --help`.

{{< alert info >}}
You can run this command without any configuration to try it.

Check the [Configuration as Code documentation]({{< ref "/reference/execute/cloud/user/configuration-as-code" >}}) for
the complete reference and advanced usage.
{{< /alert >}}

#### Start your simulations on Gatling Enterprise Cloud

You can, using the `enterprise-start` command:

- Automatically [deploy your package and associated simulations](#deploying-on-gatling-enterprise-cloud)
- Start a deployed simulation

By default, the Gatling plugin prompts the user to choose a simulation to start from amongst the deployed simulations.
However, users can also specify the simulation name directly to bypass the prompt using the following command:

```shell
npx gatling enterprise-start --enterprise-simulation="<simulation name>"
````

Replace `<simulation name>` with the desired name of the simulation you want to start.

If you are on a CI environment, you don't want to handle interaction with the plugin.
Most CI tools define the `CI` environment variable, used by the Gatling plugin to disable interactions and run in headless mode.

It's also possible to disable interactions by using the `--non-interactive` option.

Here are additional options for this command:

- `--wait-for-run-end`: Enables the command to wait until the run finishes and fail if there are assertion failures.
- `--run-title=<title>`: Allows setting a title for your run reports.
- `--run-description=<description>`:  Allows setting a description for your run reports summary.

You can check out other options with `npx gatling enterprise-start --help`.
