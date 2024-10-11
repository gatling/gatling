---
title: Azure locations configuration
menutitle: Configuration
seotitle: Configure Azure locations in Gatling Enterprise Cloud
description: Learn how to configure load generators on your private Azure portal.
lead: Learn how to configure load generators on your private Azure portal.
date: 2023-03-31T15:29:00+00:00
lastmod: 2023-10-13T08:10:39+00:00
---

## Instance specifications

We recommend that you use for your own load generator instances with at least 4 cores.

As a result, we recommend using `Standard_A4_v2` instances or larger.

You might want to tune the `Xmx` JVM options to half of the physical memory.
See `jvm-options` configuration below.
If you don't, the JVM will use a max heap size of 1/4th of the physical memory.

{{<alert tip >}}
Simplify and speed up configuration and deployment with Gatling's pre-built [Terraform modules]({{< ref "#terraform" >}}).
{{</alert>}}

## Permissions

Azure private locations require the control plane to have credentials configured in order to instantiate virtual machines and associated resources.

Those can be set through environment variables in your control plane or via [Azure RBAC](https://learn.microsoft.com/en-us/azure/role-based-access-control/overview). Select the most appropriated method depending on your infrastructure, or check our [installation guide]({{< ref "installation" >}})  for deployment with [Azure Container Apps](https://azure.microsoft.com/en-us/products/container-apps).

### Environment variables

| name                  | value             |
| --------------------- | ----------------- |
| AZURE_CLIENT_ID       | Client UUID       |
| AZURE_CLIENT_SECRET   | Client secret key |
| AZURE_TENANT_ID       | Tenant UUID       |

Check Azure documentation pages to find these values:
* [Tenant id](https://learn.microsoft.com/en-us/azure/active-directory/fundamentals/active-directory-how-to-find-tenant)
* [Client id and secret](https://learn.microsoft.com/en-us/answers/questions/834401/hi-i-want-my-client-id-and-client-secret-key)
* [Subscription id](https://learn.microsoft.com/en-us/azure/azure-portal/get-subscription-tenant-id)

## System requirements

Azure private locations rely on some dependencies.

So when using a custom image, make sure following are available:

- [cloud-init](https://learn.microsoft.com/en-us/azure/virtual-machines/custom-data) integration.
- [jq](https://jqlang.github.io/jq/download/) a lightweight and flexible command-line JSON processor.
- [curl](https://curl.se/download.html) a command line tool and library for transferring data with URLs
- [Java runtime environment](https://openjdk.org/install/): OpenJDK 64bits LTS versions: 11, 17 or 21 (see [Gatling prerequisites]({{< ref "../../../oss#java-version" >}}))

{{< alert tip >}}
Learn how to tune the OS for more performance, configure the open files limit, the kernel and the network [here]({{< ref "../../../../script/core/operations#os-tuning" >}}).
{{< /alert >}}

## Control plane configuration file

```bash
control-plane {
  # Control plane token
  token = "cpt_example_c7oze5djp3u14a5xqjanh..."
  # Control plane token with an environment variable
  token = ${?CONTROL_PLANE_TOKEN}
  # Control plane description (optional)
  description = "my control plane description"
  # Locations configurations
  locations = [
    {
      # Private location ID, must be prefixed by prl_, only consist of numbers 0-9, 
      # lowercase letters a-z, and underscores, with a max length of 30 characters
      id = "prl_private_location_example"
      # Private location description (optional)
      description = "Private Location on Azure"
      # Private location type
      type = "azure"
      # Azure location name, as listed by Azure CLI:
      # az account list-locations -o table
      region = "westeurope"
      # Virtual machine size, as listed by Azure CLI:
      # az vm list-sizes --location "westeurope"
      size = "Standard_A4_v2"
      # Engine (optional, default classic)
      engine = "classic" # Possible values: classic or javascript
      # Certified image configuration
      image {
        type = "certified"
        java = "latest" # See engine section
      }
      # Custom image configuration (alternative to certified image)
      # image = {
      #   type = custom
      #   image = "/subscriptions/4c3f1827-1a32-4d18-8e8e-c8abb129f0fe/resourceGroups/<MyResourceGroup>/providers/Microsoft.Compute/galleries/customImages/images/<MyImage>"
      # }
      # Azure subscription id as returned by Azure CLI:
      # az account show
      subscription = "<MySubscription UUID>"
      # Full identifier of Azure Virtual Network to use for your load generators
      # Use "id" field as returned by Azure CLI:
      # az network vnet list
      network-id = "/subscriptions/<MySubscription UUID>/resourceGroups/<MyResourceGroup>/providers/Microsoft.Network/virtualNetworks/<MyVNet>"
      # Subnet belonging to previously defined virtual network
      # Use "subnets.name" as returned by Azure CLI:
      # az network vnet subnet list --resource-group MyResourceGroup --vnet-name MyVNet
      subnet-name = "default"
      # Associate a public IP to network interface (optional)
      associate-public-ip = true
      # Virtual machine tags (optional)
      tags {
        # ExampleKey = ExampleValue 
      }
      # Java configuration (following configuration properties are optional)
      # System properties (optional)
      system-properties {
        # ExampleKey = ExampleValue
      }
      # Overwrite JAVA_HOME definition (optional)
      # java-home = "/usr/lib/jvm/zulu"
      # JVM Options (optional)
      # Default ones, that can be overridden with precedence:
      # [
      #   "-XX:MaxInlineLevel=20", 
      #   "-XX:MaxTrivialSize=12", 
      #   "-XX:+IgnoreUnrecognizedVMOptions", 
      #   "--add-opens=java.base/java.nio=ALL-UNNAMED", 
      #   "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
      # ]
      #  Based on your instance configuration, you may want to update Xmx and Xms values.
      # jvm-options = ["-Xmx4G", "-Xms512M"]
    }
  ]
}
```

### Engine

The engine specified for a location determines the compatible package formats (JavaScript or JVM) for Gatling packages.

Each engine (`classic` or `javascript`) supports specific Java versions, where `latest` is defaulted.

The table below outlines the supported Java versions for certified Gatling images:

| Engine      | Supported Java Versions |
|-------------|-------------------------|
| classic     | 21 or latest            |
| javascript  | latest                  |

{{< alert info >}}
For the `javascript` engine, only the latest Java version is supported, which corresponds to the GraalVM version used to run Gatling with JavaScript.
{{< /alert >}}

## Configure instances using Terraform {#terraform}

Gatling provides Terraform modules to set up Azure infrastructure for Private Locations. One module specifies the load generator location(s), and the second module deploys the control plane. To use the Terraform module, visit our dedicated [GitHub repository](https://github.com/gatling/gatling-enterprise-control-plane-deployment/tree/main/terraform/examples/AZURE-private-location)
