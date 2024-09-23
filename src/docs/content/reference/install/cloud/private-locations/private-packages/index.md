---
title: Private packages
seotitle: Private packages for Gatling Enterprise Cloud
description: How to install and use a private repository on your Control Plane.
lead: Store your simulations' packages privately in your infrastructure, and use them with private locations.
date: 2023-08-09T12:00:00+00:00
lastmod: 2023-08-09T12:00:00+00:00
---

## Introduction

Private Locations facilitate running simulations in your dedicated cloud environment. 
Ensure secure storage of sensitive simulation packages in your private cloud. 

The control plane offers a private repository; enable it for confidential package management!

## Private packages

A private package is uploaded through the control plane into a private repository.
_Gatling Enterprise Cloud only receives the Gatling version associated with the package and the names of simulation classes, which helps in simulation configuration_

When initiating a Gatling run, the control plane generates a temporary signed link to allow the download of the private package from the load generators.

{{< img src="private_packages_general_architecture.png" alt="Infrastructure schema" >}}

{{< alert info >}}
Private Packages are based on Private Locations. You can not use them with managed locations.
{{< /alert >}}

## Infrastructure

Currently, Private Packages support the following underlying storages:

* AWS S3
* GCP Cloud Storage
* Azure Blob Storage
* the Control Plane host filesystem

{{< alert info >}}
Before going further, ensure that your repository is ready to hold your packages.
{{< /alert >}}

### Control plane server {#control-plane-server}

The control plane with a private repository has a server to manage uploads to the repository, secured by a Gatling Enterprise Cloud API Token with `Configure` role.

The server is accessible on port 8080 by default when a repository is configured.
The following **optional server configuration** with the default settings is provided for your reference.

```bash
control-plane {
  repository {
    # Upload configuration (optional)
    upload {
      directory = "/tmp" # (optional, default: /tmp)
    }
    # Server configuration (optional)
    server {
      port = 8080 # (optional, default: 8080)
      bindAddress = "0.0.0.0" # (optional, default: 0.0.0.0)
      
      # PKCS#12 certificate (optional)
      certificate {
        path = "/path/to/certificate.p12"
        password = ${CERTIFICATE_PASSWORD} # (optional)
      }
    }
  }
}
```

This configuration includes the following parameters:
- **upload.directory**: This directory temporarily stores uploaded JAR files. (optional)
- **server.port**: The port on which the control plane is listening for private package uploads.
- **server.bindAddress**: The network interface to bind to. The default is `0.0.0.0`, which means all available network IPv4 interfaces.
- **server.certificate**: The server P12 certificate for secure connection without SSL reverse proxy. (optional)

### Control plane repository

#### AWS S3

{{< alert warning >}}
Control plane with private repository needs AWS permissions `s3:PutObject`, `s3:DeleteObject` and `s3:GetObject` on the bucket.

To download a private package, the location requires outbound connection access to `https://<bucket>.s3.<region>.amazonaws.com`.

To upload a private package using HTTPS, please check this [section]({{< ref "#enableHttps" >}})
{{< /alert >}}

Once it is done, add the private repository configuration section in your [control plane configuration]({{< ref "introduction" >}}) file:

```bash
control-plane {
  repository {
    # S3 Bucket configuration
    type = "aws"
    bucket = "bucket-name"
    path = "folder/to/upload" # (optional, default: root)
  }
}
```

This configuration includes the following parameters:
- **bucket**: The name of the bucket where packages are uploaded to on AWS S3.
- **path:** The path of a folder in AWS S3 bucket. (optional)

{{<alert tip>}}
Simplify and speed up your AWS installation and configuration with Gatling's pre-built [Terraform modules]({{< ref "#configure-private-packages-with-terraform-aws" >}})
{{</alert>}}

#### GCP Cloud Storage

{{< alert warning >}}
Control plane with private repository needs GCP service account role with permissions `storage.objects.create`, 
`storage.objects.delete` and `iam.serviceAccounts.signBlob` on the bucket.

To download a private package, the location requires outbound connection access to `https://storage.googleapis.com/<bucket>`.

To upload a private package using HTTPS, please check this [section]({{< ref "#enableHttps" >}})
{{< /alert >}}

```bash
control-plane {
  repository {
    # Cloud Storage Bucket configuration
    type = "gcp"
    bucket = "bucket-name"
    path = "folder/to/upload" # (optional, default: root)
    project = "project-name"
  }
}
```

This configuration includes the following parameters:
- **bucket**: The name of the bucket where packages are uploaded to on GCP Cloud Storage.
- **path:** The path of a folder in Cloud Storage bucket. (optional)

#### Azure Blob Storage

{{< alert warning >}}
Control plane with private repository needs to be associate with Azure storage account role `Storage Blob Data Contributor`.
For more information, check [Authenticate to Azure and authorize access to blob data](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java?tabs=powershell%2Cmanaged-identity%2Croles-azure-portal%2Csign-in-azure-cli#authenticate-to-azure-and-authorize-access-to-blob-data)

To download a private package, the location requires outbound connection access to `https://<storage-account>>.blob.core.windows.net/<container>`
{{< /alert >}}

```bash
control-plane {
  repository {
    # Azure Blob Storage configuration
    type = "azure"
    storage-account = "storage-account-name"
    container = "container-name"
    path = "folder/to/upload" # (optional, default: root)
  }
}
```

{{<alert tip>}}
Simplify and speed up your Azure installation and configuration with Gatling's pre-built [Terraform modules]({{< ref "#configure-private-packages-with-terraform-azure" >}})
{{</alert>}}

#### Filesystem Storage

{{< alert warning >}}
To download a private package, the location requires outbound connection access to configured `download-base-url`.
{{< /alert >}}

This option allows the storage of simulations directly on the control-plane filesystem.
```bash
  repository = {
    # Filesystem configuration
    type = "filesystem"
    # Directory to store your private packages
    directory = "/data/gatling-repository"
    upload {
      # Directory to temporarily store your incoming simulation during the upload process
      directory = "/tmp" # (optional, default: /tmp)
    }
    location {
      # URL of your control-plane from your private locations
      download-base-url = "http://www.example.com:8080"
    }
  }
```

{{< alert warning >}}
Please note that the optional `upload.directory` configuration, which defaults to /tmp, will be used to temporarily store your incoming simulation during the upload process.
Once the upload is complete, the file will be stored in your configured directory (`/data/gatling-repository` in the provided example).
{{< /alert >}}

This configuration includes the following parameters:
- **directory**: The directory where the simulations will be stored.
- **location.download-base-url**: The access URL for the control-plane. This URL will be provided to the load-generators so that they can download your simulations. 


### Configure Private Packages with Terraform

Gatling provides Terraform modules to set up your infrastructure for Private Locations with Private Packages. There are three required modules for a successful setup:

- specify the load generator location(s),
- specify the private package,
- deploy the control plane.

#### AWS S3 {#configure-private-packages-with-terraform-aws}

To use the Terraform module to setup your AWS Private Package infrastructure, visit our dedicated [GitHub repository](https://github.com/gatling/gatling-enterprise-control-plane-deployment/blob/main/terraform/examples/AWS-private-package).

#### Azure Blob Storage {#configure-private-packages-with-terraform-azure}

To use the Terraform module to setup your Azure Private Package infrastructure, visit our dedicated [GitHub repository](https://github.com/gatling/gatling-enterprise-control-plane-deployment/blob/main/terraform/examples/AZURE-private-package).

#### GCP Cloud Storage {#configure-private-packages-with-terraform-gcp}

To use the Terraform module to setup your GCP Private Package infrastructure, visit our dedicated [GitHub repository](https://github.com/gatling/gatling-enterprise-control-plane-deployment/blob/main/terraform/examples/GCP-private-package).

### Upload Private Packages using HTTPS {#enableHttps}

#### AWS

To enable HTTPS for your Control Plane container, there are two options:

- Using an Application Load Balancer (ALB) (Recommended for production)
  - Obtain a valid Domain Name and TLS Certificate. You can use AWS Certificate Manager for simplicity.
  - Create an Application Load Balancer and configure it to listen on port 443.
  - Attach TLS Certificate to the Application Load Balancer.
  - If you optionally wish to implement TLS encryption on the traffic between ALB and Control Plane server, generate a certificate for the server and update [repository server configuration]({{< ref "#control-plane-server" >}}) in the Control Plane configuration with the generated certificate.
  - Register your Control Plane as a target group associated with the ALB.
  - Update ALB Security Group to allow inbound traffic on port 443 and allow outbound on your server's port (default: 8080) for the Control Plane Security Group.
  - Update your Route53 or DNS provider settings to point domain or subdomain to the ALB using a CNAME record.
- Direct IP Aliasing
  - Obtain a valid Domain Name and TLS Certificate.
  - Update the [repository server configuration]({{< ref "#control-plane-server" >}}) in the Control Plane configuration with the generated certificate.
  - Update your Route53 or DNS provider settings to point domain or subdomain to the Control Plane's public IP using an A record.

#### Azure

By default, HTTPS is enabled for your Control Plane container on Azure when Ingress is enabled.

- Use the Application URL with the following: `https://<app-name>.<region>.azurecontainerapps.io`
- Modify Ingress settings in order adjust the Control Plane's Ingress configuration as needed.

#### GCP

To enable HTTPS for your Control Plane container on GCP, there are two options:

- Using a Google Cloud HTTPS Load Balancer (Recommended for production)
  - Obtain a valid domain name and TLS certificate. You can use Google-managed certificates for simplicity.
  - Create a Google Cloud HTTPS Load Balancer and configure it to listen on port 443.
  - Attach the TLS certificate to the HTTPS Load Balancer.
  - If you optionally wish to implement TLS encryption on the traffic between Google Cloud HTTPS Load Balancer and Control Plane server, generate a certificate for the server and update [repository server configuration]({{< ref "#control-plane-server" >}}) in the Control Plane configuration with the generated certificate.
  - Register your Control Plane as a backend service associated with the Load Balancer.
  - Update the firewall rules to allow inbound traffic on port 443 and allow outbound traffic on your server's port (default: 8080) for the Control Plane's network.
  - Update your Cloud DNS settings or your DNS provider to point your domain or subdomain to the Load Balancer's IP address using a CNAME or A record.
- Direct IP Aliasing
  - Obtain a valid domain name and TLS certificate.
  - Update the [repository server configuration]({{< ref "#control-plane-server" >}}) in the Control Plane configuration with the generated certificate.
  - Update your Cloud DNS settings or your DNS provider to point your domain or subdomain to the Control Plane's public IP address using an A record.

## Usage 

After configuration, restart the control plane to start the server.

### Create a private package

To create a private package, use Gatling Plugin deployment commands with control plane URL configured:
- [Maven plugin]({{< ref "/reference/integrations/build-tools/maven-plugin#private-packages" >}})
- [Gradle plugin]({{< ref "/reference/integrations/build-tools/gradle-plugin#private-packages" >}})
- [sbt plugin]({{< ref "/reference/integrations/build-tools/sbt-plugin#private-packages" >}})

### Delete a private package

To delete a private package, delete the package within Gatling Enterprise Cloud. 
The control plane will receive the order to delete the package on the configured private repository.
