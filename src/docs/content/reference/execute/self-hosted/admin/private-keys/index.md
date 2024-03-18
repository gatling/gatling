---
menutitle: Private keys
title: Private keys and certificates
seotitle: Private keys administration in Gatling Enterprise Self-Hosted
description: Learn how to administrate Private Keys in Gatling Enterprise Self-Hosted.
lead: Private keys are necessary to configure pools and injectors.
date: 2021-03-25T18:09:45+01:00
lastmod: 2021-08-16T17:55:36+02:00
---

To access the Private Keys administration, click on **Admin** in the navigation bar, and choose **Private Keys**.

A private key may be needed to:
- Connect to your Git repository
- Connect to load generators
- Upload a custom certificate for [Kubernetes URL]({{< ref "/reference/install/self-hosted/injectors/configuration/kubernetes#credentials-settings" >}})

{{< img src="private-keys.png" alt="Private Keys table" >}}

To create a Private Key, click on the **Create** button.

{{< img src="create-private-key.png" alt="Private Key creation modal" >}}

A private key can be scoped for pools, repositories or certificates. 
It means that you can only use this private key while configuring a **repository**, a **pool** or a **certificate**. 

The **all** scope can't be chosen, as it is only there for the legacy private keys without scope.

You have two possibilities to reference private keys:
- Upload them directly by drag-and-drop or click on the input to choose the file on your filesystem
- Locate a private key existing on Gatling Enterprise's host. The private key permissions should be 600 or 400, and its owner should be the Gatling Enterprise process user

{{< alert tip >}}
If you are using the AWS marketplace offer and wish to reference an existing private key, you must connect with the `ec2-user` user and then `sudo` to the `frontline` user which is the one running the Gatling Enterprise process.{{< /alert >}}

You can edit the private key by clicking on the {{< icon pencil-alt >}} icon and delete them using the checkboxes on the table's right part.

