---
menutitle: Teams
title: Teams administration
seotitle: Teams administration in Gatling Enterprise Self-Hosted
description: Learn how to administrate teams in Gatling Enterprise Self-Hosted.
lead: Administrate your organization's teams in Gatling Enterprise Self-Hosted.
date: 2021-03-10T08:47:03-05:00
lastmod: 2021-08-16T17:55:36+02:00
---

## Managing Teams

To access the Teams administration, click on **Admin** in the navigation bar, and choose **Teams**.

{{< img src="teams.png" alt="Teams" >}}

In the teams table, you can visualize the team name, the optional team quota, and the number of associated users, pools and simulations.
You can also copy the team id by clicking on the {{< icon clipboard >}} icon.

### Teams settings

To open the teams settings, click on **Teams Settings** on the right side of the search bar.

{{< img src="teams-settings.png" alt="Teams settings" caption="Teams settings" >}}

The simulation quota of a team means the number of simulations a team is allowed to own. By default, there won't be any limitation, and your teams will be able to create simulations until you reach the number of simulations defined in your license.

The checkbox **Simulations quotas** needs to be enabled if you want the quotas to be applied. The sum of the quotas needs to be less or equal to the number of simulations allowed by your license. Please note that if this option is enabled, you need to provide a quota for each team, or this team won't be able to create a simulation.

### Team

To create a team, click on the **Create** button.

{{< img src="create-team.png" alt="Creating a team" caption="Creating a team" >}}

You can edit the team by clicking on the {{< icon pencil-alt >}} icon and delete them using the checkboxes on the table's right part.
Note that you can't edit a quota from this modal, it can only be done from the team settings modal.
