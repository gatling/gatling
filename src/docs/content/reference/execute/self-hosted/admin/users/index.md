---
menutitle: Users
title: Users administration
seotitle: Users administration in Gatling Enterprise Self-Hosted
description: Learn how to administrate users and their permissions in Gatling Enterprise Self-Hosted.
lead: Administrate your organization's users and their permissions in Gatling Enterprise Self-Hosted.
date: 2021-03-10T08:47:07-05:00
lastmod: 2021-08-16T17:55:36+02:00
---

## Managing Users

To access the Users administration, click on **Admin** in the navigation bar, and choose **Users**.

{{< img src="users.png" alt="Users table" >}}

### Permissions

There are 4 different user roles in Gatling Enterprise:

- System Admin
- Test Admin
- Tester
- Viewer

|                                            | Viewer             | Tester             | Test Admin         | System Admin       |
|--------------------------------------------|:------------------:|:------------------:|:------------------:|:------------------:|
| Access own profile                         | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} | {{< icon check >}} |
| Access Reports and Trends                  | Own team           | Own team           | Own team           | Own team           |
| Start Simulation                           |                    | Own team           | Own team           | Own team           |
| Generate Public Links                      |                    | Own team           | Own team           | Own team           |
| Create Simulation                          |                    |                    | Own team           | Own team           |
| Access Pools                               |                    |                    | Own team           | Own team           |
| Manage Pools                               |                    |                    |                    | Own team           |
| Administrate Private Keys and Repositories |                    |                    | Own team           | Own team           |
| Administrate API Tokens, Users and Teams   |                    |                    |                    | Own team           |

Each role can be global or team-specific.

### Users administration

{{< alert warning >}}
If you switch between Gatling Enterprise embedded user management system and LDAP/OpenID mode, or if there is a problem fetching your user data in LDAP/OpenID, some users may be flagged as invalid. For example, if a user created in Gatling Enterprise doesn't exist in your LDAP/OpenID server, you won't be able to connect with this user anymore.
{{< /alert >}}

To create a user, click on the **Create** button. Once the user is created, copy his password, as you won't be able to retrieve it again. OpenID authentication disable user creation, and only allow to edit users permissions (users who have already connected one time).

{{< img src="users-create.png" alt="User creation" >}}

If you are using Gatling Enterprise with LDAP or OpenID, you will only have to fill the username and permissions to create a user, the other pieces of information come directly from the LDAP/OpenID. The username should be the same username as in the LDAP/OpenID.

You can edit the user by clicking on the {{< icon pencil-alt >}} icon and delete them using the checkboxes on the table's right part.
To reset a user password, click on the {{< icon undo >}} icon (only available in non-LDAP mode).

{{< alert tip >}}
The superAdmin account can't be seen here.
{{< /alert >}}
