---
title: Package Configuration
seotitle: Configure packages in Gatling Enterprise Cloud
description: Learn how to configure and upload a package to Gatling Enterprise Cloud.
lead: Configure and upload your package to Gatling Enterprise Cloud.
date: 2021-03-10T14:29:36+00:00
aliases:
- artifact_conf
lastmod: 2021-08-05T13:13:30+00:00
---

## Managing

To access the Packages section, click on **Packages** in the navigation bar. You need the **Leader** or **Team Leader** role to access this page.

The Packages view contains all the packages you have configured with the given name, team, filename of the uploaded package if not empty and the date of the last upload.

The Gatling version of the package is displayed in a badge next to the filename.

{{< img src="package-table.png" alt="Packages table" >}}

## Creation

In order to add a package, click on the "Create" button above the packages table.

{{< img src="package-create.png" alt="Package creation" >}}

- **Name**: the name that will appear on the simulations general step.
- **Team**: select the team which will have access to the package.

## Upload

### Option 1: Manual Upload

In order to fill the package with your bundled simulation, click on the {{< icon upload >}} icon on the right side of the table.

{{< alert tip >}}
In order to package a bundle of your simulation, refer to the [Package Generation documentation]({{< ref "package-gen" >}}).
{{< / alert >}}

{{< img src="package-upload-empty.png" alt="Package upload empty" >}}

You can then drag and drop your package to fill the form and click on the **Upload** button.

{{< img src="package-upload-filled.png" alt="Package upload filled" >}}

You can upload multiple packages concurrently. Uploads progress will be shown in the Packages table.

You can cancel an upload in progress by clicking on the cancel icon.

{{< img src="package-upload-progress.png" alt="Package upload progress" >}}

### Option 2: API Upload

You can also upload packages programmatically with our REST API.

You'll need:
* an [API token]({{< ref "../admin/api-tokens" >}}) with at least the `Packages` permission
* the Package's ID, which can be copied from the WebUI.

You can then upload your package, eg with `curl`:

```
curl -X PUT --upload-file <PACKAGE_LOCAL_PATH> \
  "https://<DOMAIN>/api/public/artifacts/<PACKAGE_ID>/content?filename=<PACKAGE_FILE_NAME>" \
  -H "Authorization:<API_TOKEN>"
```

### Option 3: Plugin configuration

Maven, sbt and Gradle plugin offer commands to upload the generated package if properly configured.
Check the [Maven]({{< ref "../../../extensions/build-tools/maven-plugin/#package-and-upload" >}}), 
[Gradle]({{< ref "../../../extensions/build-tools/gradle-plugin/#package-and-upload" >}}), 
[sbt]({{< ref "../../../extensions/build-tools/sbt-plugin/#package-and-upload" >}}) 
plugin extensions for more information.

## Usage

You can configure which package to use for a simulation in the simulation's **General** step.

{{< img src="package-simulation-step.png" alt="Package upload filled" >}}
