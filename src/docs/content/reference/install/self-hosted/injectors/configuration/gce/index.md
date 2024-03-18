---
title: Google Compute Engine
seotitle: GCE injectors with Gatling Enterprise Self-Hosted
description: Learn how to configure GCE injectors for Gatling Enterprise.
lead: Learn how to configure GCE injectors for Gatling Enterprise.
date: 2021-03-26T09:40:40+01:00
lastmod: 2022-03-10T09:18:38+00:00
---

Like the AWS pool, a GCE pool is a reference to the GCP account you want to use to spawn injectors to run the simulation.

{{< alert warning >}}
Make sure your network configuration will let Gatling Enterprise connect to your injectors on port 22 (SSH) and 9999 (HTTP).
{{< /alert >}}

To configure the type of instances you want to spawn, you need to fill the form below:

{{< img src="gce.png" alt="GCE pool" >}}

## Main Settings

- **Team**: Set if the pool is global or owned by a team

## Credentials Settings

- **Credentials**: If you're running Gatling Enterprise on GCE or using `GOOGLE_APPLICATION_CREDENTIALS` to configure access, use `Application Default`. Otherwise, use `JSON credentials`.

When creating a virtual machine in GCE, it gives you two ways to configure accesses in the **Identity and API access** section of the **Create an instance** form. Using a **service account** is one of them.

{{< alert warning >}}
If you use the default compute engine service account, Gatling Enterprise might returns errors. Left untouch, this service account doesn't have enough permissions to spawn virtual machines.

Make sure your use a service account that has enough permissions before using the Application Default feature.
{{< /alert >}}

In order to create a service account with sufficient permissions, you need to do the following:

1. Create a service account
2. Create a custom role with the wanted permissions or use `roles/compute.admin` (Compute Admin) if you don't need fine-grained permissions
3. Create a principal in the IAM section of GCP with the previous service account and the custom role

The required permissions in the **Custom Role** are, at least:

```
compute.addresses.list
compute.addresses.use
compute.disks.create
compute.instanceTemplates.list
compute.instances.create
compute.instances.delete
compute.instances.get
compute.instances.setMetadata
compute.instances.setTags
compute.machineTypes.list
compute.regions.list
compute.subnetworks.list
compute.subnetworks.use
compute.subnetworks.useExternalIp
```

If you want to use instance templates, you will need to add the `roles/iam.serviceAccountUser` role (Service Account User) to the principal. This will allow Gatling Enterprise to assign a service account to the instance group. You will also need to add the following permissions to the previous custom role:

```
compute.instanceGroupManagers.create
compute.instanceGroupManagers.delete
compute.instanceGroups.create
compute.instanceGroups.delete
compute.instanceGroups.get
compute.instanceTemplates.get
compute.instanceTemplates.useReadOnly
```

{{< alert tip >}}
The instance template itself doesn't need any specific service account.
{{< /alert >}}

Finally, it is also possible to setup **Access Scopes** directly on the instance running Gatling Enterprise, but it is a [legacy feature of GCP and is **not** recommended to use](https://cloud.google.com/iam/docs/best-practices-for-securing-service-accounts#access-scopes).

## Instance Settings

- **Zone**: the zone where you want to spawn your injectors
- **Private Key**: the previously added [private key]({{< ref "../../../../execute/self-hosted/admin/private-keys" >}}) used by your Template
- **Connect to private IP**: Gatling Enterprise will connect to the injectors' private IP instead of the public one. If unchecked, the private IP remains a fallback if a public IP is missing. This option should be used only when the Gatling Enterprise host and the injector are both in the same GCE network.
- **Deployment**: You can choose to spawn GCE instances from an image or an instance template

Specific configuration if you chose Image:

- **Image**: the image you want to use for your instances. You can use our certified Images or the url of your custom Image (the Image should at least have JDK8 installed and a configured key pair without password)
- **Machine type**: this machine type will be used by the injectors. We recommend using n1-highcpu-4 or n1-highcpu-8 machines.
- **Subnetwork**: the subnetwork the instances will use
- **Preemptible**: check this if you want to use preemptible instances (cheaper, but can be reclaimed by GCE)
- **Network tags**: networks tags you may want to apply to the instances
- **Use Static IPs**: check this if you want your injectors to use predefined static IPs

Specific configuration if you chose Instance Template:

- **Template**: the template used for your instances, the template should at least have JDK8 installed, a configured key pair without password and the port 22 & 9999 should be open
- **Username**: the username used by your ssh command to connect to the instances
