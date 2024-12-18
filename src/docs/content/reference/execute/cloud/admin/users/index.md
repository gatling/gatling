---
menutitle: Users
title: User Management
seotitle: User management in Gatling Enterprise
description: Learn how to manage users and their permissions in Gatling Enterprise.
lead: Manage your organization's users and their permissions in Gatling Enterprise.
date: 2021-03-10T13:47:07+00:00
lastmod: 2024-12-18T09:40:00+00:00
---

## Manage users and permissions {#managing-users}

To manage users, navigate to the **Organization** menu and click on the {{< icon user >}} **Users** tab.

{{< img src="users-table.png" alt="Users table" >}}

### Understand permissions {#permissions}

There are 8 different user roles and corresponding permission levels in Gatling Enterprise:

Organization-level roles:

- Administrator
- Leader
- Contributor
- Viewer

Team-level roles:

- Team Administrator
- Team Leader
- Team Contributor
- Team Viewer

The following table details the permissions granted to each role. For team-level roles, the permissions only apply to each team for which the user has permissions: 

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

### Invite new users 

To invite a user to your organization:

1. Click on the **Invite new users** button.
2. Add the email address to which the invitation will be sent.
3. Select an organization and (optional) team role(s). See [Permissions]({{< ref "#permissions" >}}) for a detailed description of each role. 
4. Click **Send invitations**

{{< alert tip >}}
Batch-invite users with the same permissions by adding multiple email addresses to the **Email address** field. Delimit email addresses with a comma or space.
{{< /alert >}}


{{< alert warning >}}
- Organization administrators can generate invitations to the organization and each team.
- Team administrators can only generate invitations to their team(s).
{{< /alert >}}

{{< img src="users-invite.png" alt="User create" >}}

### View and manage invited users

To view the list of invited users, click on the **Invitations** tab.

Invited users receive an email with an invitation link to join the organization. When invited users click on  **Accept the invitation**, they are redirected to join the organization. Existing Gatling Enterprise users have the new organization added to their account and new Gatling Enterprise users are directed to create an account.

### Edit or remove existing users

To edit a user, click the **Edit roles** button next to any existing user. 

To remove an existing user from your organization, select the checkbox to the left of the user's name and click the **Delete** button at the top of the user table. 
