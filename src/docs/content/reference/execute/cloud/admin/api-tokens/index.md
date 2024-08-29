---
menutitle: API tokens
title: API tokens configuration
seotitle: Configure API Tokens in Gatling Enterprise Cloud
description: Learn how to administrate API tokens to authenticate your requests to the Gatling Enterprise Cloud public API.
lead: Create API Tokens to authenticate your requests to the Gatling Enterprise Cloud public API.
date: 2021-03-10T13:47:16+00:00
lastmod: 2021-08-05T13:13:30+00:00
---

API Tokens administration is accessible in the navigation bar.

## Managing API Tokens

{{< img src="tokens.png" alt="Tokens" >}}

To create an API token, click on the **Create** button.
Once the API token is created, make sure to copy the token, as you won't be able to retrieve it again later.

{{< img src="create-token.png" alt="Create token" >}}

There are three permissions available for API Tokens:

- The Read permission, allowing to read all the data
- The Start permission, allowing to start simulations + Read permissions (typically useful in a CI plugin)
- The Configure permission, allowing to create / upload packages and create simulations + Start permissions (typically useful in our build plugins)

You can set a permission globally or within a specific team only.

{{< alert tip >}}
**CI Plugins** need the **Start** permission
{{< /alert >}}
{{< alert tip >}}
**Build Plugins** need the **Configure** permission
{{< /alert >}}

You can edit the API Token permissions by clicking on the {{< icon pencil-alt >}} icon on the right part of the table. 
To regenerate a token, click on the {{< icon undo >}} icon.
