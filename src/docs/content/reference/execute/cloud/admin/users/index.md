---
menutitle: Users
title: Users administration
seotitle: Users administration in Gatling Enterprise Cloud
description: Learn how to administrate users and their permissions in Gatling Enterprise Cloud.
lead: Administrate your organization's users and their permissions in Gatling Enterprise Cloud.
date: 2021-03-10T13:47:07+00:00
lastmod: 2021-08-05T13:13:30+00:00
---

## Managing Users

To access the Users administration, click on the {{< icon user >}} icon in the navigation bar.

{{< img src="users-table.png" alt="Users table" >}}

To invite a user to your organization, click on the **Invite** button and fill in the user information:

- **Email address**: The email address to which the invitation will be sent. The invited user doesn't have to use the same address in their user profile once connected.
- **Organization role**: either choose an organization role (which will be applied to every team), or select none and specify each role by team according to the permissions you want to grant.

{{< alert warning >}}
Only organization system admins can generate invitations.
{{< /alert >}}

{{< img src="users-invite.png" alt="User create" >}}

You can now click on the **Invited** tab to see the invitation.

{{< img src="users-invited.png" alt="User invited" >}}

The invited user will receive an email with an invitation link to join the organization.

{{< img src="invitation.png" alt="User email invitation" >}}

When they click on  **Accept the invitation**, they will be able to join the organization using their existing Gatling Enterprise Cloud user account if they already have one, or else they will have to create a new account.

To edit a user, click on the {{< icon pencil-alt >}} icon. To remove them from your organization, select them using the checkbox on the left of the table and click on the **Remove** button.

## Permissions

There are 8 different user roles in Gatling Enterprise:

For an organization:

- Administrator
- Leader
- Contributor
- Viewer

For a team:

- Team Administrator
- Team Leader
- Team Contributor
- Team Viewer

|                                          | Viewer / Team Viewer | Contributor / Team Contributor | Leader / Team Leader | Administrator / Team Administrator |
|------------------------------------------|:--------------------:|:------------------------------:|:--------------------:|:----------------------------------:|
| Access own profile and Organization page |  {{< icon check >}}  |       {{< icon check >}}       |  {{< icon check >}}  |         {{< icon check >}}         |
| Access Reports and Trends                |       Own team       |            Own team            |       Own team       |              Own team              |
| Start Simulation                         |                      |            Own team            |       Own team       |              Own team              |
| Generate Public Links                    |                      |            Own team            |       Own team       |              Own team              |
| Create Simulation                        |                      |                                |       Own team       |              Own team              |
| Administrate Packages                    |                      |                                |       Own team       |              Own team              |
| Administrate API Tokens, Users and Teams |                      |                                |                      |              Own team              |
| Subscribe and view Offers                |                      |                                |                      |         Administrator only         |

