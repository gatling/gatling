---
title: GCP locations installation
menutitle: Installation
seotitle: Install GCP locations in Gatling Enterprise Cloud
description: How to install a Gatling Control Plane on GCP using Compute Engine, to set up your Private Locations and run load generators in your own GCP network.
lead: Run a Control Plane on GCP using Compute Engine, to set up your Private Locations and run load generators in your own GCP network.
date: 2023-09-03T16:00:00+00:00
lastmod: 2023-09-03T16:00:00+00:00
---

{{<alert tip >}}
Simplify and speed up installation and configuration with Gatling's pre-built [Terraform modules]({{< ref "#terraform" >}}).
{{</alert>}}

GCP [Compute Engine](https://cloud.google.com/compute/docs) is a computing and hosting service that lets you create and run virtual machines on Google infrastructure.

In this example:
- we use **Compute Engine** to configure a service to run the Gatling Control Plane
- Compute Engine runs our Docker containers on a VM on the **GCP** infrastructure
- the [Control Plane configuration]({{< ref "configuration" >}}) file is loaded from a **[Secret Manager](https://cloud.google.com/secret-manager)**

This is only an example.

{{< alert tip >}}
We advise you to create a dedicated Gatling project, as permissions give access to secrets and existing Virtual Machines.
{{< /alert >}}

## Secret Manager

{{< alert info >}}
This section shows how to create a new secret on a [Secret Manager](https://cloud.google.com/secret-manager) for the control plane configuration, 
skip if you already have a secret you want to use.
{{< /alert >}}

In the GCP management console, open the [Secret Manager](https://console.cloud.google.com/security/secret-manager) (or search for "Secret Manager" in the search bar). 
Set the right project and click **CREATE SECRET**.

Choose a name for the secret, and upload the [Control Plane configuration]({{< ref "configuration" >}})

Configure other options as preferred.

Click **CREATE SECRET**.

{{< img src="secret-configuration.png" alt="Configuring the secret" >}}

## Service account

We need a service account which will allow a VM to:

- download the Control Plane's configuration file, stored in a secret
- spawn new load generators on GCP when running a simulation

### Role creation

In the GCP management console, open [Roles](https://console.cloud.google.com/iam-admin/roles) (or search for "Roles" in the search bar).

Enable the following required permissions:
```
compute.disks.create
compute.instances.create
compute.instances.delete
compute.instances.list
compute.instances.setLabels
compute.instances.setMetadata
compute.subnetworks.use
compute.subnetworks.useExternalIp
secretmanager.versions.access
```

{{< img src="role-creation.png" alt="Role creation" >}}

Some permissions may be required based on configuration:
- `compute.images.useReadOnly` when using custom image
- `compute.instanceTemplates.useReadOnly` for instance templates

## Service account creation

In the GCP management console, open [Service accounts](https://console.cloud.google.com/iam-admin/serviceaccounts) (or search for "Service Accounts" in the search bar). 
Click **CREATE SERVICE ACCOUNT**.

Fill details with:
- a name
- an ID
- a description of the control plane service account

{{< img src="service-account-details.png" alt="Service account details" >}}

Then, choose the previously created role.

{{< img src="service-account-role.png" alt="Service account role" >}}

Click **Done**.

## VM instance creation

In the GCP management console, open [VM Instances](https://console.cloud.google.com/compute/instances) (or search for "VM instances" in the search bar).
Click **CREATE INSTANCE**.

Set the VM instance:
- name
- region
- zone

Choose a machine type.

{{< alert warning >}}
A machine type with less than 1 shared core and 2GB of memory may take up to 10 minutes to deploy the instance properly.
_This is due to the startup agent [konlet](https://github.com/GoogleCloudPlatform/konlet) deploying container on VM, and toolbox gcloud CLI installation._
{{< /alert >}}

Click **DEPLOY CONTAINER**.

Set the image to `gatlingcorp/control-plane`, and then click **ADD A VOLUME MOUNT**.

Volume type is `Directory`, mount path is set to `/app/conf/control-plane.conf` and host path to `/etc/control-plane/control-plane.conf`.

The control plane configuration from the secret manager will be downloaded to the host path later.

{{< img src="container-configuration.png" alt="Container configuration" >}}

Configure the Identity and API access for the previously created service account.

{{< img src="vm-identity-api-access.png" alt="Identity API Access" >}}

The last step is configuring an automation that downloads the control plane configuration on start.

To do so, go to advanced options in the Management section and set the Automation startup script to:
```shell
#! /bin/bash

sudo mkdir /etc/control-plane
sudo touch /etc/control-plane/control-plane.conf

toolbox gcloud version
toolbox gcloud secrets versions access latest --secret=<secret-name> | sudo tee /etc/control-plane/control-plane.conf
```

{{< alert info >}}
Do not forget to replace **<secret-name>** with the name of the secret you create for the configuration in the Secret Manager.
{{< /alert >}}


{{< img src="vm-startup-script.png" alt="Identity API Access" >}}

This script downloads the control plane configuration secret from the secret manager on the disk to a path mounted on the control plane container.

Click **Create*

## Your Control Plane is up and running!

After a short time, you should see your Control Plane get the "up" status in Gatling Enterprise Cloud.

{{< img src="gcp-control-plane-status.png" alt="Checking out the Control Plane's status in Gatling Enterprise Cloud" >}}

You can now configure a simulation to run on one or more of this Control Plane's locations!

## Update your control plane

To update your control plane, reset your GCP VM Instance.

Being configured with the image `gatlingcorp/control-plane:latest`, it automatically pulls the latest published version:

1. Go to the **Compute Engine VM Instances** page.
2. Click on the control plane instance.
3. Click the **Reset** button.

Your control plane resets and restarts using the latest image published on Docker Hub.

{{< alert warning >}}
Be aware that this operation resets your instance data, which prevents old control plane images from piling up.
{{< /alert >}}

{{< img src="gcp-cp-update-image.png" alt="Update control plane instance installed on GCP VM Instance" >}}

If you did not use the `latest` tag, you can create a new instance and specify the control plane image you want to deploy. To create a new instance, follow the [create a similar VM](https://cloud.google.com/compute/docs/instances/create-vm-from-similar-instance) instructions and only replace the image configuration.

Directly updating the control plane image is possible but not recommended since you have to start it manually instead of letting GCP manage it.

## Troubleshooting

### Deployment failed

It's important to note that, based on your configuration, certain permissions may be missing, leading to deployment failures.

If you encounter the following error within Gatling Enterprise Cloud:
```
Control plane 'cp_example' failed to deploy private location 'prl_example' failed to deploy: com.google.api.gax.rpc.PermissionDeniedException: Forbidden
```

In this scenario, you should establish a connection to the instance where the control plane container is active. 
Subsequently, you can inspect the container's log by using the command: `docker logs <container-id>`.

Then search for missing permissions exception for the Google Cloud SDK, such as:
```
"errors": [
  {
    "message": "Required 'compute.instanceTemplates.useReadOnly' permission for 'projects/example/global/instanceTemplates/gatling-template'",
    "domain": "global",
    "reason": "forbidden"
  },
  {
    "message": "Required 'compute.images.useReadOnly' permission for 'projects/example/global/images/classic-openjdk-17'",
    "domain": "global",
    "reason": "forbidden"
  },
  {
    "message": "Required 'compute.instances.setServiceAccount' permission for 'projects/example/zones/europe-west3-a/instances/unusedName'",
    "domain": "global",
    "reason": "forbidden"
  }
]
```

### Control plane is down

Containers take some time to become operational on the VM when deploying containers with konlet.
To verify the status of the control plane, you can inspect the instance by running the command: `docker ps -a`.

If docker ps displays the konlet container as running, it indicates that the control-plane container has not yet been deployed.

If you do not see any output for an extended period, it's advisable to review the konlet logs using the command: `sudo journalctl -u konlet-startup`.

### Timeout 

Control plane deployed the instances successfully; however, a timeout occurred during the deployment process.
This is likely due to issues with the load generator initialization script, such as missing requirements on a custom image or lack of internet access.

You can examine these logs directly within containers by executing the command: `sudo journalctl -u google-startup-scripts.service`.

If the instance is stopped, it can be maintained in an operational state by setting debug.keep-load-generator-alive to true in the location configuration. 
**However, remember to delete it manually when no longer needed.**

## Deploy infrastructure using Terraform {#terraform}

Gatling provides Terraform modules to set up AWS infrastructure for Private Locations. One module specifies the load generator location(s), and the second module deploys the control plane. To use the Terraform module, visit our dedicated [GitHub repository](https://github.com/gatling/gatling-enterprise-control-plane-deployment/tree/main/terraform/examples/GCP-private-location)
