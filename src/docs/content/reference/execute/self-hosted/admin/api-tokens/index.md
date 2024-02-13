---
menutitle: API tokens
title: API tokens configuration
seotitle: Configure API Tokens in Gatling Enterprise Self-Hosted
description: Learn how to administrate API tokens to authenticate your requests to the Gatling Enterprise Self-Hosted public API.
lead: Create API tokens to authenticate your requests to the Gatling Enterprise Self-Hosted public API.
date: 2021-03-10T08:47:16-05:00
lastmod: 2021-08-16T17:55:36+02:00
---

## Managing API Tokens

To access the API Tokens administration, click on **Admin** in the navigation bar, and choose **API token**.

{{< img src="tokens.png" alt="Tokens" >}}

To create an API token, click on the **Create** button. Once the API token is created, copy the token, as you won't be able to retrieve it again.

{{< img src="create-token.png" alt="Create token" >}}

There are three permissions available for an API Token:

- The Start permission, allowing to start simulations
- The Read permission, allowing to read all the data from runs (typically useful in Grafana)
- The All permission, combining both of the previous permissions

{{< alert tip >}}
The permission needed for our CI plugins is All.
{{< /alert >}}

You can edit the API Token permissions by clicking on the {{< icon pencil-alt >}} icon on the right part of the table. A permission can be set globally or to a specific team.
To regenerate a token, click on the {{< icon undo >}} icon.
