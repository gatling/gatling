---
title: Azure locations installation
menutitle: Installation
seotitle: Install Azure locations in Gatling Enterprise Cloud
description: How to install a Gatling Control Plane on Azure using Container Apps.
lead: Run a Control Plane on Azure using Container Apps and Azure Files, to set up your Private Locations and run load generators in your own Azure network.
date: 2023-03-04T16:00:00+00:00
lastmod: 2023-03-04T16:00:00+00:00
---

{{<alert tip >}}
Simplify and speed up installation and configuration with Gatling's pre-built [Terraform modules]({{< ref "#terraform" >}}).
{{</alert>}}

First of all, you should have followed [introduction]({{< ref "../introduction/" >}}) instructions to configure Gatling Cloud for receiving a new control plane. Please check this section first.

[Azure Container Apps](https://azure.microsoft.com/en-us/products/container-apps) is a managed serverless container services. You can use it to deploy your own control plane instances without creating dedicated virtual machines.

Keep in mind that this is only one way to install a control plane in Azure. You can use any other Azure service able to deploy a container.

## Azure services overview

Following Azure services are needed for this control plane installation:
- [Container Apps](https://azure.microsoft.com/en-us/products/container-apps): the base product for containerized serverless applications
- [Azure Files](https://azure.microsoft.com/en-us/products/storage/files): file share for configuration file storage

## Mounting the configuration file

Control plane locations configuration is based on a file that has to be shared with the application.

See [control plane configuration]({{< ref "../introduction/#configuration" >}}) sections of Gatling Cloud documentation for details.

### Azure storage account

{{< alert info >}}
If you already have a storage account, you can use it to create the file share and jump to the next section.
{{< /alert >}}

Azure storage account can contain and manage any data object storage you need for your applications. For details, please refer to [Azure documentation](https://learn.microsoft.com/en-us/azure/storage/common/storage-account-overview).

On your Azure portal, click on "Create a resource" button, and choose "Storage account"

{{< img src="azure-cp-installation-create-resource.png" alt="Create resource" >}}

From there, configure a new storage account according to your needs and policies.

Write down the storage account name, you will need it in further steps.

{{< img src="azure-cp-installation-create-storage-account.png" alt="Create storage account" >}}

### Azure File Share

#### File Share creation

Once again, refer to the [Azure File Share documentation](https://learn.microsoft.com/en-us/azure/storage/files/storage-how-to-use-files-portal?tabs=azure-portal) for details.

In your storage account, let's create a File Share for the [Control Plane configuration]({{< ref "configuration" >}}) file:
* Access to your storage account page
* Click on "File shares" in the side menu
* Click on "File share" creation button in the top menu
* Configure and finalize creation

Write down the file share name, you will need it in further steps.

{{< img src="azure-cp-installation-create-file-share.png" alt="Create file share" >}}

#### Upload control plane configuration file

Upload your configuration file in your new File Share in order to make it available for the control plane:
* Access to your File share page
* Click on "Overview" in the side menu
* Click on "Upload" button in the top menu
* Select your control plane configuration file
* Validate for uploading it

{{< img src="azure-cp-installation-upload-configuration-file.png" alt="Upload configuration file" >}}

You should see your control plane configuration file in the File Share.

Please refer to [Azure private locations configuration]({{< ref "configuration" >}}) for more details about the configuration file.

## Container App

A Container App is what will manage the proper control plane application.

There are several ways to create and configure it, as described in [Azure Container Apps documentation](https://learn.microsoft.com/en-us/azure/container-apps/quickstart-portal). We are going to consider only the Azure portal way of doing it.

{{< alert warning >}}
Container App creation steps on Azure portal do not allow to configure everything you need, like the File Share mounting. Make sure to go through all steps for proper configuration.
{{< /alert >}}

### Control plane application creation

On your Azure portal, click on "Create a resource" button, and choose "Container Apps".

{{< img src="azure-cp-installation-create-resource.png" alt="Create resource" >}}

Start by referencing basic information about your app, as requested by the wizard.

You can configure freely the subscription, group, etc. at this step, according to your policies.

{{< alert info >}}
If an environment already exists, it may be automatically selected by the creation wizard.
You should create a new environment, dedicated to this application.
{{< /alert >}}

{{< img src="azure-cp-installation-create-app1.png" alt="Create application step 1" >}}

You will then need to configure the Gatling control plane container as the base image of the application:
* Go to "App settings tab"
* Uncheck "Use quickstart image"
* Configure a container name as you desire
* Choose "Docker Hub or other registries" as image source
* Choose "Public" as image type
* Indicate `docker.io` in registry login server field
* Indicate `gatlingcorp/control-plane:latest` in image and tag field

Refer to [private location introduction]({{< ref "../introduction/#installation" >}}) section of this documentation for more information about this docker image.

{{< img src="azure-cp-installation-create-app2.png" alt="Create application step 2" >}}

Finally, review and create your control plane application. This will create two new resources in the configured resource group:
* The Container App itself
* A Container App Environment, associated to the Container App

### Azure credentials

#### Required Permissions

Before configuring Azure credentials, ensure that your application has the required permissions to manage Azure virtual machines across the entire control plane lifecycle.

Refer to the Azure documentation on [Create or update Azure custom roles using the Azure portal](https://learn.microsoft.com/en-us/azure/role-based-access-control/custom-roles-portal), 
specifically the [Start from JSON](https://learn.microsoft.com/en-us/azure/role-based-access-control/custom-roles-portal#start-from-json) section.

Use the JSON below to configure the role by defining the required permission actions:
```json
{
    "properties": {
        "roleName": "GatlingControlPlane",
        "description": "",
        "assignableScopes": [
            "/subscriptions/{SubscriptionId}"
        ],
        "permissions": [
            {
                "actions": [
                    "Microsoft.Compute/galleries/images/versions/read",
                    "Microsoft.Compute/virtualMachines/read",
                    "Microsoft.Compute/virtualMachines/write",
                    "Microsoft.MarketplaceOrdering/agreements/offers/plans/read",
                    "Microsoft.MarketplaceOrdering/agreements/offers/plans/sign/action",
                    "Microsoft.MarketplaceOrdering/offertypes/publishers/offers/plans/agreements/read",
                    "Microsoft.MarketplaceOrdering/offertypes/publishers/offers/plans/agreements/write",
                    "Microsoft.Network/networkInterfaces/join/action",
                    "Microsoft.Network/networkInterfaces/write",
                    "Microsoft.Network/publicIPAddresses/write",
                    "Microsoft.Network/virtualNetworks/read",
                    "Microsoft.Network/virtualNetworks/subnets/join/action",
                    "Microsoft.Resources/subscriptions/resourceGroups/delete",
                    "Microsoft.Resources/subscriptions/resourceGroups/read",
                    "Microsoft.Resources/subscriptions/resourceGroups/write",
                    "Microsoft.Storage/storageAccounts/listkeys/action",
                    "Microsoft.Storage/storageAccounts/read"
                ],
                "notActions": [],
                "dataActions": [],
                "notDataActions": []
            }
        ]
    }
}
```

{{< alert warning >}}
When creating the role, ensure that the `assignableScopes` property is pre-filled with your actual subscription ID. 
Do not leave it as `/subscriptions/{SubscriptionId}` or overwrite it with the placeholder. 
Replace it with the correct subscription ID.
{{< /alert >}}

To grant these permissions, create a new role and assign it to the managed identity as detailed below.

#### Managed identity

Azure managed identities enable you to automatically configure control plane credentials using an Active Directory identity, 
which is automatically set up when the application starts. 

For more information, refer to the [Azure managed identities for container apps documentation](https://learn.microsoft.com/en-us/azure/container-apps/managed-identity?tabs=portal%2Cdotnet).

Steps to configure:
1. Navigate to your application page.
2. Click on "Identity" in the side menu.
3. Set the status to "On."
4. Wait for the automatic setup of the managed identity.
5. Click on Azure role assignments and set role with required permissions

{{< img src="azure-cp-installation-crendentials-identity.png" alt="Setup Azure credentials with managed identity" >}}

A managed identity is automatically assigned to your application instances and will be used by the control plane to manage Azure resources.

### Mounting configuration file share

#### Configure in environment

Before effectively mounting the File Share you created earlier, you need to configure access to it in the [Container App Environment](https://learn.microsoft.com/en-us/azure/container-apps/environment):
* Navigate to the Container App Environment newly created
* Click on "Azure Files" in the side menu
* Click on "Add" button in the top menu
* "Name" field is free, this is the name you will refer to in the application container configuration
* "Storage account name" and "Storage account key" can be found in the "Access keys" section of your storage account page
* "File share" should be the file share name you created earlier

{{< img src="azure-cp-installation-configure-env-file-share.png" alt="Configure file share in environment" >}}

Double check info entered manually, and validate the creation.

#### Configure application containers

We can now mount the [File Share](https://learn.microsoft.com/en-us/azure/storage/files/storage-files-introduction) on application containers:
* Navigate to your application page on the portal
* Click on "Container" in the side menu (you can check their configuration while you are here)
* Click on "Edit and deploy" button in the top menu
{{< img src="azure-cp-installation-configure-container-file-share1.png" alt="Configure containers file share step 1" >}}

* In the "Container" tab, click on the existing container automatically created at application creation. Or, there is none, click on the "Add" button"
{{< img src="azure-cp-installation-configure-container-file-share2.png" alt="Configure containers file share step 2" >}}

* Go to "Volume mounts" tab
* Select your newly created file share in "File share name" drop box
* Enter "/app/conf" as "Mount path (folder)"
* Check next step before validating changes (additional configuration) since you can do it at the same time.
{{< img src="azure-cp-installation-configure-container-file-share3.png" alt="Configure containers file share step 3" >}}

### Additional containers configuration

During the application container configuration step, you can set scale rules for your application.

{{< alert warning >}}
We strongly advise to set the minimum scale to 1, to ensure that your control plane will always be up.
{{< /alert >}}

* In the "Scale" tab, set the min and max instances with the ruler
* Set rules if you need to (not mandatory)
* Don't forget to validate your changes

{{< img src="azure-cp-installation-configure-container-scale.png" alt="Configure containers instances minimum scale" >}}

## Use your new control plane

Once you've configured your control plane container, it should automatically start a new revision with desired configuration.

You can check logs in your application "Log stream" menu, or perform more complex requests in "Logs" menu.
Be sure to select the currently active revision logs.

Now that your control plane is up and running, after a short time you should see your control plane with {{< badge success >}}up{{< /badge >}} status in Gatling Enterprise Cloud.

{{< img src="azure-cp-status.png" alt="Checking out the Control Plane's status in Gatling Enterprise Cloud" >}}

## Update your control plane

To update your control plane, restart your Azure Container App.

Being configured with the image `gatlingcorp/control-plane:latest`, it will automatically pull the latest published version:

1. Go on your **Container App** page
2. Click on the running revision
3. Hit the **Restart** button

Your control plane restarts, using the latest image published on Docker Hub.

{{< img src="azure-cp-update-image.png" alt="Update control plane instance installed on Azure Container App" >}}

If you did not use the tag `latest`, you will have to [create a new revision](https://learn.microsoft.com/en-us/azure/container-apps/revisions-manage?tabs=bash#updating-your-container-app) and specify the control plane image you want to deploy.

## Deploy infrastructure using Terraform {#terraform}

Gatling provides Terraform modules to set up Azure infrastructure for Private Locations. One module specifies the load generator location(s), and the second module deploys the control plane. To use the Terraform module, visit our dedicated [GitHub repository](https://github.com/gatling/gatling-enterprise-control-plane-deployment/tree/main/terraform/examples/AZURE-private-location)
