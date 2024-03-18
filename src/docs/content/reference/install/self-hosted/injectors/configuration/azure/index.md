---
title: Azure
seotitle: Azure injectors with Gatling Enterprise Self-Hosted
description: Learn how to configure Azure injectors for Gatling Enterprise.
lead: Learn how to configure Azure injectors for Gatling Enterprise.
date: 2021-03-26T09:40:45+01:00
lastmod: 2021-08-16T17:55:36+02:00
---

A Microsoft Azure Pool is a reference to the Azure account you can use to spawn injectors to run the simulation. Only Linux virtual machines are supported.

{{< alert warning >}}
Make sure your network and firewall configuration will let Gatling Enterprise connect to:
* your injectors on port 22 (SSH) and 9999 (HTTP);
* the Microsoft and Azure REST API:
  * `https://login.microsoftonline.com` (Azure IAM)
  * `https://management.azure.com` (Azure REST API)
  * `http://169.254.169.254` (Azure IMDS)

{{< /alert >}}

To configure the type of instances you want to spawn, you need to fill the form below:

{{< img src="azure.png" alt="Azure Pool" >}}

- **Team**: Set if the pool is global or owned by a team
- **Subscription ID**: the Azure subscription you want to use, check [this article](https://www.inkoop.io/blog/how-to-get-azure-api-credentials/) to learn how to retrieve it
- **Application ID**: the id of the Azure application you want to authenticate with
- **Directory ID**: the Azure directory you want to use
- **Client Secret**: the key used to authenticate
- **Region**: the region where you want to spawn your instances
- **Size**: the size of the instances
- **Network**: the network configured on your Microsoft Azure account you want to use
- **Subnet**: the subnet you want to use
- **Connect to private IP**: Gatling Enterprise will connect to the injectors' private IP instead of the public one. If unchecked, the private IP remains a fallback if a public IP is missing. This option should be used only when the Gatling Enterprise host and the injector are both in the same Azure network.
- **Image location (certified)**: select the certified image you want to use for your instances.
- **Image location (custom)**: the Resource Id of the custom image in your Compute Gallery you want to use for your instances. VHD URL are supported, but Azure documentation [suggests to use galleries](https://learn.microsoft.com/en-us/azure/virtual-machines/vm-specialized-image-version).

{{< alert tip >}}
A custom image must meet those requirements:
- It must be a [generalized image](https://learn.microsoft.com/en-us/azure/virtual-machines/shared-image-galleries?tabs=azure-cli#generalized-and-specialized-images)
- JDK8+ installed
- A configured key pair without password
- Port 22 and port 9999 should be open

Refer to Azure documentation to learn more about creating and storing a generalized image:
- [How to generalize a VM](https://learn.microsoft.com/en-us/azure/virtual-machines/generalize#linux)
- [How to store an image in Azure Compute Gallery](https://learn.microsoft.com/en-us/azure/virtual-machines/shared-image-galleries?tabs=azure-cli)
{{< /alert >}}

- **Public Key**: the public ssh key to connect to your instances
{{< alert warning >}}
[Ed25519 keys are not supported](https://docs.microsoft.com/en-us/troubleshoot/azure/virtual-machines/ed25519-ssh-keys) in Azure.
{{< /alert >}}

- **Username**: the username used by your ssh command to connect to the instances
{{< alert warning >}}
Azure has some requirements about the username:

- It must be less than 20 characters
- It cannot end with a period (`.`)
- Many usernames are forbidden, such as `admin` and `root`

Check the [Azure documentation](https://docs.microsoft.com/en-us/azure/virtual-machines/windows/faq#what-are-the-username-requirements-when-creating-a-vm-) for all the details.
{{< /alert >}}

- **Private Key**: the previously added [private key]({{< ref "../../../../execute/self-hosted/admin/private-keys" >}}) associated with the public ssh key

It's also possible to use User Assigned Managed Identities, refer to the installation guide if you want to create a Managed Identity:

{{< img src="azure-msi.png" alt="User Assigned Managed Identities" >}}
