---
title: Control plane setup for private locations
menutitle: Introduction
description: Learn how to configure and install a control plane to use Private Locations for Gatling Enterprise Cloud.
lead: Private Locations on your own private account.
date: 2021-11-07T14:29:04+00:00
lastmod: 2023-04-03T12:00:00+00:00
---

A location describes where to start load generators:

- Public locations are available by default, and listed by regions.
- Private locations must be configured, and allow you to deploy load generators on your own infrastructure.

{{< alert warning >}}
Private locations are not compatible with the no-code test builder.
{{< /alert >}}

{{< alert info >}}
Private locations allow you to load test in a private infrastructure, without any inbound connection or credentials sharing.
To access this feature, please contact our [technical support](https://gatlingcorp.atlassian.net/servicedesk/customer/portal/8/group/12/create/59?summary=Private+Locations&description=Contact%20email%3A%20%3Cemail%3E%0A%0AHello%2C%20we%20would%20like%20to%20enable%20the%20private%20locations%20feature%20on%20our%20organization.).
{{< /alert >}}

## Introduction

To configure private locations, you must configure an agent in your infrastructure, called a **Control Plane**.

This control plane will be in charge of spawning load generators in different locations, based on the configuration
you will provide.

{{< img src="schema.png" alt="Infrastructure schema" >}}

The control plane will periodically poll our API to find out if a new simulation run has been started using locations handled by this control plane.

If so, it will start new instances (based on the locations configurations) and start the simulation run on them. 
Those instances will send stats through the API as well.

## Control plane

### Network prerequisites

Ensure the control plane network permits outbound access to the following domain:
- Gatling Cloud API: `https://api.gatling.io`
  - This is used to register the control plane to the Gatling Enterprise Cloud application and to receive deployment orders.

Your private locations, as configured by the control plane, must permit outbound access to these domains:
- Gatling Cloud API: `https://api.gatling.io`
  - This is used to communicate metrics from the run.
- AWS S3 domains for downloading Gatling dependencies and simulations:
  - `https://cloud-probes-eu-west-3.s3.eu-west-3.amazonaws.com` (for Gatling libraries)
  - `https://frontline-cloud-prod-eu-west3.s3.eu-west-3.amazonaws.com` (for simulation files)

### Token

Access the private locations section by clicking on the Private locations in the navigation bar (only visible if the feature is activated on your organization).

{{< alert warning >}}
Only organization system admins can manage private locations.
{{< /alert >}}

Click on create control plane, and fill a unique identifier (only lowercase, separated by underscores).

{{< img src="create-control-plane.png" alt="Create control plane" >}}

On the next modal, **make sure to copy the control plane token**, you’ll need it later.

{{< img src="copy-token.png" alt="Copy control plane token" >}}

You can see the control plane you just created under the Control planes tab.

{{< img src="control-planes-uninitialized.png" alt="Control Planes table" >}}

It is defined by a unique identifier, a description (provided later on by its configuration) and a status.

The status has three possible values:
- {{< badge enterprise Uninitialized />}}: the control plane has never contacted the application
- {{< badge success Up />}}: the control plane is properly configured, has uploaded the names of its configured private locations, and periodically calls the application to fetch new runs.
- {{< badge danger Down />}}: the control plane has been up, but hasn't called the application for a while.

### Configuration

The locations managed by the control plane are configured by a file.
The configuration file uses the [HOCON format (Human-Optimized Config Object Notation)](https://github.com/lightbend/config/blob/master/HOCON.md):

```bash
control-plane {
  # Control plane token
  token = "cpt_example_c7oze5djp3u14a5xqjanh..."
  # Control plane token with an environment variable
  token = ${?CONTROL_PLANE_TOKEN}
  # Control plane description (optional)
  description = "Control plane optional description"
  # Control plane private locations
  locations = [
    {
      # Private location ID, must be prefixed by prl_, only consist of numbers 0-9, 
      # lowercase letters a-z, and underscores, with a max length of 30 characters
      id = "prl_example"
      # Private location description (optional)
      description = "Private location optional description"
      # Private location provider specification
      # ...
    }
  ]
}
```

For examples of private locations configuration, see:
* [Configuration of Amazon EC2 locations]({{< ref "aws/configuration" >}})
* [Configuration of Azure Virtual Machines locations]({{< ref "azure/configuration" >}})
* [Configuration of GCP Compute Engine locations]({{< ref "gcp/configuration" >}})
* [Configuration of Kubernetes locations]({{< ref "kubernetes/configuration" >}})
* [Configuration of Dedicated Machines locations]({{< ref "dedicated/configuration" >}})

### Installation

The control plane agent is distributed as a docker image: [`gatlingcorp/control-plane`](https://hub.docker.com/r/gatlingcorp/control-plane). It is available for the `linux/amd64` and `linux/arm64` platforms (Docker will automatically select the correct image variant).

{{< alert info >}}
Configuration file is mounted at `/app/conf/control-plane.conf`.
{{< /alert >}}

For examples of installations, see:
* [Amazon elastic container service]({{< ref "aws/installation" >}})
* [Azure container applications service]({{< ref "azure/installation" >}})
* [GCP Compute Engine service]({{< ref "gcp/installation" >}})
* [Kubernetes deployment]({{< ref "kubernetes/installation" >}})
* [Docker container deployment]({{< ref "dedicated/installation" >}})

## Managing Control Planes on Gatling Enterprise Cloud

Once configured and running, your control plane status should go {{< badge success Up />}} within a few seconds,
details are available by clicking on the {{< icon eye >}} button.
Tokens can be refreshed by clicking on the {{< icon undo >}} button.

{{< img src="control-plane-details.png" alt="Control Planes details" >}}

Private locations can be seen in the **Locations** tab.
You can see their relations to their control plane and simulations.

{{< img src="locations-table.png" alt="Locations table" >}}

They can be deleted if they are neither linked to a control plane that is currently {{< badge success Up />}} nor to any
simulation.

## Simulation configuration

When configuring a simulation, on the locations step, click on private.

{{< img src="simulation-config.png" alt="Simulation configuration of private locations" >}}

{{< alert warning >}}
At the moment, it is not possible to use private locations along with public ones.
{{< /alert >}}

## Credit consumption

Private locations consume credits like public locations: one credit, per load generator, per minute.
