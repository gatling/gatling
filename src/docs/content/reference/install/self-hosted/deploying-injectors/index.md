---
title: Deploying injectors
seotitle: Deploy injectors with Gatling Enterprise Self-Hosted
description: Preliminary configuration of Gatling Enterprise's injectors.
lead: Find generic information about Gatling Enterprise injectors, and specific information about each supported cloud provider.
date: 2021-03-26T17:41:00+01:00
lastmod: 2021-08-16T17:55:36+02:00
---

Gatling Enterprise enable users to configure either on demand or on-premises pools.
In Gatling Enterprise, pools are instances cluster where you deploy Gatling instances and your simulations.

Valid characters for a pool name are letters, digits, spaces, dashes and underscores.

## Injector requirements

### Architecture

We support AMD64 and ARM for both the Gatling Enterprise server and the Gatling injectors.

### Image

The hosts running the Gatling injectors must:

* run on Linux 64 bits, with Kernel >= 3.10
* have a JDK 8, 11 or 17 installed. We recommend using the  [OpenJDK Zulu builds from Azul System](https://www.azul.com/downloads/?package=jdk#download-openjdk).
* have a passphrase-less SSH key
* `/tmp` not mounted with `noexec`

We recommend that you tune your OS for maximum performance.
Please check the [Gatling documentation]({{< ref "../../script/core/operations#os-tuning" >}}).

We also recommend that you disable automatic updates and automatic package repositories listing updates.
This could eventually lead to upgrading the JDK while running, which could break your simulation run.

### Network

The hosts running Gatling injectors must be reachable from Gatling Enterprise:

* over SSH (port 22)
* over HTTP, on port 9999 (by default, configurable in `frontline.conf`)

### CPU/Memory

Unless your simulation keeps huge amounts of data in memory (eg. for feeders with a lot of data), Gatling is mostly CPU-bound and IO-bound, and will take advantage of CPU and network-heavy hosts.

Therefore, we recommend the following instances:

* AWS: `c5.xlarge` or `c6g.xlarge` (4 cores), or `c5.2xlarge` or `c6g.2xlarge` (8 cores)
* GCE: `n1-highcpu-4` (4 cores) or `n1-highcpu-8` (8 cores)
* Azure: `F4` (4 cores) or `F8` (8 cores)

We strongly discourage the use of CPU-burstable instance types:

* AWS: `T2`/`T3` instances
* GCE: `f1-micro`, `g1-small`
* Azure: `Bs` instances

These instance types target light workloads, but sporadic peaks of activity where they can benefit from a temporary increase in CPU performance.

Gatling is more likely to use 100% of the CPU cores and will not benefit from CPU bursts.

## Certified images

We provide certified, pre-built images for AWS, Azure, GCE & Docker.
Those images are available for OpenJDK 8, 11 and 17, and include all recommended optimisations.
If you can't use our certified images, we provide the templates from which our certified images are built:

* For AWS & GCE: [frontline-injector-playbook](https://github.com/gatling/frontline-injector-playbook/) (requires Ansible & Packer)
* For Docker: [frontline-injector-docker-image](https://github.com/gatling/frontline-injector-docker-image)
## Local

It's possible to have Gatling Enterprise use a "Local" pool to deploy a single injector on the same host.
This option is turned off by default and has to be enabled in the `frontline.conf` file:

```hocon
frontline {
  injector {
    enableLocalPool = true
  }
}
```

This option is only intended to be used for demos and as a quick start when evaluating Gatling Enterprise.

{{< alert warning >}}
It should definitively be disabled once your Gatling Enterprise installation will go live, or you'd risk ending up with Gatling Enterprise lacking resources (CPU, network) because a load test is eating all of them.
{{< /alert >}}

## On-premises

It's very easy to configure on-premises pools from Gatling Enterprise:

* Create a pool
* Create a host by providing hostname, username, credentials and optional custom working directory (default is `/tmp`). The working directory should be executable.
* Assign the created pool to this host

## On Demand

Gatling Enterprise is currently managing five different cloud providers: AWS, GCE, OpenStack and Microsoft Azure.

### AWS (On-premises license & AWS marketplace)

Gatling Corp provides certified AMIs that you choose in the Gatling Enterprise AWS configuration. This AMI will be used as a base for your injectors. However, you can still build a custom one with a JDK 8, 11 or 17 installed, a key pair without password configured and the port 22 and 9999 opened.

You'll also need to configure AWS API access keys on the Gatling Enterprise host using one of these methods:

1. If you've installed Gatling Enterprise on AWS EC2, you can directly [set a IAM Role to the instance](http://docs.aws.amazon.com/general/latest/gr/aws-access-keys-best-practices.html).
2. Environment Variables – AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
3. Java System Properties – aws.accessKeyId and aws.secretKey
4. The default credential and config files. See [Set up AWS Credentials and Region for Development](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)

{{< alert tip >}}
Gatling Enterprise requires the following permissions (or grant `AmazonEC2FullAccess` if you don't care about fine-grained permissions):
{{< /alert >}}

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "ec2:Describe*",
        "ec2:CreateTags",
        "ec2:RunInstances",
        "ec2:TerminateInstances",
        "ec2:RequestSpotInstances",
        "ec2:CancelSpotInstanceRequests",
        "ec2:AllocateAddress",
        "ec2:AssociateAddress",
        "iam:GetInstanceProfile",
        "iam:ListInstanceProfiles",
        "iam:PassRole" <1>
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
```

<1> ONLY REQUIRED WHEN SETTING INSTANCE PROFILE ON INJECTORS

### GCE (On-premises license only)

There are requirements before creating a GCE pool:

* Create a project from Google console
* Enable `Google Compute Engine API` from Google API Manager console
* If not using Application Default credentials, create a Service Account key from Google console: API & Services => Credentials => Create credentials => Service account key (only JSON is supported).

{{< alert tip >}}
The GCE Account used must have the `instanceAdmin` role.
{{< /alert >}}

### OpenStack (On-premises license only)

There are requirements before creating a OpenStack pool:

* Get credentials information from `Access & Security` tab.
* Create an image (snapshot) from an existing instance.

{{< alert tip >}}
The OpenStack User might need some special permissions to launch instances.
{{< /alert >}}

### Microsoft Azure (On-premises license & Azure marketplace)

Gatling Corp provides certified images that you choose in the Gatling Enterprise Microsoft Azure configuration. This image will be used as a base for your injectors. However, you can still build a custom one with a JDK 8, 11 or 17 installed, a key pair without password configured and the port 22 and 9999 opened.

There are requirements before creating an Azure pool:

* Create a virtual network.
* Create an image by following the [Azure documentation](https://docs.microsoft.com/en-us/azure/virtual-machines/virtual-machines-linux-capture-image) if not using certified images.
* Create and save a SSH key pair without password.

There are two ways to provide Azure credentials to your Gatling Enterprise instance:

* Get the credentials from Microsoft Azure: follow [this link](https://www.inkoop.io/blog/how-to-get-azure-api-credentials/) and save the subscription ID, application ID, directory ID and client secret.
* Use [User Assigned Managed Identities](https://docs.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/qs-configure-portal-windows-vm#user-assigned-managed-identity).

{{< alert warning >}}
In both cases, the Azure User used must have the `Contributor` permission.
{{< /alert >}}

#### Create a User Assigned Managed Identity

Identities can be created using either the Azure Portal or the Azure CLI.

{{< alert warning >}}
Role should be set under the subscription scope, otherwise Contributor will not suffice. It can’t be set under the resource group scope as Gatling Enterprise will start Azure virtual machines in a new resource group each time it launches a simulation.
{{< /alert >}}

Here are the creation steps:

* Go to [https://portal.azure.com/#create/Microsoft.ManagedIdentity](https://portal.azure.com/#create/Microsoft.ManagedIdentity), set resource name, resource group and location.
* Go to your subscription, click on **Access Control (IAM)**, then Add role assignment.
* Role should be set to Contributor.
* Set "Assign access to" to User assigned managed identity and select the name of your Managed Identity.
* Go to the Virtual machine configuration, then go to the Identity tab of the Virtual Machine, assign the previous identity to your virtual machine.

{{< alert warning >}}
Make sure to assign the User Assigned Managed Identity from the **Identity tab of the Virtual Machine** hosting Gatling Enterprise. Using the Access Control (IAM) will result in an error when trying to use the identity at a later stage.
{{< /alert >}}

### Kubernetes / OpenShift {#kubernetes-openshift}

There are some requirements before creating a Kubernetes/OpenShift pool:

* `kubectl` must be available in the `PATH` of the Linux user running Gatling Enterprise. This is taken care of in our installer or in our AWS and Azure MarketPlace offers, but this might not be the case if you manually handpicked and installed each component.
* Docker Hub should be reachable from your infrastructure, otherwise you'll need to [build the injector image](https://github.com/gatling/frontline-injector-docker-image) and push it to your private registry.
* A service account able to manage pods and services (either attached to Gatling Enterprise or for its token).

Additionally, if Gatling Enterprise is deployed outside Kubernetes/OpenShift:

* The Kubernetes API should be reachable by Gatling Enterprise.
* If using the NodePort mode, firewall rules must be added so that Gatling Enterprise can reach Kubernetes nodes on the configured Kubernetes NodePort range (by default, 30000-32767).
* If using the Ingress or Route modes, the Gatling Enterprise server will create ingress rules and connect to the injectors on port 9999 (HTTP).

{{< alert tip >}}
If your cluster uses RBAC, you'll need a role with the following permissions for Gatling Enterprise's service account:
{{< /alert >}}

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: frontline-manage-injectors
rules:
  - apiGroups: [""]
    # "services" can be skipped if Gatling Enterprise is deployed inside Kubernetes
    resources: ["pods", "pods/exec", "services"]
    verbs: ["create","delete","get","list","patch","update","watch"]
```
