---
title: On-premises
seotitle: On-premises injectors with Gatling Enterprise Self-Hosted
description: Learn how to configure dedicated on-premises pools and hosts for Gatling Enterprise.
lead: Learn how to configure dedicated on-premises pools and hosts for Gatling Enterprise.
date: 2021-03-26T09:40:55+01:00
lastmod: 2021-08-16T17:55:36+02:00
---

Here you can create an on-premises pool which represents existing machines.

{{< alert warning >}}
Make sure your network configuration will let Gatling Enterprise connect to your injectors on port 22 (SSH) and 9999 (HTTP).
{{< /alert >}}

The pool is defined by a name and a team:

{{< img src="create-pool.png" alt="On premises pool table" >}}

You can edit the name and set if the pool is global or owned by a team by clicking on the {{< icon pencil-alt >}} icon of that pool in the table.
If you click on the name, it will highlight all the hosts which are in this pool in the host table.

You can delete one or more pool by selecting them and click on the **Delete** button above the table. The number of simulations associated to the pool is also displayed.

Now you need to create an host and assign it to the pool. To do so, you need to fill the following form:

{{< img src="create-host.png" alt="Host creation" >}}

An host is basically a server where the Gatling Enterprise server will upload the binary to use it as an injector for the test.

- **Hostname**: the hostname of the server
- **User**: the username to use for the connection to the server
- **Private Key**: the private key used by the server for SSH authentication
- **Pool**: Choose the pool to assign the host. You can select `Unassigned Hosts` if you don't want to put your host into a pool yet.
- **Working Directory**: Directory on the host where the code will be launched, it needs to be executable

To be sure that Gatling Enterprise can access to the host you can click on the **Check connection** button.

You can edit your hosts by clicking on the {{< icon pencil-alt >}} icon in the hosts table.
You can duplicate an host to quickly create an host with the same parameters by clicking on the {{< icon file >}} icon of the host on the table.

{{< img src="duplicate.png" alt="Duplicate host" >}}

If you select one or more hosts, you will see a new button named **Action** appear on the search bar above the table which allow you to delete all the selected hosts or to switch pool to another for all selected hosts.

{{< img src="hosts-actions.png" alt="Hosts actions" >}}

You can sort the pool table or the host table by clicking on their respective columns.
