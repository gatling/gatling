---
menutitle: Custom SSO
title: Custom SSO configuration
seotitle: Configure a custom SSO with Gatling Enterprise Cloud
description: Learn how to set up a custom Single Sign-On (SSO) system for your organization.
lead: A custom Single Sign-On (SSO) configuration allows your users to sign into Gatling Enterprise Cloud using your organization's authentication system.
date: 2022-03-01T14:00:00+00:00
lastmod: 2022-03-01T14:00:00+00:00
---

Configuring a custom Single Sign-On (SSO) solution is only available on [corporate plan](https://gatling.io/pricing/) and must be requested through our [support portal](https://gatlingcorp.atlassian.net/servicedesk/customer/portal/8).

At the moment, we only support integration with SSO systems which are accessible on the Internet, and user roles are still administered within Gatling Enterprise.

If you already have an organization on Gatling Enterprise Cloud, after configuring a custom SSO, you will need to re-invite your users and configure their roles. You will, however, keep all other existing data, such as your [teams]({{< ref "teams" >}}), [simulations]({{< ref "../user/simulations" >}}), [reports]({{< ref "/reference/stats/reports/cloud" >}}), [API tokens]({{< ref "api-tokens" >}}), etc.

## Required information

To handle your support request, we will need to know:

- the name and slug for your organization, visible on the [organization settings page]({{< ref "../user/organization" >}})
- relevant information depending on the type of SSO system used (see below)
- a way to configure one of your users as a global admin, so that they can invite back your other users - in most cases, we will ask them to sign in once, after we have configured the SSO, to link their account

{{< alert warning >}}
We provide some pointers below about the supported SSO systems, but keep in mind that you will need to open a support ticket first before you start configuring anything on your end.
{{< /alert >}}

### OpenID Connect (OIDC) v1.0

If your SSO system offers OIDC v1.0 integration, it will typically publish its metadata at a well known URL, often of the form `https://my-sso-server.com/.well-known/openid-configuration`. To configure the SSO integration, we will need:

- the metadata URL
- the client authentications which are supported by your OIDC implementation (client secret sent as POST, Basic Auth, or
  JWT)
- the client ID and secret we should use to connect to the SSO

{{< alert info >}}
You will need to configure a **redirect URI** based on your organization's slug:
`https://auth.gatling.io/auth/realms/<slug>/broker/oidc/endpoint`

So for example, for an organization named Gatling Corp, with the slug `helloworld`, result in the following **redirect URI**:
`https://auth.gatling.io/auth/realms/helloworld/broker/oidc/endpoint`
{{< /alert >}}


#### Azure Active Directory (Azure AD) using OIDC

We recommend using OIDC to integrate with Azure AD. You will need to create an app registration for Gatling Enterprise, and configure which accounts are allowed to connect. You will then find the metadata URL in Overview > Endpoints > OpenID Connect metadata document, and you can generate a new client secret in Certificates & secrets > Client secrets.

You will need to configure the **redirect URI** in Overview > Add a **redirect URI**.

#### Okta using OIDC

We recommend using OIDC to integrate with Okta. You will need to create a new app integration, with the sign-in method "OIDC" and application type "Web Application". You can then copy the client ID, client secret and Okta domain (you can find the metadata URL from the domain) for the application integration you just created.

You will need to edit in the application integration you created to configure the **redirect URI**.

### SAML v2.0

{{< alert tip >}}
If your SSO system supports both OIDC and SAML, we recommend using OIDC.
{{< /alert >}}

If your SSO system provides SAML v2.0 integration, the Identity Provider entity (IdP) is usually published on a particular URL. 
If that URL is not publicly accessible, export the metadata containing the information necessary to integrate with the IdP and forward it.

To configure the SSO integration, we will need:
- the SAML entity descriptor for your Identity Provider
- the attributes for a user's first name, last name, and email address (they should appear as `ClaimType`s in your IdP's entity descriptor)

{{< alert info >}}
By default, our system will rely on the subject NameID to uniquely and consistently identify each user. 
We will also, by default, leave the NameID policy format unspecified (`urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified`), 
letting the IdP free to choose how to populate the subject NameID. 

If needed we can rely on another attribute instead of the NameID (it needs to be unique and immutable), or specify a particular NameID policy format.
{{< /alert >}}

We can then give you our Service Provider (SP) descriptor if you need to import it into your SSO system.

### Google

See [Google's guide](https://support.google.com/cloud/answer/6158849) on setting up an oAuth 2.0 app. Make sure to:

- select the user type "internal" to only allow users from your organization
- add `gatling.io` as an authorized domain

You will need to create a client ID of the type "web application" with the authorized **redirect URI**, then send us the client ID and client secret.

### GitHub

See [GitHub's guide](https://docs.github.com/en/developers/apps/building-oauth-apps/creating-an-oauth-app) on setting up an oAuth 2.0 app for an organization. Make sure to:

- configure `https://cloud.gatling.io` as the Homepage URL
- configure the Authorization callback URL we will provide you

You will need to generate a new client secret, then send us the client ID and client secret. We will also need your GitHub organization's name to restrict access to users from your organization.

### GitLab

See [GitLab's guide](https://docs.gitlab.com/ee/integration/oauth_provider.html) on setting up an oAuth 2.0 app for a group. Make sure to:

- configure the **redirect URI**
- enable the following scopes: `api`, `read_user`, `read_api`

We will need the application ID and secret. We will also need your GitLab group ID to restrict access to users from your group.
